package operation_providers

import graphql.Scalars
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLList
import graphql.schema.GraphQLNonNull

object BaseGraphQLTypes {

    val IntComparisonExpressionType = mkComparisonExpressionInputType(
        Scalars.GraphQLInt,
        "Int_comparison_exp",
        "Boolean expression to compare columns of type \"Int\". All fields are combined with logical 'AND'."
    )

    val FloatComparisonExpressionType = mkComparisonExpressionInputType(
        Scalars.GraphQLFloat,
        "Float_comparison_exp",
        "Boolean expression to compare columns of type \"Float\". All fields are combined with logical 'AND'."
    )

    val BooleanComparisonExpressionType = mkComparisonExpressionInputType(
        Scalars.GraphQLBoolean,
        "Boolean_comparison_exp",
        "Boolean expression to compare columns of type \"Boolean\". All fields are combined with logical 'AND'."
    )

    val StringComparisonExpressionType: GraphQLInputObjectType = mkComparisonExpressionInputType(
        Scalars.GraphQLString,
        "String_comparison_exp",
        "Boolean expression to compare columns of type \"String\". All fields are combined with logical 'AND'."
    ).transform { t: GraphQLInputObjectType.Builder ->
        val fields = listOf(
            Pair("_ilike", "does the column match the given case-insensitive pattern"),
            Pair("_like", "does the column match the given pattern"),
            Pair("_nilike", "does the column entity.NOT match the given case-insensitive pattern"),
            Pair("_nlike", "does the column entity.NOT match the given pattern"),
            Pair("_regex", "does the column match the given regular expression"),
            Pair("_nregex", "does the column entity.NOT match the given regular expression"),
            Pair("_iregex", "does the column match the given case-insensitive regular expression"),
            Pair("_niregex", "does the column entity.NOT match the given case-insensitive regular expression"),
            Pair("_similar", "does the column match the given SQL regular expression"),
            Pair("_nsimilar", "does the column entity.NOT match the given SQL regular expression"),
        )
        for ((name, description) in fields) {
            t.field(
                GraphQLInputObjectField.newInputObjectField()
                    .name(name)
                    .description(description)
                    .type(Scalars.GraphQLString)
            )
        }
    }

    private fun mkComparisonExpressionInputType(
        type: GraphQLInputType,
        name: String,
        description: String
    ): GraphQLInputObjectType {
        val inputObject = GraphQLInputObjectType.newInputObject()
            .name(name)
            .description(description)

        for (entry in listOf("_eq", "_ne", "_gt", "_gte", "_lt", "_lte")) {
            inputObject.field(
                GraphQLInputObjectField.newInputObjectField()
                    .name(entry)
                    .type(type)
            )
        }

        for (entry in listOf("_in", "_nin")) {
            inputObject.field(
                GraphQLInputObjectField.newInputObjectField()
                    .name(entry)
                    .type(GraphQLList(GraphQLNonNull(type)))
            )
        }

        return inputObject.build()
    }
}
