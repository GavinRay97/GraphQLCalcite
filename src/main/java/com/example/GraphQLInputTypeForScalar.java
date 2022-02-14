package com.example;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLScalarType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static graphql.Scalars.GraphQLBoolean;

abstract class GraphQLInputTypeForScalar {
    String name;
    GraphQLScalarType scalarType;
    GraphQLInputObjectType inputObjectType;

    GraphQLInputTypeForScalar(@NotNull GraphQLScalarType scalarType, @NotNull String name) {
        this.name = name;
        this.scalarType = scalarType;
        this.inputObjectType = GraphQLInputObjectType.newInputObject()
                .name(name)
                .fields(getBaseInputFields(scalarType))
                .build();
    }


    public GraphQLInputObjectType getInputObjectType() {
        return inputObjectType;
    }

    public Expression handleQueryFilterExpression(QueryFilterExpression queryFilterExpression) {
        Expression expression = defaultQueryFilterExpressionHandler(queryFilterExpression);
        if (expression == null) {
            throw new IllegalArgumentException("Unsupported query filter expression: " + queryFilterExpression);
        }
        return expression;
    }

    // ------------------------------------------------------------------------------------------------
    private static final List<String> BASE_LIST_VALUE_COMPARISON_EXPS = List.of("_in", "_nin");

    private static final List<String> BASE_SINGLE_VALUE_COMPARISON_EXPS = List.of("_eq", "_ne", "_gt", "_gte", "_lt",
            "_lte");

    private static List<GraphQLInputObjectField> getBaseInputFields(GraphQLScalarType scalarType) {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add(
                GraphQLInputObjectField.newInputObjectField()
                        .name("_is_null")
                        .type(GraphQLBoolean)
                        .build());

        for (String entry : BASE_SINGLE_VALUE_COMPARISON_EXPS) {
            fields.add(
                    GraphQLInputObjectField
                            .newInputObjectField()
                            .name(entry)
                            .type(scalarType)
                            .build());
        }

        for (String entry : BASE_LIST_VALUE_COMPARISON_EXPS) {
            fields.add(
                    GraphQLInputObjectField
                            .newInputObjectField()
                            .name(entry)
                            .type(new GraphQLList(scalarType))
                            .build());
        }

        return fields;
    }

    @Nullable
    static Expression defaultQueryFilterExpressionHandler(QueryFilterExpression filterExpression) {
        String column = filterExpression.column();
        String operator = filterExpression.operator();
        Object value = filterExpression.value();

        return switch (operator) {
            case "_eq" -> new Expression.EQ(new Expression.Column(column), new Expression.Literal(value));
            case "_ne" -> new Expression.NEQ(new Expression.Column(column), new Expression.Literal(value));
            case "_lt" -> new Expression.LT(new Expression.Column(column), new Expression.Literal(value));
            case "_le" -> new Expression.LTE(new Expression.Column(column), new Expression.Literal(value));
            case "_gt" -> new Expression.GT(new Expression.Column(column), new Expression.Literal(value));
            case "_ge" -> new Expression.GTE(new Expression.Column(column), new Expression.Literal(value));
            case "_in" -> new Expression.IN(new Expression.Column(column), new Expression.Literal(value));
            case "_nin" -> new Expression.NIN(new Expression.Column(column), new Expression.Literal(value));
            case "_like" -> new Expression.LIKE(new Expression.Column(column), new Expression.Literal(value));
            case "_nlike" -> new Expression.NLIKE(new Expression.Column(column), new Expression.Literal(value));
            case "_similar" -> new Expression.SIMILAR(new Expression.Column(column), new Expression.Literal(value));
            case "_nsimilar" -> new Expression.NSIMILAR(new Expression.Column(column), new Expression.Literal(value));
            case "_regex" -> new Expression.REGEX(new Expression.Column(column), new Expression.Literal(value));
            case "_nregex" -> new Expression.NREGEX(new Expression.Column(column), new Expression.Literal(value));
            case "_iregex" -> new Expression.IREGEX(new Expression.Column(column), new Expression.Literal(value));
            case "_niregex" -> new Expression.NIREGEX(new Expression.Column(column), new Expression.Literal(value));
            case "_is_null" -> new Expression.IS_NULL(new Expression.Column(column));
            default -> null;
        };
    }
}
