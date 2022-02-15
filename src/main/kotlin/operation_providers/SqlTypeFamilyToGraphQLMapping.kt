package operation_providers

import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLType
import org.apache.calcite.sql.type.SqlTypeFamily

interface SqlTypeFamilyToGraphQLMapping {
    fun toGraphQLScalarType(family: SqlTypeFamily): GraphQLType
    fun toGraphQLInputType(family: SqlTypeFamily): GraphQLInputObjectType
}
