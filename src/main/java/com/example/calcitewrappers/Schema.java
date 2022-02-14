package com.example.calcitewrappers;

import org.apache.calcite.schema.SchemaPlus;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record Schema(SchemaPlus underlyingSchema, List<TableRecord> tables) {
    public String name() {
        return underlyingSchema.getName();
    }

    public String databaseName() {
        return requireNonNull(underlyingSchema.getParentSchema()).getName();
    }

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        sb.append("Schema: ").append(name()).append("\n");
        for (TableRecord table : tables) {
            sb.append("  ").append(table.prettyPrint()).append("\n");
        }
        return sb.toString();
    }

    public static Schema fromCalciteSchema(SchemaPlus schema) {
        List<TableRecord> tables = schema.getTableNames().stream()
                .map(tableName -> TableRecord.fromCalciteTable(tableName, schema, schema.getTable(tableName)))
                .toList();
        return new Schema(schema, tables);
    }
}
