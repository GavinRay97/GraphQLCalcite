package com.example.calcitewrappers;

import org.jetbrains.annotations.Nullable;

public record FullyQualifiedTableName(String database, @Nullable String schema, String table) {

}
