package com.example.calcitewrappers;

import com.example.CalciteUtils;
import com.google.common.base.Suppliers;
import org.apache.calcite.adapter.enumerable.EnumerableConvention;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.Driver;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.tools.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.function.Supplier;

public class DatabaseManager {
    private final CalciteConnection connection;
    public final FrameworkConfig frameworkConfig;

    DatabaseManager(CalciteConnection connection, FrameworkConfig frameworkConfig) {
        this.connection = connection;
        this.frameworkConfig = frameworkConfig;
    }

    static class DatabaseManagerHolder {
        static final DatabaseManager INSTANCE;

        static {
            try {
                CalciteConnection connection = initializeCalciteConnection();
                FrameworkConfig frameworkConfig = getFrameworkConfig(connection);
                INSTANCE = new DatabaseManager(connection, frameworkConfig);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static FrameworkConfig getFrameworkConfig(CalciteConnection connection) {
            return Frameworks.newConfigBuilder()
                    .defaultSchema(connection.getRootSchema())
                    .parserConfig(SqlParser.config().withCaseSensitive(false))
                    .build();
        }

        static CalciteConnection initializeCalciteConnection() throws ClassNotFoundException, SQLException {
            // Initialize the JDBC driver
            Class.forName(Driver.class.getName());
            // Create a JDBC connection
            // DO NOT USE TRY-WITH-RESOURCES, IT WILL AUTO-CLOSE THE CONNECTION AND MAKE
            // QUERYING IMPOSSIBLE
            Connection connection = DriverManager.getConnection(Driver.CONNECT_STRING_PREFIX);
            return connection.unwrap(CalciteConnection.class);
        }
    }

    public static DatabaseManager getInstance() {
        return DatabaseManagerHolder.INSTANCE;
    }

    public SchemaPlus getRootSchema() {
        return connection.getRootSchema();
    }

    Supplier<RelBuilder> relBuilderSupplier = Suppliers.memoize(() -> {
        return RelBuilder.create(DatabaseManagerHolder.INSTANCE.frameworkConfig);
    });

    public RelBuilder getRelBuilder() {
        return relBuilderSupplier.get();
    }

    public RelRunner getRelRunner() throws SQLException {
        return this.connection.unwrap(RelRunner.class);
    }

    public String prettyPrintDatabases() {
        StringBuilder sb = new StringBuilder();
        for (Database database : getDatabases()) {
            sb.append(database.prettyPrint());
            sb.append("\n");
        }
        return sb.toString();
    }

    public List<Database> getDatabases() {
        SchemaPlus rootSchema = getRootSchema();
        return rootSchema.getSubSchemaNames().stream()
                .map(rootSchema::getSubSchema)
                .map(Database::fromCalciteSchema)
                .toList();
    }

    public ResultSet executeRelNode(RelNode relNode) throws SQLException {
        RelRunner runner = connection.unwrap(RelRunner.class);
        try (PreparedStatement statement = runner.prepareStatement(relNode)) {
            return statement.executeQuery();
        }
    }

    public ResultSet executeSql(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return statement.executeQuery(sql);
        }
    }

    static AbstractSchema emptyAbstractSchema() {
        return new AbstractSchema() {
            @Override
            protected Map<String, Schema> getSubSchemaMap() {
                return new HashMap<>();
            }
        };
    }

    public SchemaPlus addDatabase(String databaseName, DataSource ds) throws SQLException {
        List<String> schemas = getSubSchemas(ds.getConnection());
        List<String> systemSchemas = List.of("information_schema", "sys", "pg_catalog");

        SchemaPlus rootSchema = getRootSchema();
        if (schemas.isEmpty()) {
            rootSchema.add(databaseName, JdbcSchema.create(rootSchema, databaseName, ds, null, null));
        } else {
            SchemaPlus databaseSchema = rootSchema.add(databaseName, emptyAbstractSchema());
            for (String schemaName : schemas) {
                if (systemSchemas.contains(schemaName.toLowerCase(Locale.US))) {
                    continue;
                }
                databaseSchema.add(schemaName, JdbcSchema.create(databaseSchema, schemaName, ds, null, schemaName));
            }
        }

        return rootSchema;
    }

    private static List<String> getSubSchemas(Connection connection) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getSchemas()) {
            List<String> schemas = new ArrayList<>();
            while (resultSet.next()) {
                schemas.add(resultSet.getString("TABLE_SCHEM"));
            }
            return schemas;
        }
    }

    public ResultSet executeQuery(RelNode relRoot) throws SQLException {
        relRoot.getCluster().getPlanner().setRoot(
                relRoot.getCluster().getPlanner().changeTraits(
                        relRoot,
                        relRoot.getCluster().traitSet().replace(EnumerableConvention.INSTANCE)));

        final RelNode bestExp = relRoot.getCluster().getPlanner().findBestExp();
        final RelRunner runner = connection.unwrap(RelRunner.class);

        try (PreparedStatement statement = runner.prepareStatement(bestExp)) {
            return statement.executeQuery();
        }
    }

    public ResultSet executeQuery(String sql)
            throws SQLException, ValidationException, SqlParseException, RelConversionException {
        return executeQuery(CalciteUtils.parseSql(sql, frameworkConfig));
    }
}
