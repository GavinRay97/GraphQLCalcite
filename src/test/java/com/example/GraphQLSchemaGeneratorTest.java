//package com.example;
//
//import graphql.ExecutionResult;
//import graphql.GraphQL;
//import graphql.schema.GraphQLSchema;
//import graphql.schema.idl.SchemaPrinter;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//import java.util.Map;
//
//public class GraphQLSchemaGeneratorTest {
//
//    @Test
//    void test() {
//        // Given
//        HrClusteredSchema schema = new HrClusteredSchema();
//        CalciteSchemaManager.rootSchema.add("hr", schema);
//
//        GraphQLSchema graphqlSchema = GraphQLSchemaGenerator.INSTANCE.generateGraphQLSchemaFromRootSchema(CalciteSchemaManager.rootSchema);
//
//        SchemaPrinter schemaPrinter = new SchemaPrinter();
//        System.out.println(schemaPrinter.print(graphqlSchema));
//
//        String graphqlQuery = """
//                query {
//                     hr {
//                         emps(where: {empid: {_eq: 100 }}) {
//                             empid
//                             name
//                             deptno
//                         }
//                     }
//                 }
//                 """;
//
//        // When
//        GraphQL build = GraphQL.newGraphQL(graphqlSchema).build();
//        ExecutionResult executionResult = build.execute(graphqlQuery);
//
//        // Then
//        Map<String, Object> result = executionResult.getData();
//        System.out.println("Query result: " + result.toString());
//
//        assertThat(result).isNotNull();
//
//        Map<String, Object> hr = (Map<String, Object>) result.get("hr");
//        assertThat(hr).isNotNull();
//
//        List<Map<String, Object>> emps = (List<Map<String, Object>>) hr.get("emps");
//        assertThat(emps).isNotNull();
//        assertThat(emps.size()).isEqualTo(1);
//    }
//
//}
