package graphql.operationgenerators

import calcite.CalciteRootSchema
import calcite.CalciteSchemaManager
import graphql.Scalars.GraphQLInt
import graphql.TableGQLFieldGenerator
import graphql.schema.DataFetcher
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLList
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLTypeReference
import operation_providers.DefaultSqlTypeToGraphQLMapping

// TODO: Figure out support for upserts. See "MERGE" operator in Calcite repository.
// SEE: https://github.com/apache/calcite/blob/4bc916619fd286b2c0cc4d5c653c96a68801d74e/core/src/main/java/org/apache/calcite/sql/SqlMerge.java
object InsertManyMutationGenerator : TableGQLFieldGenerator(DefaultSqlTypeToGraphQLMapping) {
    override fun generate(table: CalciteRootSchema.Table): GraphQLFieldDefinition {
        return GraphQLFieldDefinition.newFieldDefinition()
            .name(table.name)
            .dataFetcher(getDataFetcher(table))
            .type(
                GraphQLObjectType.newObject()
                    .name(
                        if (table.schema == null)
                            table.database.name + "_" + table.name + "_mutation_response"
                        else
                            table.database.name + "_" + table.schema.name + "_" + table.name + "_mutation_response"
                    )
                    .field(
                        GraphQLFieldDefinition.newFieldDefinition()
                            .name("affected_rows")
                            .type(GraphQLNonNull.nonNull(GraphQLInt))
                            .build()
                    )
                    .field(
                        GraphQLFieldDefinition.newFieldDefinition()
                            .name("returning")
                            .type(
                                GraphQLNonNull(
                                    GraphQLList(
                                        GraphQLTypeReference(
                                            if (table.schema == null)
                                                table.database.name + "_" + table.name + "_type"
                                            else
                                                table.database.name + "_" + table.schema.name + "_" + table.name + "_type"
                                        )
                                    )
                                )
                            )
                            .build()
                    )
            )
            .argument(
                GraphQLArgument.newArgument()
                    .name("objects")
                    .type(
                        GraphQLNonNull(
                            GraphQLList(
                                GraphQLNonNull(
                                    GraphQLTypeReference(
                                        if (table.schema == null)
                                            table.database.name + "_" + table.name + "_insert_input"
                                        else
                                            table.database.name + "_" + table.schema.name + "_" + table.name + "_insert_input"
                                    )
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()
    }

    override fun getDataFetcher(table: CalciteRootSchema.Table): DataFetcher<Any> {
        return DataFetcher { env ->
            val objects = env.getArgument<List<Map<String, Any>>>("objects")
            val keys = objects.map { it.keys }.flatten().toSet()

            var query = """
                INSERT INTO ${table.quotedTableName}
                   (${keys.joinToString(", ")})
                VALUES
            """

            for (i in objects.indices) {
                query += "("
                for (key in keys) {
                    val entry = objects[i][key]
                    query += when (entry) {
                        null -> "DEFAULT"
                        is String -> "'$entry'"
                        else -> entry
                    }
                    if (key != keys.last())
                        query += ", "
                }
                query += ")"
                if (i != objects.size - 1)
                    query += ", "
            }


            println("query")
            println(query)

            val result = CalciteSchemaManager.executeUpdate(query)
            println("result = $result")

            mapOf(
                "affected_rows" to result,
                "returning" to listOf<Any>()
            )
        }
    }
}

