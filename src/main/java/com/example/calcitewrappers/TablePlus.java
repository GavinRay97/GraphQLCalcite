package com.example.calcitewrappers;

import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNode;
import org.checkerframework.checker.nullness.qual.Nullable;

public record TablePlus(String name, SchemaPlus schema, Table table) implements Table {
    @Override
    public RelDataType getRowType(RelDataTypeFactory typeFactory) {
        return table.getRowType(typeFactory);
    }

    @Override
    public Statistic getStatistic() {
        return table.getStatistic();
    }

    @Override
    public org.apache.calcite.schema.Schema.TableType getJdbcTableType() {
        return table.getJdbcTableType();
    }

    @Override
    public boolean isRolledUp(String column) {
        return false;
    }

    @Override
    public boolean rolledUpColumnValidInsideAgg(String column, SqlCall call, @Nullable SqlNode parent, @Nullable CalciteConnectionConfig config) {
        return false;
    }
}
