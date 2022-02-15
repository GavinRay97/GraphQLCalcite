import org.apache.calcite.rel.type.RelDataType
import org.apache.calcite.rel.type.RelDataTypeField
import org.apache.calcite.schema.SchemaPlus
import org.apache.calcite.sql.type.SqlTypeFamily

data class CalciteRootSchema(val rootSchema: SchemaPlus) {

    fun buildDatabaseSnapshotFromRootSchema(rootSchema: SchemaPlus): List<Database> {
        return rootSchema.subSchemaNames.map {
            val db = requireNotNull(rootSchema.getSubSchema(it))
            val databaseMinusTables = Database(
                underlyingSchema = db,
                schemas = db.subSchemaNames.map {
                    val databaseSchemaMinusTables = Schema(
                        underlyingSchema = db.getSubSchema(it)!!
                    )
                    databaseSchemaMinusTables.copy(
                        tables = databaseSchemaMinusTables.underlyingSchema.tableNames.map {
                            Table(
                                type = TableType.SCHEMA_TABLE,
                                schema = databaseSchemaMinusTables.underlyingSchema,
                                underlyingTable = databaseSchemaMinusTables.underlyingSchema.getTable(it)!!
                            )
                        }
                    )
                }
            )
            databaseMinusTables.copy(
                tables = db.tableNames.map {
                    Table(
                        type = TableType.DATABASE_TABLE,
                        underlyingTable = db.getTable(it)!!,
                        schema = databaseMinusTables.underlyingSchema,
                    )
                },
            )
        }
    }

    enum class TableType {
        // A table in a database that has one level of nesting (database -> table)
        // Example: MySQL
        DATABASE_TABLE,

        // A table in a database that has two levels of nesting (database -> schema -> table)
        // Example: Postgres
        SCHEMA_TABLE,
    }

    data class Database(
        val underlyingSchema: SchemaPlus,
        val tables: List<Table> = emptyList(),
        val schemas: List<Schema> = emptyList()
    ) {
        val name: String = underlyingSchema.name
    }

    data class Schema(
        val underlyingSchema: SchemaPlus,
        val tables: List<Table> = emptyList()
    ) {
        val name: String = underlyingSchema.name
    }

    data class Table(
        val type: TableType,
        val underlyingTable: org.apache.calcite.schema.Table,
        val schema: SchemaPlus
    ) {
        val name: String = underlyingTable.jdbcTableType.name
        val columns: List<Column> = underlyingTable.getRowType(JAVA_TYPE_FACTORY_IMPL).fieldList.map {
            Column(
                table = this,
                underlyingColumn = it
            )
        }

        val fullyQualifiedTableName: FullyQualifiedTableName = FullyQualifiedTableName(
            when (type) {
                TableType.DATABASE_TABLE -> schema.parentSchema!!.name
                TableType.SCHEMA_TABLE -> schema.parentSchema!!.parentSchema!!.name
            },
            schema.name,
            name
        )
    }

    // This class is necessary because the Calcite RelDataTypeField doesn't hold a reference to the table
    // So it's not possible to access it from just the RelDataTypeField context
    data class Column(
        val table: Table,
        val underlyingColumn: RelDataTypeField,
    ) {
        val name: String = underlyingColumn.name
        val type: RelDataType = underlyingColumn.type
        val family: SqlTypeFamily = underlyingColumn.type.family as SqlTypeFamily
        val isPrimaryKey: Boolean = PrimaryKeyManager.primaryKeys.find {
            it.fullyQualifiedTableName == table.fullyQualifiedTableName && it.columns.contains(name)
        } != null
    }
}
