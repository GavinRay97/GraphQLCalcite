package com.example.calcitewrappers;

import org.apache.calcite.util.Pair;

import java.util.List;

public record ForeignKey(FullyQualifiedTableName sourceTable, FullyQualifiedTableName targetTable,
                         List<Pair<String, String>> columns) {

}
