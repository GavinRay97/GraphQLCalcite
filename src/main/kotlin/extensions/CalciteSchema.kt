package extensions

import JAVA_TYPE_FACTORY_IMPL
import com.google.common.collect.ImmutableList
import org.apache.calcite.plan.RelOptPlanner
import org.apache.calcite.plan.RelOptSchema
import org.apache.calcite.plan.RelOptTable
import org.apache.calcite.prepare.RelOptTableImpl
import org.apache.calcite.rel.type.RelDataTypeFactory
import org.apache.calcite.schema.Schema
import org.apache.calcite.schema.Table


fun Schema.print(indentSpaces: Int = 0): String {
    val sb = StringBuilder()

    if (this.tableNames.size > 0) {
        sb.append(" ".repeat(indentSpaces) + "Tables:\n")
        for (table in this.tableNames) {
            sb.append(" ".repeat(indentSpaces) + "  - " + table + "\n")
        }
    }

    for (subSchemaName in this.subSchemaNames) {
        val subSchema = this.getSubSchema(subSchemaName)
        if (subSchema != null) {
            sb.append(" ".repeat(indentSpaces + 4) + "Schema: " + subSchemaName + "\n")
            sb.append(subSchema.print(indentSpaces + 4) + "\n")
        }
    }

    return sb.toString()
}

fun Schema.toRelOptSchema(): RelOptSchema {
    val schema = this
    return object : RelOptSchema {
        override fun getTableForMember(names: MutableList<String>): RelOptTable? {
            val fullyQualifiedTableName = names.joinToString(".")
            val table = schema.getTable(fullyQualifiedTableName) ?: return null
            return RelOptTableImpl.create(
                this,
                table.getRowType(JAVA_TYPE_FACTORY_IMPL),
                table,
                ImmutableList.copyOf(names)
            )
        }

        override fun getTypeFactory(): RelDataTypeFactory {
            return JAVA_TYPE_FACTORY_IMPL
        }

        override fun registerRules(planner: RelOptPlanner) {
            return
        }
    }
}

fun Schema.groupSchemasAndTables(): Map<String, List<Table>> {
    return this.subSchemaNames.associateWith { schemaName ->
        val schema = requireNotNull(this.getSubSchema(schemaName))
        schema.tableNames.map { schema.getTable(it)!! }
    }
}

