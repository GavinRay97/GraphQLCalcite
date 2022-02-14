package com.example.calcitewrappers;

import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record TableRecord(TablePlus underlyingTable, List<Column> columns) {
    public String name() {
        return underlyingTable.name();
    }

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        sb.append("Table: ").append(name()).append("\n");
        for (Column column : columns) {
            sb.append("  ").append(column.prettyPrint()).append("\n");
        }
        return sb.toString();
    }

    public static TableRecord fromCalciteTable(String name, SchemaPlus schema, Table table) {
        TablePlus tablePlus = new TablePlus(name, schema, table);
        List<Column> columns = table.getRowType(new JavaTypeFactoryImpl()).getFieldList().stream()
                .map(Column::fromCalciteColumn)
                .toList();
        return new TableRecord(tablePlus, columns);
    }

    public List<SchemaPlus> getSchemaHierarchy() {
        List<SchemaPlus> schemaPath = new ArrayList<>();
        SchemaPlus nextSchema = underlyingTable.schema();

        while (nextSchema.getParentSchema() != null) {
            schemaPath.add(0, nextSchema);
            nextSchema = nextSchema.getParentSchema();
        }
        schemaPath.add(0, nextSchema);

        return schemaPath;
    }

    public SchemaPlus getDatabase() {
        List<SchemaPlus> schemaHieararchy = getSchemaHierarchy();
        return schemaHieararchy.get(1);
    }

    public @Nullable SchemaPlus getSchema() {
        List<SchemaPlus> schemaHieararchy = getSchemaHierarchy();
        try {
            return schemaHieararchy.get(2);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
}
