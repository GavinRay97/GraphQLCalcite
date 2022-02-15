package operation_providers

import CalciteRootSchema
import argument
import field
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import inputObjectField
import inputObjectType
import objectType

/**
 * A [GraphQLOperationsProvider] that provides "find-by-pk" style queries
 */
object GraphQLFindByPrimaryKeyProviderImpl : GraphQLOperationsProvider {

    override val sqlTypeFamilyToGraphQLMapping = DefaultSqlTypeFamilyToGraphQLMappingImpl

    override fun getQueries(databases: List<CalciteRootSchema.Database>): List<GraphQLFieldDefinition> {
        val mapper = object : DatabaseToGraphQLMapper {
            override fun handleDatabase(database: CalciteRootSchema.Database): GraphQLFieldDefinition {
                return field {
                    name = database.name
                    type = objectType {
                        name = database.name + "_type"
                        fields = if (database.schemas.isEmpty())
                            database.tables.map(::handleTable)
                        else
                            database.schemas.map(::handleSchema)
                    }
                }
            }

            override fun handleSchema(schema: CalciteRootSchema.Schema): GraphQLFieldDefinition {
                return field {
                    name = schema.name
                    type = objectType {
                        name = schema.name + "_type"
                        fields = schema.tables.map(::handleTable)
                    }
                }
            }

            override fun handleTable(table: CalciteRootSchema.Table): GraphQLFieldDefinition {
                return field {
                    name = table.name + "_by_pk"
                    type = objectType {
                        name = table.name + "_type"
                        // Get the primary key columns
                        fields = table.columns.map(::handleColumn)
                    }
                    arguments = listOf(
                        argument {
                            name = "pk"
                            type = inputObjectType {
                                fields = table.columns.filter { it.isPrimaryKey }.map {
                                    inputObjectField {
                                        name = it.name
                                        type =
                                            sqlTypeFamilyToGraphQLMapping.toGraphQLScalarType(it.family) as GraphQLInputType
                                    }
                                }
                            }
                        }
                    )
                }
            }

            override fun handleColumn(column: CalciteRootSchema.Column): GraphQLFieldDefinition {
                return field {
                    name = column.name
                    type = sqlTypeFamilyToGraphQLMapping.toGraphQLScalarType(column.family) as GraphQLOutputType
                }
            }
        }
        return databases.map(mapper::handleDatabase)
    }

    override fun getMutations(databases: List<CalciteRootSchema.Database>): List<GraphQLFieldDefinition> {
        return emptyList()
    }

    override fun getSubscriptions(databases: List<CalciteRootSchema.Database>): List<GraphQLFieldDefinition> {
        return emptyList()
    }

    override fun getTypes(databases: List<CalciteRootSchema.Database>): List<GraphQLType> {
        return emptyList()
    }

    override fun getScalars(): List<GraphQLScalarType> {
        return emptyList()
    }
}
