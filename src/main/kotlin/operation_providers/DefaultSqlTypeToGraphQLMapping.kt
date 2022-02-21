package operation_providers

import graphql.Scalars
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLType
import org.apache.calcite.sql.type.SqlTypeName

object DefaultSqlTypeToGraphQLMapping : SqlTypeToGraphQLMapping {
    override fun toGraphQLScalarType(type: SqlTypeName): GraphQLType {
        return when (type) {
            SqlTypeName.BOOLEAN -> Scalars.GraphQLBoolean
            SqlTypeName.TINYINT -> Scalars.GraphQLInt
            SqlTypeName.SMALLINT -> Scalars.GraphQLInt
            SqlTypeName.INTEGER -> Scalars.GraphQLInt
            SqlTypeName.BIGINT -> Scalars.GraphQLInt
            SqlTypeName.FLOAT -> Scalars.GraphQLFloat
            SqlTypeName.REAL -> Scalars.GraphQLFloat
            SqlTypeName.DOUBLE -> Scalars.GraphQLFloat
            SqlTypeName.DECIMAL -> Scalars.GraphQLFloat
            SqlTypeName.CHAR -> Scalars.GraphQLString
            SqlTypeName.VARCHAR -> Scalars.GraphQLString
            SqlTypeName.DATE -> Scalars.GraphQLString
            SqlTypeName.TIME -> Scalars.GraphQLString
            SqlTypeName.TIMESTAMP -> Scalars.GraphQLString
            SqlTypeName.BINARY -> Scalars.GraphQLString
            SqlTypeName.VARBINARY -> Scalars.GraphQLString
            else -> throw IllegalArgumentException("Unknown type $type")
        }
    }

    override fun toGraphQLInputType(type: SqlTypeName): GraphQLInputObjectType {
        return when (type) {
            SqlTypeName.BOOLEAN -> BaseGraphQLTypes.BooleanComparisonExpressionType
            SqlTypeName.TINYINT -> BaseGraphQLTypes.IntComparisonExpressionType
            SqlTypeName.SMALLINT -> BaseGraphQLTypes.IntComparisonExpressionType
            SqlTypeName.INTEGER -> BaseGraphQLTypes.IntComparisonExpressionType
            SqlTypeName.BIGINT -> BaseGraphQLTypes.IntComparisonExpressionType
            SqlTypeName.FLOAT -> BaseGraphQLTypes.FloatComparisonExpressionType
            SqlTypeName.REAL -> BaseGraphQLTypes.FloatComparisonExpressionType
            SqlTypeName.DOUBLE -> BaseGraphQLTypes.FloatComparisonExpressionType
            SqlTypeName.DECIMAL -> BaseGraphQLTypes.FloatComparisonExpressionType
            SqlTypeName.CHAR -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeName.VARCHAR -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeName.DATE -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeName.TIME -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeName.TIMESTAMP -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeName.BINARY -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeName.VARBINARY -> BaseGraphQLTypes.StringComparisonExpressionType
            else -> throw IllegalArgumentException("Unknown type $type")
        }
    }
}
