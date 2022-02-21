package graphql

import calcite.CalciteRootSchema
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema
import operation_providers.SqlTypeToGraphQLMapping

class GraphQLSchemaGeneratorNew(
    private val calciteRootSchema: CalciteRootSchema,
    private val sqlTypeToGraphQLMapping: SqlTypeToGraphQLMapping
) {
    fun generate(
        queryGenerators: List<TableGQLFieldGenerator> = listOf(),
        mutationGenerators: List<TableGQLFieldGenerator> = listOf(),
        subscriptionGenerators: List<TableGQLFieldGenerator> = listOf(),
    ): GraphQLSchema {
        val schema = GraphQLSchema.newSchema()
        val query = GraphQLObjectType.newObject().name("Query")
        val mutation = GraphQLObjectType.newObject().name("Mutation")
        val subscription = GraphQLObjectType.newObject().name("Subscription")

        if (queryGenerators.isNotEmpty()) {
            query.fields(mkFields("query", queryGenerators))
            schema.query(query)
        }

        if (mutationGenerators.isNotEmpty()) {
            mutation.fields(mkFields("mutation", mutationGenerators))
            schema.mutation(mutation)
        }

        if (subscriptionGenerators.isNotEmpty()) {
            subscription.fields(mkFields("subscription", subscriptionGenerators))
            schema.subscription(subscription)
        }

        // Build the table GraphQLObjectTypes for the tables in each DB and Schema
        calciteRootSchema.databases().forEach { db ->
            db.tables().forEach { table ->
                schema.additionalType(mkTableObjectType(table))
                schema.additionalType(mkTableInsertObjectType(table))
            }
            db.schemas().forEach { dbSchema ->
                dbSchema.tables().forEach { table ->
                    schema.additionalType(mkTableObjectType(table))
                    schema.additionalType(mkTableInsertObjectType(table))
                }
            }
        }

        return schema.build()
    }

    // TODO: There might be hard-to-diagnose bugs later if "generators.filter()" returns zero fields
    // It would cause an error creating the ObjectType because you can't pass an empty list to "fields()" builder
    private fun mkFields(
        typeNameSuffix: String,
        generators: List<TableGQLFieldGenerator>
    ): List<GraphQLFieldDefinition> {
        return calciteRootSchema.databases().map { db ->
            GraphQLFieldDefinition.newFieldDefinition()
                .name(db.underlying.name)
                .dataFetcher { "no-op" }
                .type(
                    GraphQLObjectType.newObject()
                        .name(db.underlying.name + "_" + typeNameSuffix + "_type")
                        .fields(
                            db.tables().flatMap { table ->
                                generators
                                    .filter {
                                        it.shouldGenerate(table)
                                    }
                                    .map {
                                        it.generate(table)
                                    }
                            }
                        )
                        .fields(
                            db.schemas().map { schema ->
                                GraphQLFieldDefinition.newFieldDefinition()
                                    .name(schema.underlying.name)
                                    .dataFetcher { "no-op" }
                                    .type(
                                        GraphQLObjectType.newObject()
                                            .name(db.underlying.name + "_" + schema.underlying.name + "_" + typeNameSuffix + "_type")
                                            .fields(
                                                schema.tables().flatMap { table ->
                                                    generators
                                                        .filter {
                                                            it.shouldGenerate(table)
                                                        }
                                                        .map {
                                                            it.generate(table)
                                                        }
                                                }
                                            )
                                            .build()
                                    )
                                    .build()
                            }
                        )
                        .build()
                )
                .build()
        }
    }

    // Makes the GraphQLObjectType for a table
    // This type should be used via GraphQLTypeReference in graphql.TableGQLFieldGenerator implementations
    private fun mkTableObjectType(table: CalciteRootSchema.Table): GraphQLObjectType {
        return GraphQLObjectType.newObject()
            .name(
                if (table.schema == null)
                    table.database.name + "_" + table.name + "_type"
                else
                    table.database.name + "_" + table.schema.name + "_" + table.name + "_type"
            )
            .fields(
                table.columns().map {
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(it.name)
                        .type(sqlTypeToGraphQLMapping.toGraphQLScalarType(it.underlying.type.sqlTypeName) as GraphQLOutputType)
                        .build()
                }
            )
            .build()
    }

    // Makes the GraphQLObjectType for a table insert object
    // This type should be used via GraphQLTypeReference in graphql.TableGQLFieldGenerator implementations
    private fun mkTableInsertObjectType(table: CalciteRootSchema.Table): GraphQLInputObjectType {
        return GraphQLInputObjectType.newInputObject()
            .name(
                if (table.schema == null)
                    table.database.name + "_" + table.name + "_insert_input"
                else
                    table.database.name + "_" + table.schema.name + "_" + table.name + "_insert_input"
            )
            .fields(
                table.columns().map {
                    GraphQLInputObjectField.newInputObjectField()
                        .name(it.name)
                        .type(sqlTypeToGraphQLMapping.toGraphQLScalarType(it.underlying.type.sqlTypeName) as GraphQLInputType)
                        .build()
                }
            )
            .build()
    }
}
