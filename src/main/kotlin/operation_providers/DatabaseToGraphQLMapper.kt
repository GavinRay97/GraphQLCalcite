package operation_providers

import CalciteRootSchema
import graphql.schema.GraphQLFieldDefinition

interface DatabaseToGraphQLMapper {
    fun handleDatabase(database: CalciteRootSchema.Database): GraphQLFieldDefinition
    fun handleSchema(schema: CalciteRootSchema.Schema): GraphQLFieldDefinition
    fun handleTable(table: CalciteRootSchema.Table): GraphQLFieldDefinition
    fun handleColumn(column: CalciteRootSchema.Column): GraphQLFieldDefinition
}
