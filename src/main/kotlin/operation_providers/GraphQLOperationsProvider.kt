package operation_providers

import CalciteRootSchema
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType

interface GraphQLOperationsProvider {
    val sqlTypeFamilyToGraphQLMapping: SqlTypeFamilyToGraphQLMapping
    fun getQueries(databases: List<CalciteRootSchema.Database>): List<GraphQLFieldDefinition>
    fun getMutations(databases: List<CalciteRootSchema.Database>): List<GraphQLFieldDefinition>
    fun getSubscriptions(databases: List<CalciteRootSchema.Database>): List<GraphQLFieldDefinition>
    fun getTypes(databases: List<CalciteRootSchema.Database>): List<GraphQLType>
    fun getScalars(): List<GraphQLScalarType>
}
