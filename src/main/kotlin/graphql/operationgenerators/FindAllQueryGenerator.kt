package graphql.operationgenerators

import calcite.CalciteRootSchema
import graphql.Scalars
import graphql.TableDataFetcher2
import graphql.TableGQLFieldGenerator
import graphql.schema.DataFetcher
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLList
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLTypeReference
import operation_providers.DefaultSqlTypeToGraphQLMapping

object FindAllQueryGenerator : TableGQLFieldGenerator(DefaultSqlTypeToGraphQLMapping) {
    override fun generate(table: CalciteRootSchema.Table): GraphQLFieldDefinition {
        return GraphQLFieldDefinition.newFieldDefinition()
            .name(table.name)
            .dataFetcher(getDataFetcher(table))
            .type(
                GraphQLNonNull(
                    GraphQLList(
                        GraphQLNonNull(
                            GraphQLTypeReference(
                                if (table.schema == null)
                                    table.database.name + "_" + table.name + "_type"
                                else
                                    table.database.name + "_" + table.schema.name + "_" + table.name + "_type"
                            )
                        )
                    )
                )
            )
            .argument(
                GraphQLArgument.newArgument()
                    .name("limit")
                    .type(Scalars.GraphQLInt)
                    .build()
            )
            .argument(
                GraphQLArgument.newArgument()
                    .name("offset")
                    .type(Scalars.GraphQLInt)
                    .build()
            )
            .argument(
                GraphQLArgument.newArgument()
                    .name("where")
                    .type(
                        GraphQLInputObjectType.newInputObject()
                            .name(
                                if (table.schema == null)
                                    table.database.name + "_" + table.name + "_bool_exp"
                                else
                                    table.database.name + "_" + table.schema.name + "_" + table.name + "_bool_exp"
                            )
                            .fields(
                                table.columns().map { column ->
                                    GraphQLInputObjectField.newInputObjectField()
                                        .name(column.name)
                                        .type(sqlTypeToGraphQLMapping.toGraphQLInputType(column.underlying.type.sqlTypeName))
                                        .build()
                                }
                            )
                            .build()
                    )
            )
            .build()
    }

    override fun getDataFetcher(table: CalciteRootSchema.Table): DataFetcher<Any> {
        return TableDataFetcher2(table)
    }
}

