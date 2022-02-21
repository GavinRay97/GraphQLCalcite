package operation_providers

import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLType
import org.apache.calcite.sql.type.SqlTypeName

interface SqlTypeToGraphQLMapping {
    fun toGraphQLScalarType(type: SqlTypeName): GraphQLType
    fun toGraphQLInputType(type: SqlTypeName): GraphQLInputObjectType
}
