package com.example;

import org.apache.calcite.schema.Schema;

public class CalciteSchemaPrinter {
    // Recursively prints the tables and sub-schemas  of a Calcite schema
    static String printSchema(Schema schema) {
        return printSchemaImpl(schema, 0);
    }

    // Recursively prints the tables and sub-schemas  of a Calcite schema
    private static String printSchemaImpl(Schema schema, int level) {
        StringBuilder sb = new StringBuilder();
        StringBuilder indent = new StringBuilder();
        
        indent.append("  ".repeat(Math.max(0, level)));

        for (String tableName : schema.getTableNames()) {
            sb.append(indent).append("  [T] ").append(tableName).append("\n");
        }

        for (String subSchemaName : schema.getSubSchemaNames()) {
            if (subSchemaName.equals("information_schema")) {
                continue;
            }
            if (subSchemaName.equals("pg_catalog")) {
                continue;
            }
            sb.append(indent).append("[S] ").append(subSchemaName).append("\n");
            sb.append(printSchemaImpl(schema.getSubSchema(subSchemaName), level + 1));
        }

        return sb.toString();
    }
}
