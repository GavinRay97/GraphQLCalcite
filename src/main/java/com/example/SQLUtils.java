package com.example;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public final class SQLUtils {

    private SQLUtils() {
    }

    @NotNull
    public static List<Map<String, Object>> executeStatementReturningListOfObjects(PreparedStatement statement)
            throws SQLException {

        try (ResultSet resultSet = statement.executeQuery()) {
            List<Map<String, Object>> queryResult = new ArrayList<>();

            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    row.put(resultSet.getMetaData().getColumnLabel(i), resultSet.getObject(i));
                }
                queryResult.add(row);
            }

            return queryResult;
        }
    }

}
