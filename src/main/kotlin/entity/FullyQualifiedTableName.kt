package entity

import JAVA_TYPE_FACTORY_IMPL
import calcite.CalciteSchemaManager
import org.apache.calcite.rel.type.RelDataTypeField
import org.apache.calcite.schema.SchemaPlus
import org.apache.calcite.schema.Table

data class FullyQualifiedTableName(val database: String, val schema: String?, val table: String) {
    fun toFullyQualifiedString(): String {
        return toStringPathList().joinToString(".")
    }

    fun toStringPathList(): List<String> {
        val path = mutableListOf<String>()
        path.add(database)
        if (schema != null) {
            path.add(schema)
        }
        path.add(table)
        return path
    }

    fun getDatabase(): SchemaPlus {
        return CalciteSchemaManager.rootSchema.getSubSchema(database)
            ?: throw IllegalStateException("Database $database not found")
    }

    fun getSchema(): SchemaPlus {
        return getDatabase().getSubSchema(schema)
            ?: throw IllegalStateException("Schema $schema not found")
    }

    fun getTable(): Table {
        return if (schema != null) {
            getSchema().getTable(table)
                ?: throw IllegalStateException("Table $table not found")
        } else {
            getDatabase().getTable(table)
                ?: throw IllegalStateException("Table $table not found")
        }
    }

    fun getColumns(): List<RelDataTypeField> {
        return getTable().getRowType(JAVA_TYPE_FACTORY_IMPL).fieldList
    }

    companion object {
        fun fromQualifiedName(qualifiedName: List<String>): FullyQualifiedTableName {
            val database = qualifiedName[0]
            val schema = if (qualifiedName.size > 1) qualifiedName[1] else null
            val table = qualifiedName[qualifiedName.size - 1]
            return FullyQualifiedTableName(database, schema, table)
        }
    }
}

