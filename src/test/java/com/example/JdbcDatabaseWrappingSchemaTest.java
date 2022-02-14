package com.example;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

@Testcontainers
public class JdbcDatabaseWrappingSchemaTest extends AbstractContainerDatabaseTest {

    @Container
    private static final PostgreSQLContainer postgresContainer = new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName("test-postgres");

    private final DataSource datasource = getDataSource(postgresContainer);

    @Test
    void test() {

    }

}
