package com.example;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;

import java.util.List;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;

public class DefaultGraphQLInputTypes {

    static GraphQLInputTypeForScalar GRAPHQL_INPUT_TYPE_FOR_INT = new GraphQLInputTypeForScalar(GraphQLInt,
            "Int_comparison_exp") {
    };

    static GraphQLInputTypeForScalar GRAPHQL_INPUT_TYPE_FOR_FLOAT = new GraphQLInputTypeForScalar(GraphQLFloat,
            "Float_comparison_exp") {
    };

    static GraphQLInputTypeForScalar GRAPHQL_INPUT_TYPE_FOR_BOOLEAN = new GraphQLInputTypeForScalar(GraphQLBoolean,
            "Boolean_comparison_exp") {

        final GraphQLInputObjectType inputObjectType = GraphQLInputObjectType
                .newInputObject()
                .name(name)
                .field(
                        GraphQLInputObjectField
                                .newInputObjectField()
                                .name("_is_null")
                                .type(GraphQLBoolean)
                                .build())
                .field(
                        GraphQLInputObjectField
                                .newInputObjectField()
                                .name("_eq")
                                .type(GraphQLBoolean)
                                .build())
                .field(
                        GraphQLInputObjectField
                                .newInputObjectField()
                                .name("_ne")
                                .type(GraphQLBoolean)
                                .build())
                .build();

        @Override
        public GraphQLInputObjectType getInputObjectType() {
            return inputObjectType;
        }
    };

    static GraphQLInputTypeForScalar GRAPHQL_INPUT_TYPE_FOR_STRING = new GraphQLInputTypeForScalar(GraphQLString,
            "String_comparison_exp") {

        final GraphQLInputObjectType inputObjectType = super.getInputObjectType()
                .transform(builder -> {
                    for (String entry : List.of("_similar", "_nsimilar", "_regex", "_nregex", "_iregex", "_niregex")) {
                        builder.field(
                                GraphQLInputObjectField.newInputObjectField()
                                        .name(entry)
                                        .type(scalarType)
                                        .build());
                    }
                });

        @Override
        public GraphQLInputObjectType getInputObjectType() {
            return inputObjectType;
        }

    };
}
