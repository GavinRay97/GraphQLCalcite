package com.example.calcitewrappers;

import org.apache.calcite.schema.SchemaPlus;

import java.util.List;

public record Database(SchemaPlus underlyingSchema, List<Schema> schemas, List<TableRecord> tables) {
    public String name() {
        return underlyingSchema.getName();
    }

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        sb.append("Database: ").append(name()).append("\n");
        for (TableRecord table : tables) {
            sb.append(table.prettyPrint()).append("\n");
        }
        for (Schema schema : schemas) {
            sb.append(schema.prettyPrint()).append("\n");
        }
        return sb.toString();
    }

    public static Database fromCalciteSchema(SchemaPlus schema) {
        List<SchemaPlus> subSchemas = schema.getSubSchemaNames().stream().map(schema::getSubSchema).toList();
        if (subSchemas.isEmpty()) {
            List<TableRecord> tables = schema.getTableNames().stream()
                    .map(tableName -> TableRecord.fromCalciteTable(tableName, schema, schema.getTable(tableName)))
                    .toList();
            return new Database(schema, List.of(), tables);
        }
        List<Schema> schemas = subSchemas.stream().map(Schema::fromCalciteSchema).toList();
        return new Database(schema, schemas, List.of());
    }

}
