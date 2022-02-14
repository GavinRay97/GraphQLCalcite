//package com.example;
//
//import org.apache.calcite.schema.SchemaPlus;
//import org.junit.jupiter.api.Test;
//import org.testcontainers.containers.PostgreSQLContainer;
//
//import javax.sql.DataSource;
//import java.sql.Connection;
//import java.sql.DatabaseMetaData;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//
//
//class JdbcTableWithReferentialConstraints {
//
//    static void getForeignKeysForTable(Connection connection, String tableName) throws SQLException {
//        DatabaseMetaData metaData = connection.getMetaData();
//        System.out.println(connection.getCatalog());
//        System.out.println(connection.getSchema());
//        ResultSet foreignKeys = metaData.getImportedKeys(null, connection.getSchema(), tableName);
//        while (foreignKeys.next()) {
//            String fkTableName = foreignKeys.getString("FKTABLE_NAME");
//            String fkColumnName = foreignKeys.getString("FKCOLUMN_NAME");
//            String pkTableName = foreignKeys.getString("PKTABLE_NAME");
//            String pkColumnName = foreignKeys.getString("PKCOLUMN_NAME");
//            System.out.println(fkTableName + "." + fkColumnName + " -> " + pkTableName + "." + pkColumnName);
//        }
//    }
//
//}
//
//public class CalciteForeignKeyTest extends AbstractContainerDatabaseTest {
//
//    @Test
//    void test() throws SQLException {
//        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14")) {
//            postgres.start();
//
//            DataSource dataSource = getDataSource(postgres);
//            dataSource.getConnection().createStatement().execute(
//                    """
//                            CREATE TABLE "public"."artist" (
//                                "id" integer NOT NULL PRIMARY KEY,
//                                "name" varchar(255) NOT NULL
//                            );
//                            CREATE TABLE "public"."album" (
//                                "id" integer NOT NULL PRIMARY KEY,
//                                "name" varchar(255) NOT NULL,
//                                "artist_id" integer NOT NULL REFERENCES "public"."artist" ("id")
//                            );
//                            """);
//
//            CalciteSchemaManager.addDatabase("pg1", dataSource);
//
//            System.out.println(CalciteSchemaPrinter.printSchema(CalciteSchemaManager.rootSchema));
//            SchemaPlus schema = CalciteSchemaManager.rootSchema.getSubSchema("pg1").getSubSchema("public");
//        }
//    }
//
//}
