package com.example.calcitewrappers;

import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;

import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class ForeignKeyManager {
    public static final MutableNetwork<FullyQualifiedTableName, ForeignKey> foreignKeyGraph =
            NetworkBuilder.directed().allowsParallelEdges(false).allowsSelfLoops(false).build();

    public static void addForeignKey(ForeignKey foreignKey) {
        foreignKeyGraph.addEdge(
                foreignKey.sourceTable(),
                foreignKey.targetTable(),
                foreignKey
        );
    }

    public static void removeForeignKey(ForeignKey foreignKey) {
        foreignKeyGraph.removeEdge(foreignKey);
    }

    public static boolean isForeignKey(FullyQualifiedTableName sourceTable, FullyQualifiedTableName targetTable) {
        return foreignKeyGraph.hasEdgeConnecting(sourceTable, targetTable);
    }

    public static Set<ForeignKey> getForeignKeysForTable(FullyQualifiedTableName table) {
        try {
            return foreignKeyGraph.incidentEdges(table);
        } catch (IllegalArgumentException e) {
            return Set.of();
        }
    }
}
