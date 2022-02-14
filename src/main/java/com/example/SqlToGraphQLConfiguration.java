package com.example;

import graphql.schema.GraphQLScalarType;
import org.apache.calcite.sql.type.SqlTypeFamily;

import java.util.Map;

public interface SqlToGraphQLConfiguration {
    Map<SqlTypeFamily, GraphQLScalarType> getSqlTypeFamilyGraphQLScalarTypeMap();

    Map<GraphQLScalarType, GraphQLInputTypeForScalar> getGraphqlScalarTypeToGraphQLInputTypeMap();
}
