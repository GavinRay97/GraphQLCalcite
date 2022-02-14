package com.example;

import com.example.calcitewrappers.DatabaseManager;
import com.example.calcitewrappers.ForeignKey;
import com.example.calcitewrappers.ForeignKeyManager;
import com.example.calcitewrappers.FullyQualifiedTableName;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;
import org.apache.calcite.util.Pair;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

class SandboxTest {

    // Uncomment the below to enable auto-discovery of entire application
    // @WeldSetup
    // public WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld().enableDiscovery());

    static JDBCDataSource dataSource = new JDBCDataSource();

    @BeforeAll
    static void setUp() throws SQLException {
        dataSource.setURL("jdbc:hsqldb:mem:foreignkeytest1;shutdown=true");

        dataSource.getConnection().createStatement().execute(
                """
                        create table users (
                            id int primary key,
                            name varchar(255)
                        );
                        create table posts (
                            id int primary key,
                            user_id int,
                            title varchar(255),
                            content varchar(255),
                            foreign key (user_id) references users(id)
                        );
                        """);
        dataSource.getConnection().createStatement().execute(
                """
                        insert into users (id, name) values (1, 'John Doe');
                        insert into users (id, name) values (2, 'Jane Doe');
                        insert into posts (id, user_id, title, content) values (1, 1, 'Post 1', 'Content 1');
                        insert into posts (id, user_id, title, content) values (2, 1, 'Post 2', 'Content 2');
                        """);
    }


    @Test
    void something() throws SQLException {
        DatabaseManager calciteSchemaManager = DatabaseManager.getInstance();
        calciteSchemaManager.addDatabase("hsql", dataSource);

        ForeignKeyManager.addForeignKey(
                new ForeignKey(
                        new FullyQualifiedTableName("hsql", "PUBLIC", "POSTS"),
                        new FullyQualifiedTableName("hsql", "PUBLIC", "USERS"),
                        List.of(
                                Pair.of("USER_ID", "ID")
                        )
                )
        );

        com.example.calcitewrappers.DatabaseManager databaseManager = com.example.calcitewrappers.DatabaseManager.getInstance();
        SqlToGraphQLConfiguration sqlToGraphQLConfiguration = new DefaultSqlToGraphQLConfiguration();

        SchemaGenerationProvider schemaGenerationProvider = new SchemaGenerationProvider(databaseManager, sqlToGraphQLConfiguration);
        schemaGenerationProvider.addOperationProvider(
                new GraphQLFindAllQueryProvider(schemaGenerationProvider));

        GraphQLSchema graphQLSchema = schemaGenerationProvider.buildSchema();
        SchemaPrinter printer = new SchemaPrinter();
        System.out.println(printer.print(graphQLSchema));


        GraphQL build = GraphQL.newGraphQL(graphQLSchema).build();

        String query = """
                query {
                    hsql {
                        PUBLIC {
                            POSTS(where: { ID: { _lt: 2  } }) {
                                ID
                                TITLE
                                USER_ID
                                USERS {
                                    ID
                                    NAME
                                }
                            }
                        }
                      }
                  }
                """;

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .build();

        for (int i = 0; i < 10; i++) {
            Instant start = Instant.now();
            ExecutionResult executionResult = build.execute(executionInput);
            Instant finish = Instant.now();
            long timeElapsed = Duration.between(start, finish).toMillis();
            System.out.println(timeElapsed);
        }

    }
}
