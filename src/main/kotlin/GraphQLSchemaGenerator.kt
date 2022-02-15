import graphql.Scalars
import graphql.schema.DataFetcher
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLList
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import graphql.schema.GraphQLTypeReference
import operation_providers.BaseGraphQLTypes
import org.apache.calcite.rel.type.RelDataType
import org.apache.calcite.rel.type.RelDataTypeField
import org.apache.calcite.schema.SchemaPlus
import org.apache.calcite.schema.Table
import org.apache.calcite.sql.type.SqlTypeFamily


interface GraphQLOperationProvider {
    fun getQueries(): List<GraphQLFieldDefinition>
    fun getMutations(): List<GraphQLFieldDefinition>
    fun getSubscriptions(): List<GraphQLFieldDefinition>
}

object GraphQLFindAllQueryProvider : GraphQLOperationProvider {
    override fun getQueries(): List<GraphQLFieldDefinition> {
        return emptyList()

    }

    override fun getMutations(): List<GraphQLFieldDefinition> {
        return emptyList()
    }

    override fun getSubscriptions(): List<GraphQLFieldDefinition> {
        return emptyList()
    }

    private fun generateDatabaseQueryFields(database: SchemaPlus): List<GraphQLFieldDefinition> {
        return emptyList()

    }

    private fun generateSchemaQueryFields(schema: SchemaPlus): List<GraphQLFieldDefinition> {
        return emptyList()

    }

    private fun generateTableQueryFields(table: Table): List<GraphQLFieldDefinition> {
        return emptyList()
    }

    private fun generateColumnQueryFields(fields: List<RelDataTypeField>): List<GraphQLFieldDefinition> {
        return emptyList()
    }

}

data class GraphQLSchemaGenerator2(
    val operationProviders: List<GraphQLOperationProvider>
) {
    fun buildSchema(): GraphQLSchema {
        val schema = GraphQLSchema.newSchema()

        val queryTypeBuilder = GraphQLObjectType.newObject().name("Query")
        val mutationTypeBuilder = GraphQLObjectType.newObject().name("Mutation")
        val subscriptionTypeBuilder = GraphQLObjectType.newObject().name("Subscription")

        for (operationProvider in operationProviders) {
            for (query in operationProvider.getQueries()) {
                queryTypeBuilder.field(query)
            }
            for (mutation in operationProvider.getMutations()) {
                mutationTypeBuilder.field(mutation)
            }
            for (subscription in operationProvider.getSubscriptions()) {
                subscriptionTypeBuilder.field(subscription)
            }
        }

        val queryType = queryTypeBuilder.build()
        val mutationType = mutationTypeBuilder.build()
        val subscriptionType = subscriptionTypeBuilder.build()

        if (queryType.fields.isNotEmpty()) {
            schema.query(queryTypeBuilder)
        }
        if (mutationType.fields.isNotEmpty()) {
            schema.mutation(mutationTypeBuilder)
        }
        if (subscriptionType.fields.isNotEmpty()) {
            schema.subscription(subscriptionTypeBuilder)
        }

        return schema.build()
    }
}

object GraphQLSchemaGenerator {
    private fun relDataTypeToGraphQLType(type: RelDataType): GraphQLType {
        return when (type.family as SqlTypeFamily) {
            SqlTypeFamily.BOOLEAN -> Scalars.GraphQLBoolean
            SqlTypeFamily.INTEGER -> Scalars.GraphQLInt
            SqlTypeFamily.NUMERIC, SqlTypeFamily.DECIMAL -> Scalars.GraphQLFloat
            SqlTypeFamily.CHARACTER, SqlTypeFamily.STRING -> Scalars.GraphQLString
            SqlTypeFamily.DATE, SqlTypeFamily.DATETIME -> Scalars.GraphQLString
            SqlTypeFamily.TIME, SqlTypeFamily.TIMESTAMP -> Scalars.GraphQLString
            else -> throw IllegalArgumentException("Unknown type $type")
        }
    }

    private fun relDataTypeToGraphQLComparisonExprType(type: RelDataType): GraphQLType {
        return when (type.family as SqlTypeFamily) {
            SqlTypeFamily.BOOLEAN -> BaseGraphQLTypes.BooleanComparisonExpressionType
            SqlTypeFamily.INTEGER -> BaseGraphQLTypes.IntComparisonExpressionType
            SqlTypeFamily.NUMERIC, SqlTypeFamily.DECIMAL -> BaseGraphQLTypes.FloatComparisonExpressionType
            SqlTypeFamily.CHARACTER, SqlTypeFamily.STRING -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeFamily.DATE, SqlTypeFamily.DATETIME -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeFamily.TIME, SqlTypeFamily.TIMESTAMP -> BaseGraphQLTypes.StringComparisonExpressionType
            else -> throw IllegalArgumentException("Unknown type $type")
        }
    }

    fun generateGraphQLSchemaFromRootSchema(rootSchema: SchemaPlus): GraphQLSchema {
        val systemSchemaNames = listOf("information_schema", "sys", "pg_catalog", "metadata")
        return graphqlSchema {
            query = objectType {
                name = "Query"
                fields = rootSchema.subSchemaNames.map { databaseName ->
                    val database = requireNotNull(rootSchema.getSubSchema(databaseName))
                    field {
                        name = databaseName
                        dataFetcher = DataFetcher { "continue" }
                        type = objectType {
                            name = databaseName + "_type"
                            fields = if (database.subSchemaNames.isEmpty()) {
                                makeGraphQLFieldsForDBOrSchemaTables(database, null)
                            } else {
                                val ssn = database.subSchemaNames
                                val schemaNames = ssn.filterNot { systemSchemaNames.contains(it.lowercase()) }

                                schemaNames.map { schemaName ->
                                    val schema = requireNotNull(database.getSubSchema(schemaName))
                                    field {
                                        name = schemaName
                                        dataFetcher = DataFetcher { "continue" }
                                        type = objectType {
                                            name = databaseName + "_" + schemaName + "_type"
                                            fields = makeGraphQLFieldsForDBOrSchemaTables(database, schema)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun makeGraphQLFieldsForDBOrSchemaTables(
        database: SchemaPlus,
        schema: SchemaPlus?,
    ): List<GraphQLFieldDefinition> {
        val target = schema ?: database
        val path = if (schema == null) database.name else database.name + "_" + schema.name

        // Outgoing foreign keys = object relationship (IE: User -> House)
        fun buildOutgoingForeignKeyFieldTypes(fullyQualifiedTableName: FullyQualifiedTableName): List<GraphQLFieldDefinition> {
            val outgoingForeignKeys = ForeignKeyManager.getForeignKeysForTable(fullyQualifiedTableName).filter {
                it.sourceTable == fullyQualifiedTableName
            }

            return outgoingForeignKeys.map {
                val targetGQLTypeName = it.targetQualifiedName.joinToString("_")
                field {
                    name = it.targetQualifiedName.last()
                    type = GraphQLTypeReference(targetGQLTypeName + "_type")
                    arguments = listOf(
                        argument {
                            name = "where"
                            type = GraphQLTypeReference(targetGQLTypeName + "_bool_exp")
                        }
                    )
                }
            }
        }

        // Incoming foreign keys = array relationship (IE: House -> User)
        fun buildIncomingForeignKeyFieldTypes(fullyQualifiedTableName: FullyQualifiedTableName): List<GraphQLFieldDefinition> {
            val incomingForeignKeys = ForeignKeyManager.getForeignKeysForTable(fullyQualifiedTableName).filter {
                it.targetTable == fullyQualifiedTableName
            }
            return incomingForeignKeys.map {
                val targetGQLTypeName = it.sourceQualifiedName.joinToString("_")
                field {
                    name = it.sourceQualifiedName.last()
                    type = GraphQLNonNull(
                        GraphQLList(
                            GraphQLNonNull(
                                GraphQLTypeReference(targetGQLTypeName + "_type")
                            )
                        )
                    )
                    arguments = listOf(
                        argument {
                            name = "where"
                            type = GraphQLTypeReference(targetGQLTypeName + "_bool_exp")
                        }
                    )
                }
            }
        }

        return target.tableNames.map { tableName ->
            val table = requireNotNull(target.getTable(tableName))
            val boolExpTypeName = path + "_" + tableName + "_bool_exp"
            val fieldList = table.getRowType(JAVA_TYPE_FACTORY_IMPL).fieldList

            val fullyQualifiedTableName = FullyQualifiedTableName(database.name, schema?.name, tableName)
            val outgoingFkTypes = buildOutgoingForeignKeyFieldTypes(fullyQualifiedTableName)
            val incomingFkTypes = buildIncomingForeignKeyFieldTypes(fullyQualifiedTableName)
            val relatedTypes = outgoingFkTypes + incomingFkTypes

            field {
                name = tableName
                dataFetcher = TableDataFetcher(fullyQualifiedTableName)
                type = graphqlList {
                    objectType {
                        name = path + "_" + tableName + "_type"
                        fields = relatedTypes + fieldList.map { field: RelDataTypeField ->
                            field {
                                name = field.name
                                type = relDataTypeToGraphQLType(field.type) as GraphQLOutputType
                            }
                        }
                    }
                }
                arguments = listOf(
                    argument {
                        name = "limit"
                        type = Scalars.GraphQLInt
                    },
                    argument {
                        name = "offset"
                        type = Scalars.GraphQLInt
                    },
                    argument {
                        name = "where"
                        type = inputObjectType {
                            name = boolExpTypeName
                            fields = fieldList.map { field: RelDataTypeField ->
                                inputObjectField {
                                    name = field.name
                                    type = relDataTypeToGraphQLComparisonExprType(field.type) as GraphQLInputType
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

