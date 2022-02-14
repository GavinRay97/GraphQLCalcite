package com.example;

import com.google.common.collect.ImmutableMap;
import graphql.schema.GraphQLScalarType;
import org.apache.calcite.sql.type.SqlTypeFamily;

import java.util.Map;

import static graphql.Scalars.*;

public class DefaultSqlToGraphQLConfiguration implements SqlToGraphQLConfiguration {

    static Map<SqlTypeFamily, GraphQLScalarType> sqlTypeFamilyGraphQLScalarTypeMap = ImmutableMap
            .<SqlTypeFamily, GraphQLScalarType>builder()
            .put(SqlTypeFamily.BOOLEAN, GraphQLBoolean)
            .put(SqlTypeFamily.NUMERIC, GraphQLFloat)
            .put(SqlTypeFamily.INTEGER, GraphQLInt)
            .put(SqlTypeFamily.DATE, GraphQLString)
            .put(SqlTypeFamily.TIME, GraphQLString)
            .put(SqlTypeFamily.TIMESTAMP, GraphQLString)
            .put(SqlTypeFamily.STRING, GraphQLString)
            .put(SqlTypeFamily.CHARACTER, GraphQLString)
            .build();

    @Override
    public Map<SqlTypeFamily, GraphQLScalarType> getSqlTypeFamilyGraphQLScalarTypeMap() {
        return sqlTypeFamilyGraphQLScalarTypeMap;
    }

    static Map<GraphQLScalarType, GraphQLInputTypeForScalar> graphqlScalarTypeToGraphQLInputTypeMap = ImmutableMap
            .<GraphQLScalarType, GraphQLInputTypeForScalar>builder()
            .put(GraphQLBoolean, DefaultGraphQLInputTypes.GRAPHQL_INPUT_TYPE_FOR_BOOLEAN)
            .put(GraphQLFloat, DefaultGraphQLInputTypes.GRAPHQL_INPUT_TYPE_FOR_FLOAT)
            .put(GraphQLInt, DefaultGraphQLInputTypes.GRAPHQL_INPUT_TYPE_FOR_INT)
            .put(GraphQLString, DefaultGraphQLInputTypes.GRAPHQL_INPUT_TYPE_FOR_STRING)
            .build();

    @Override
    public Map<GraphQLScalarType, GraphQLInputTypeForScalar> getGraphqlScalarTypeToGraphQLInputTypeMap() {
        return graphqlScalarTypeToGraphQLInputTypeMap;
    }

}
