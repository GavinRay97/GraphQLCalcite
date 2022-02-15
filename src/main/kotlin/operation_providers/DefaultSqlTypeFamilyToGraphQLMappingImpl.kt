package operation_providers

import graphql.Scalars
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLType
import org.apache.calcite.sql.type.SqlTypeFamily

object DefaultSqlTypeFamilyToGraphQLMappingImpl : SqlTypeFamilyToGraphQLMapping {
    override fun toGraphQLScalarType(family: SqlTypeFamily): GraphQLType {
        return when (family) {
            SqlTypeFamily.BOOLEAN -> Scalars.GraphQLBoolean
            SqlTypeFamily.INTEGER -> Scalars.GraphQLInt
            SqlTypeFamily.NUMERIC, SqlTypeFamily.DECIMAL -> Scalars.GraphQLFloat
            SqlTypeFamily.CHARACTER, SqlTypeFamily.STRING -> Scalars.GraphQLString
            SqlTypeFamily.DATE, SqlTypeFamily.DATETIME -> Scalars.GraphQLString
            SqlTypeFamily.TIME, SqlTypeFamily.TIMESTAMP -> Scalars.GraphQLString
            else -> throw IllegalArgumentException("Unknown type $family")
        }
    }

    override fun toGraphQLInputType(family: SqlTypeFamily): GraphQLInputObjectType {
        return when (family) {
            SqlTypeFamily.BOOLEAN -> BaseGraphQLTypes.BooleanComparisonExpressionType
            SqlTypeFamily.INTEGER -> BaseGraphQLTypes.IntComparisonExpressionType
            SqlTypeFamily.NUMERIC, SqlTypeFamily.DECIMAL -> BaseGraphQLTypes.FloatComparisonExpressionType
            SqlTypeFamily.CHARACTER, SqlTypeFamily.STRING -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeFamily.DATE, SqlTypeFamily.DATETIME -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeFamily.TIME, SqlTypeFamily.TIMESTAMP -> BaseGraphQLTypes.StringComparisonExpressionType
            else -> throw IllegalArgumentException("Unknown type $family")
        }
    }
}
