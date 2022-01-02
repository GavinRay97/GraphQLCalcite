package extensions

import BaseGraphQLTypes
import graphql.Scalars
import graphql.schema.GraphQLType
import org.apache.calcite.rel.type.RelDataType
import org.apache.calcite.sql.type.SqlTypeFamily

fun RelDataType.toGraphQLType(): GraphQLType {
    return when (this.family) {
        SqlTypeFamily.BOOLEAN -> Scalars.GraphQLBoolean
        SqlTypeFamily.INTEGER -> Scalars.GraphQLInt

        SqlTypeFamily.NUMERIC, SqlTypeFamily.DECIMAL -> Scalars.GraphQLFloat
        SqlTypeFamily.CHARACTER, SqlTypeFamily.STRING -> Scalars.GraphQLString

        SqlTypeFamily.DATE, SqlTypeFamily.DATETIME -> Scalars.GraphQLString
        SqlTypeFamily.TIME, SqlTypeFamily.TIMESTAMP -> Scalars.GraphQLString

        else -> throw IllegalArgumentException("Unknown type: $this")
    }
}

fun RelDataType.toGraphQLComparisonExprType(): GraphQLType {
    return when (this.family) {
        SqlTypeFamily.BOOLEAN -> BaseGraphQLTypes.BooleanComparisonExpressionType
        SqlTypeFamily.INTEGER -> BaseGraphQLTypes.IntComparisonExpressionType

        SqlTypeFamily.NUMERIC, SqlTypeFamily.DECIMAL -> BaseGraphQLTypes.FloatComparisonExpressionType
        SqlTypeFamily.CHARACTER, SqlTypeFamily.STRING -> BaseGraphQLTypes.StringComparisonExpressionType

        SqlTypeFamily.DATE, SqlTypeFamily.DATETIME -> BaseGraphQLTypes.StringComparisonExpressionType
        SqlTypeFamily.TIME, SqlTypeFamily.TIMESTAMP -> BaseGraphQLTypes.StringComparisonExpressionType

        else -> throw IllegalArgumentException("Unknown type: $this")
    }
}
