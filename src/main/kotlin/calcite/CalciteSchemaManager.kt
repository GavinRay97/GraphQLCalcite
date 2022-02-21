package calcite

import JAVA_TYPE_FACTORY_IMPL
import entity.FullyQualifiedTableName
import org.apache.calcite.adapter.enumerable.EnumerableConvention
import org.apache.calcite.adapter.jdbc.JdbcSchema
import org.apache.calcite.avatica.util.Casing
import org.apache.calcite.config.CalciteConnectionProperty
import org.apache.calcite.jdbc.CalciteConnection
import org.apache.calcite.jdbc.Driver
import org.apache.calcite.rel.RelNode
import org.apache.calcite.schema.SchemaPlus
import org.apache.calcite.schema.impl.AbstractSchema
import org.apache.calcite.sql.parser.SqlParser
import org.apache.calcite.tools.FrameworkConfig
import org.apache.calcite.tools.Frameworks
import org.apache.calcite.tools.RelBuilder
import org.apache.calcite.tools.RelRunner
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.Properties
import javax.sql.DataSource


// calcite.CalciteSchemaManager holds the master schema that contains all datasource
// schemas and their sub-schemas
//
// It also holds metadata about the datasources and their schemas, like a
// foreign key map for each datasource
object CalciteSchemaManager {
    private val connection: CalciteConnection = initCalciteConnection()

    val rootSchema: SchemaPlus
        get() = connection.rootSchema

    // Need to set case-sensitive to false, or else it tries to look up capitalized table names and fails
    // IE: "EMPS" instead of "emps"
    val frameworkConfig: FrameworkConfig = Frameworks.newConfigBuilder()
        .defaultSchema(connection.rootSchema)
        .parserConfig(SqlParser.config().withCaseSensitive(false))
        .build()
    val relBuilder: RelBuilder = RelBuilder.create(frameworkConfig)

    private fun initCalciteConnection(): CalciteConnection {
        // Initialize the JDBC driver
        Class.forName(Driver::class.java.name)
        DriverManager.registerDriver(Driver())
        return DriverManager
            .getConnection(
                "jdbc:calcite:",
                Properties().apply {
                    setProperty(CalciteConnectionProperty.FUN.camelName(), "mysql,postgresql")
                    setProperty(CalciteConnectionProperty.CASE_SENSITIVE.camelName(), "false")
                    setProperty(CalciteConnectionProperty.QUOTED_CASING.camelName(), Casing.UNCHANGED.name)
                    setProperty(CalciteConnectionProperty.UNQUOTED_CASING.camelName(), Casing.UNCHANGED.name)
                }
            )
            .unwrap(CalciteConnection::class.java)
    }

    fun addDatabase(databaseName: String, ds: DataSource): SchemaPlus {
        assert(rootSchema.getSubSchema(databaseName) == null) { "Database $databaseName already exists" }

        fun emptyAbstractSchema(): AbstractSchema {
            return object : AbstractSchema() {
                override fun getSubSchemaMap(): Map<String, org.apache.calcite.schema.Schema> {
                    return HashMap()
                }
            }
        }

        fun getSubSchemas(connection: Connection): List<String> {
            connection.metaData.getSchemas().use { resultSet ->
                val schemas: MutableList<String> = mutableListOf()
                while (resultSet.next()) {
                    schemas.add(resultSet.getString("TABLE_SCHEM"))
                }
                return schemas
            }
        }

        val schemas: List<String> = getSubSchemas(ds.connection)
        val systemSchemas = listOf("information_schema", "sys", "pg_catalog", "metadata")

        // WARNING:
        //
        // If you use an invalid identifier for the second param of "JdbcSchema.create()"
        // then you will receive the following error:
        //
        // "Cannot invoke "calcite.schema.Schema.getTable(String)" because the
        // return value of "calcite.schema.SchemaPlus.getSubSchema(String)" is null"
        if (schemas.isEmpty()) {
            rootSchema.add(databaseName, JdbcSchema.create(rootSchema, databaseName, ds, null, null))
        } else {
            val databaseSchema: SchemaPlus = rootSchema.add(databaseName, emptyAbstractSchema())
            for (schemaName in schemas.filterNot { systemSchemas.contains(it.lowercase()) }) {
                databaseSchema.add(schemaName, JdbcSchema.create(databaseSchema, schemaName, ds, null, schemaName))
            }
        }

        return rootSchema
    }

    fun executeQuery(relRoot: RelNode): ResultSet {
        relRoot.cluster.planner.setRoot(
            relRoot.cluster.planner.changeTraits(
                relRoot,
                relRoot.cluster.traitSet().replace(EnumerableConvention.INSTANCE)
            )
        )
        val bestExp = relRoot.cluster.planner.findBestExp()
        val relRunner = connection.unwrap(RelRunner::class.java)

        return relRunner.prepareStatement(bestExp).executeQuery()
    }

    fun executeQuery(sql: String): ResultSet {
        return executeQuery(CalciteUtils.parse(sql, frameworkConfig))
    }

    fun executeUpdate(relRoot: RelNode): Int {
        relRoot.cluster.planner.setRoot(
            relRoot.cluster.planner.changeTraits(
                relRoot,
                relRoot.cluster.traitSet().replace(EnumerableConvention.INSTANCE)
            )
        )
        val bestExp = relRoot.cluster.planner.findBestExp()
        val relRunner = connection.unwrap(RelRunner::class.java)

        return relRunner.prepareStatement(bestExp).executeUpdate()
    }

    fun executeUpdate(sql: String): Int {
        return executeUpdate(CalciteUtils.parse(sql, frameworkConfig))
    }

    fun getOrdinalForColumn(
        rootSchema: SchemaPlus,
        fqtn: FullyQualifiedTableName,
        columnName: String
    ): Int {
        val db = rootSchema.getSubSchema(fqtn.database)
            ?: throw IllegalArgumentException("Database ${fqtn.database} does not exist")

        val schema = if (fqtn.schema == null)
            db.getSubSchema(fqtn.database)
        else
            db.getSubSchema(fqtn.schema)!!.getSubSchema(fqtn.schema)

        if (schema == null)
            throw RuntimeException("Schema ${fqtn.schema} not found")

        val table = schema.getTable(fqtn.table)
            ?: throw RuntimeException("Table ${fqtn.table} does not exist")

        val column = table.getRowType(JAVA_TYPE_FACTORY_IMPL)
            .fieldList
            .firstOrNull { it.name.equals(columnName) }
            ?: throw IllegalArgumentException("Column $columnName not found")

        return column.index
    }

    /**
     * Easier method for getting the ordinal of a column in a fully-qualified table name
     * This is used in generating RelReferentialConstraint objects for foreign keys (among other things)
     */
    fun getOrdinalForColumn(rootSchema: SchemaPlus, columnQualifiedPath: List<String>): Int {
        var schema = rootSchema

        // Last two elements are the table name and the column name
        // All preceding elements are the schema names
        for (i in 0 until columnQualifiedPath.size - 2) {
            schema = schema.getSubSchema(columnQualifiedPath[i])
                ?: throw IllegalArgumentException("Schema ${columnQualifiedPath[i]} not found")
        }

        val tableName = columnQualifiedPath[columnQualifiedPath.size - 2]
        val table = schema.getTable(tableName) ?: throw IllegalArgumentException("Table $tableName not found")

        val columnName = columnQualifiedPath[columnQualifiedPath.size - 1]
        val column = table.getRowType(JAVA_TYPE_FACTORY_IMPL)
            .fieldList
            .firstOrNull { it.name.equals(columnName) }
            ?: throw IllegalArgumentException("Column $columnName not found")

        return column.index
    }
}

