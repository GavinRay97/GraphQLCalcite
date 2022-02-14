package com.example.calcitewrappers;

import org.apache.calcite.rel.type.RelDataTypeField;

public record Column(RelDataTypeField underlyingColumn) {
    public String name() {
        return underlyingColumn.getName();
    }

    public static Column fromCalciteColumn(RelDataTypeField field) {
        return new Column(field);
    }

    public String prettyPrint() {
        return "Column: " +
                name() +
                " type: " +
                underlyingColumn.getType().getSqlTypeName() +
                "\n";
    }
}
