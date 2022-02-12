import extensions.toListOfMaps
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingFieldSelectionSet
import graphql.schema.GraphQLScalarType
import graphql.schema.SelectedField
import org.apache.calcite.rel.RelNode
import org.apache.calcite.rex.RexNode

data class TableDataFetcher(val fullyQualifiedTableName: FullyQualifiedTableName) :
    DataFetcher<Any> {
    private val databaseName = fullyQualifiedTableName.database
    private val schemaName = fullyQualifiedTableName.schema
    private val tableName = fullyQualifiedTableName.table
    private val foreignKeys = ForeignKeyManager.getForeignKeysForTable(fullyQualifiedTableName)

    override fun get(env: DataFetchingEnvironment): Any {
        return mutuallyRecursiveHandleField(env.arguments, env.selectionSet)
    }

    /**
     * For each query field:
     *  1. Create a query by:
     *      a. Scan on (DB, Schema, Table)
     *      b. Project all scalar fields
     *      c. Convert the "where" argument to a filter clause
     *      d. Add "limit" and "offset" clauses
     *  2. Run the query
     *  3. For each nested field selection in the query:
     *      a. Repeat the above (Adding the foreign key to the filter clause
     *         as an IN statement using the result of the previous query)
     *      b. Add the results to the parent record under the nested field's name
     */
    fun mutuallyRecursiveHandleField(
        arguments: Map<String, Any>,
        selectionSet: DataFetchingFieldSelectionSet,
        joinFilterCondition: RexNode? = null
    ): List<Map<String, Any>> {
        val where = arguments["where"] as Map<String, Any>?
        val limit = arguments["limit"] as Int?
        val offset = arguments["offset"] as Int?
        val orderBy = arguments["order_by"] as List<String>?

        val (scalars, joins) = selectionSet.immediateFields.partition {
            it.type is GraphQLScalarType
        }

        val relNode = buildRelationalExpr(joinFilterCondition, where, offset, limit, scalars)
        val rows = CalciteSchemaManager.executeQuery(relNode).toListOfMaps()

        for (join in joins) {
            // Recursively mutates the row values in parent records to attach values from child tables
            // TODO: Can this be immutable?
            mutuallyRecursiveHandleJoinField(join, rows)
        }

        return rows
    }

    private fun buildRelationalExpr(
        joinFilterCondition: RexNode?,
        where: Map<String, Any>?,
        offset: Int?,
        limit: Int?,
        scalars: List<SelectedField>
    ): RelNode {
        val relBuilder = CalciteSchemaManager.relBuilder.apply {
            // If there is a filter condition, we already have a scan on the stack and so shouldn't create a new one
            if (joinFilterCondition == null) {
                // Scan on (DB, Schema, Table)
                if (schemaName != null)
                    this.scan(databaseName, schemaName, tableName)
                else
                    this.scan(databaseName, tableName)
            }

            //  Convert the "where" argument to a filter clause
            if (where != null)
                this.filter(whereArgumentToExpression(where).toRexNode(this))

            // If there is a join filter condition, we need to add it to the filter clause
            if (joinFilterCondition != null)
                this.filter(joinFilterCondition)

            // Add "limit" and "offset" clauses
            if (offset != null || limit != null)
                this.limit((offset ?: 0), (limit ?: -1))

            // Project all scalar fields
            val columns = scalars.map { this.field(it.name) }
            this.project(columns)
        }

        return relBuilder.build()
    }

    // Mutually recursive with "mutuallyRecursiveHandleField"
    private fun mutuallyRecursiveHandleJoinField(
        join: SelectedField,
        rows: MutableList<MutableMap<String, Any>>
    ) {
        val foreignKey = foreignKeys.find { it.sourceTable.table == join.name || it.targetTable.table == join.name }
        if (foreignKey == null) return

        val tableIsTargetInJoin = foreignKey.targetTable == fullyQualifiedTableName
        val joinTable = if (tableIsTargetInJoin) foreignKey.sourceTable else foreignKey.targetTable

        val innerRelBuilder = CalciteSchemaManager.relBuilder
        innerRelBuilder.scan(joinTable.database, joinTable.schema, joinTable.table)

        val joinResults = mutuallyRecursiveHandleField(
            join.arguments,
            join.selectionSet,
            innerRelBuilder.and(
                foreignKey.columns.map { colPair ->
                    if (tableIsTargetInJoin) {
                        innerRelBuilder.`in`(
                            innerRelBuilder.field(colPair.first),
                            rows.map { innerRelBuilder.literal(it[colPair.second]) }
                        )
                    } else {
                        innerRelBuilder.`in`(
                            innerRelBuilder.field(colPair.second),
                            rows.map { innerRelBuilder.literal(it[colPair.first]) }
                        )
                    }
                }
            )
        )

        // TODO: Check if the below is faster:
        //  - Grouping the rows by their primary key first
        //  - Then looking up directly instead of filtering
        //  - Finally flattening
        fun buildParentRowProperty(foreignKey: ForeignKey) {
            if (tableIsTargetInJoin) {
                for (row in rows) {
                    row[join.name] = joinResults.filter { joinRow ->
                        foreignKey.columns.all { colPair ->
                            joinRow[colPair.first] == row[colPair.second]
                        }
                    }
                }
            } else {
                for (row in rows) {
                    row[join.name] = joinResults.first { joinRow ->
                        foreignKey.columns.all { colPair ->
                            joinRow[colPair.second] == row[colPair.first]
                        }
                    }
                }
            }
        }

        buildParentRowProperty(foreignKey)
    }

    private fun whereArgumentToExpression(whereArgument: Map<*, *>): Expression {
        val exprs = whereArgument.entries.map { (key, value) ->
            when (key) {
                "_and" -> {
                    if (value is List<*>) {
                        value.map {
                            if (it is Map<*, *>) {
                                return whereArgumentToExpression(it)
                            } else {
                                throw IllegalArgumentException("_and must be a list of expressions")
                            }
                        }.reduceRight(::AND)
                    }
                    throw IllegalArgumentException("_and must be a list")
                }
                "_or" -> {
                    if (value is List<*>) {
                        value.map {
                            if (it is Map<*, *>) {
                                return whereArgumentToExpression(it)
                            } else {
                                throw IllegalArgumentException("_and must be a list of expressions")
                            }
                        }.reduceRight(::OR)
                    }
                    throw IllegalArgumentException("_or must be a list")
                }
                "_not" -> {
                    if (value is Map<*, *>) {
                        return NOT(whereArgumentToExpression(value))
                    } else {
                        throw IllegalArgumentException("_not must be an expression")
                    }
                }
                else -> {
                    if (value is Map<*, *>) {
                        assert(value.size == 1) { "Expected single value" }
                        if (key is String) {
                            return getExpression(key, value)
                        }
                        throw IllegalArgumentException("Expected string key")
                    }
                    throw IllegalArgumentException("Unknown key $key")
                }
            }
        }
        return exprs.reduceRight(::AND)
    }

    private fun getExpression(column: String, valueMap: Map<*, *>): Expression {
        val operator = valueMap.keys.iterator().next()
        val operand = requireNotNull(valueMap.values.iterator().next()) { "Operand must be present" }
        return when (operator) {
            "_eq" -> EQ(COLUMN(column), LITERAL(operand))
            "_neq" -> NEQ(COLUMN(column), LITERAL(operand))
            "_gt" -> GT(COLUMN(column), LITERAL(operand))
            "_gte" -> GTE(COLUMN(column), LITERAL(operand))
            "_lt" -> LT(COLUMN(column), LITERAL(operand))
            "_lte" -> LTE(COLUMN(column), LITERAL(operand))
            "_in" -> IN(COLUMN(column), LITERAL(operand))
            "_nin" -> NIN(COLUMN(column), LITERAL(operand))
            "_is_null" -> IS_NULL(COLUMN(column))
            else -> throw IllegalArgumentException("Unknown operator: $operator")
        }
    }
}
