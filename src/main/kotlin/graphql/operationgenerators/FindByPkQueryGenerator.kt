package graphql.operationgenerators

import calcite.CalciteRootSchema
import calcite.CalciteSchemaManager
import graphql.TableGQLFieldGenerator
import graphql.schema.DataFetcher
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLTypeReference
import operation_providers.DefaultSqlTypeToGraphQLMapping

object FindByPkQueryGenerator : TableGQLFieldGenerator(DefaultSqlTypeToGraphQLMapping) {
    override fun shouldGenerate(table: CalciteRootSchema.Table): Boolean {
        return table.primaryKey != null
    }

    override fun generate(table: CalciteRootSchema.Table): GraphQLFieldDefinition {
        val primaryKey = requireNotNull(table.primaryKey) { "Table ${table.name} has no primary key" }
        return GraphQLFieldDefinition.newFieldDefinition()
            .name(table.name + "_by_pk")
            .dataFetcher(getDataFetcher(table))
            .type(
                GraphQLTypeReference(
                    if (table.schema == null)
                        table.database.name + "_" + table.name + "_type"
                    else
                        table.database.name + "_" + table.schema.name + "_" + table.name + "_type"
                )
            )
            .arguments(
                primaryKey.columns.map { pkColumn ->
                    GraphQLArgument.newArgument()
                        .name(pkColumn)
                        .type(
                            table.columns()
                                .find { it.name == pkColumn }
                                ?.let {
                                    val sqlType = it.underlying.type.sqlTypeName
                                    sqlTypeToGraphQLMapping.toGraphQLScalarType(sqlType) as GraphQLInputType
                                }
                                ?: throw IllegalStateException("Could not find column $pkColumn in table ${table.name}")
                        )
                        .build()
                }
            )
            .build()
    }

    override fun getDataFetcher(table: CalciteRootSchema.Table): DataFetcher<Any> {
        return DataFetcher { env ->
            val primaryKey = table.primaryKey!!
            val primaryKeyValues = primaryKey.columns.map { pkColumn ->
                env.getArgument<Any>(pkColumn)
            }

            val (scalars, joins) = env.selectionSet.immediateFields.partition {
                it.type is GraphQLScalarType
            }

            val relBuilder = CalciteSchemaManager.relBuilder.apply {
                if (table.schema != null)
                    this.scan(table.database.name, table.schema.name, table.name)
                else
                    this.scan(table.database.name, table.name)

                this.project(scalars.map { this.field(it.name) })

                primaryKey.columns.forEachIndexed { index, pkColumn ->
                    this.filter(
                        this.equals(
                            this.field(pkColumn),
                            this.literal(primaryKeyValues[index])
                        )
                    )
                }
            }

            val relNode = relBuilder.build()
            val rows = CalciteSchemaManager.executeQuery(relNode)

            // Return the first row
            if (rows.next()) {
                val md = rows.metaData
                val result = mutableMapOf<String, Any>()
                for (i in 1..md.columnCount) {
                    result[md.getColumnName(i)] = rows.getObject(i)
                }
                result
            } else {
                null
            }
        }
    }
}
