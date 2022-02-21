package graphql

import calcite.CalciteRootSchema
import graphql.schema.DataFetcher
import graphql.schema.GraphQLFieldDefinition
import operation_providers.SqlTypeToGraphQLMapping

abstract class TableGQLFieldGenerator(val sqlTypeToGraphQLMapping: SqlTypeToGraphQLMapping) {
    abstract fun getDataFetcher(table: CalciteRootSchema.Table): DataFetcher<Any>

    abstract fun generate(table: CalciteRootSchema.Table): GraphQLFieldDefinition
    open fun shouldGenerate(table: CalciteRootSchema.Table): Boolean {
        return true
    }
}
