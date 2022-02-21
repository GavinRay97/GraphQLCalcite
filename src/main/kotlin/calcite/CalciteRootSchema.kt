package calcite

import JAVA_TYPE_FACTORY_IMPL
import entity.FullyQualifiedTableName
import org.apache.calcite.rel.type.RelDataTypeField
import org.apache.calcite.schema.SchemaPlus

data class CalciteRootSchema(val rootSchema: SchemaPlus) {
    fun databases(): List<Database> = rootSchema.subSchemaNames.map {
        Database(rootSchema.getSubSchema(it)!!)
    }

    data class Database(val underlying: SchemaPlus) {
        fun schemas(): List<Schema> = underlying.subSchemaNames.map {
            Schema(underlying.getSubSchema(it)!!)
        }

        fun tables(): List<Table> = underlying.tableNames.map { tableName ->
            Table(tableName, this.underlying, null, underlying.getTable(tableName)!!)
        }
    }

    data class Schema(val underlying: SchemaPlus) {
        fun tables(): List<Table> = underlying.tableNames.map { tableName ->
            Table(tableName, this.underlying.parentSchema!!, this.underlying, underlying.getTable(tableName)!!)
        }
    }

    data class Table(
        val name: String,
        val database: SchemaPlus,
        val schema: SchemaPlus?,
        val underlying: org.apache.calcite.schema.Table
    ) {
        fun columns(): List<Column> = underlying.getRowType(JAVA_TYPE_FACTORY_IMPL).fieldList.map {
            Column(this, it)
        }

        val quotedTableName: String by lazy {
            if (schema != null) {
                "\"${database.name}\".\"${schema.name}\".\"$name\""
            } else {
                "\"${database.name}\".\"$name\""
            }
        }

        val fullyQualifiedTableName by lazy {
            FullyQualifiedTableName(database.name, schema?.name, name)
        }

        val foreignKeys by lazy {
            ForeignKeyManager.getForeignKeysForTable(fullyQualifiedTableName)
        }

        val primaryKey by lazy {
            PrimaryKeyManager.primaryKeys.values.find {
                it.table.name == this.name &&
                        it.table.database.name == this.database.name &&
                        it.table.schema?.name == this.schema?.name
            }
        }
    }

    data class Column(val table: Table, val underlying: RelDataTypeField) {
        val name: String by lazy {
            underlying.name
        }
    }
}
