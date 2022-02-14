//package com.example;
//
//import org.apache.calcite.schema.SchemaPlus;
//import org.apache.calcite.sql.parser.SqlParseException;
//import org.apache.calcite.tools.RelConversionException;
//import org.apache.calcite.tools.ValidationException;
//import org.junit.jupiter.api.Test;
//import org.testcontainers.containers.MySQLContainer;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import javax.sql.DataSource;
//import java.sql.SQLException;
//
//@Testcontainers
//class CalciteSchemaManagerTest extends AbstractContainerDatabaseTest {
//
//    @Container
//    private static final PostgreSQLContainer postgresContainer = new PostgreSQLContainer<>("postgres:14")
//            .withDatabaseName("test-postgres");
//
//    @Container
//    private static final MySQLContainer mysqlContainer1 = new MySQLContainer<>("mysql:8")
//            .withDatabaseName("test-mysql1");
//
//    @Container
//    private static final MySQLContainer mysqlContainer2 = new MySQLContainer<>("mysql:8")
//            .withDatabaseName("test-mysql2");
//
//    private final DataSource postgresDatasource = getDataSource(postgresContainer);
//    private final DataSource mysqlDatasource1 = getDataSource(mysqlContainer1);
//    private final DataSource mysqlDatasource2 = getDataSource(mysqlContainer2);
//
//    private void createTestTables() throws SQLException {
//        postgresDatasource.getConnection().createStatement()
//                .execute("""
//                        CREATE TABLE test_root_pg_table (id INTEGER, name VARCHAR(255));
//
//                        CREATE SCHEMA test_pg_schema_1;
//                        CREATE TABLE test_pg_schema_1.test_pg_table_1 (
//                            id INTEGER,
//                            name VARCHAR(255)
//                        );
//                        CREATE TABLE test_pg_schema_1.test_pg_table_2 (
//                            id INTEGER,
//                            name VARCHAR(255)
//                        );
//
//                        INSERT INTO test_pg_schema_1.test_pg_table_1 (id, name) VALUES (1, 'test_pg_table_1_1');
//
//                        CREATE SCHEMA test_pg_schema_2;
//                        CREATE TABLE test_pg_schema_2.test_pg_table_2 (
//                            id INTEGER,
//                            name VARCHAR(255)
//                        );
//                        INSERT INTO test_pg_schema_2.test_pg_table_2 (id, name) VALUES (1, 'test_pg_table_2_1');
//                        """);
//
//        mysqlDatasource1.getConnection().createStatement()
//                .execute("""
//                            CREATE TABLE test_mysql_table_1 (id INTEGER, name VARCHAR(255));
//                        """);
//        mysqlDatasource1.getConnection().createStatement()
//                .execute("""
//                           INSERT INTO test_mysql_table_1 (id, name) VALUES (1, 'test_mysql_table_1_1');
//                        """);
//
//        mysqlDatasource2.getConnection().createStatement()
//                .execute("""
//                            CREATE TABLE test_mysql_table_2 (id INTEGER, name VARCHAR(255));
//                        """);
//    }
//
//    @Test
//    void test() throws SQLException, ValidationException, SqlParseException, RelConversionException {
//        // given
//        createTestTables();
//        SchemaPlus rootSchema = CalciteSchemaManager.rootSchema;
//        CalciteSchemaManager.addDatabase("pg_db1", postgresDatasource);
//        CalciteSchemaManager.addDatabase("mysql_db1", mysqlDatasource1);
//        CalciteSchemaManager.addDatabase("mysql_db2", mysqlDatasource2);
//
//
//        System.out.println(CalciteSchemaPrinter.printSchema(rootSchema));
//
//        CalciteUtils.executeQuery(CalciteSchemaManager.frameworkConfig, "select * from pg_db1.test_pg_schema_1.test_pg_table_1", true);
//
//        // then
////        assertThat(rootSchema.getSubSchemaNames()).contains("test-postgres", "test-mysql");
////
////        assertThat(postgres.getSubSchemaNames()).contains("test_pg_schema_1");
////        assertThat(postgres.getSubSchema("test_pg_schema_1").getTableNames()).contains("test_pg_table_1");
////
////        assertThat(mysql1.getSubSchemaNames()).contains("root");
////        assertThat(mysql1.getSubSchema("root").getTableNames()).contains("test_mysql_table_1");
////
////        assertThat(mysql1.getSubSchemaNames()).contains("root");
////        assertThat(mysql1.getSubSchema("root").getTableNames()).contains("test_mysql_table_1");
//    }
//
//}
//
//
