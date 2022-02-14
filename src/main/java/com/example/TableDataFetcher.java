package com.example;

import com.example.calcitewrappers.DatabaseManager;
import com.example.calcitewrappers.ForeignKey;
import com.example.calcitewrappers.ForeignKeyManager;
import com.example.calcitewrappers.FullyQualifiedTableName;
import graphql.schema.*;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.tools.RelBuilder;
import org.apache.calcite.tools.RelRunner;
import org.apache.calcite.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;

import static com.example.Utils.reverse;
import static java.util.Objects.requireNonNull;

public final class TableDataFetcher implements DataFetcher {

    private final FullyQualifiedTableName fullyQualifiedTableName;
    private final SqlToGraphQLConfiguration sqlToGraphQLConfiguration;
    private final DatabaseManager databaseManager;

    private final String databaseName;
    private final String schemaName;
    private final String tableName;

    private final Set<ForeignKey> foreignKeys;

    public TableDataFetcher(
            FullyQualifiedTableName fullyQualifiedTableName,
            SqlToGraphQLConfiguration sqlToGraphQLConfiguration,
            DatabaseManager databaseManager) {
        this.fullyQualifiedTableName = fullyQualifiedTableName;
        this.sqlToGraphQLConfiguration = sqlToGraphQLConfiguration;
        this.databaseManager = databaseManager;

        this.databaseName = fullyQualifiedTableName.database();
        this.schemaName = fullyQualifiedTableName.schema();
        this.tableName = fullyQualifiedTableName.table();

        this.foreignKeys = ForeignKeyManager.getForeignKeysForTable(fullyQualifiedTableName);
    }


    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        return handleField(environment.getArguments(), environment.getSelectionSet(), null);
    }

    private List<Map<String, Object>> handleField(
            Map<String, Object> arguments,
            DataFetchingFieldSelectionSet selectionSet,
            @Nullable RexNode joinFilterCondition
    ) throws SQLException {
        Map<String, Object> where = (Map<String, Object>) arguments.get("where");
        Integer limit = (Integer) arguments.get("limit");
        Integer offset = (Integer) arguments.get("offset");

        if (limit == null) {
            limit = 0;
        }
        if (offset == null) {
            offset = 0;
        }

        List<SelectedField> scalars = new ArrayList<>();
        List<SelectedField> joins = new ArrayList<>();

        for (SelectedField field : selectionSet.getImmediateFields()) {
            if (field.getType() instanceof GraphQLScalarType) {
                scalars.add(field);
            } else {
                joins.add(field);
            }
        }

        RelNode relNode = buildRelationalExpr(joinFilterCondition, where, offset, limit, scalars);
        List<Map<String, Object>> rows;
        RelRunner relRunner = this.databaseManager.getRelRunner();
        try (PreparedStatement statement = relRunner.prepareStatement(relNode)) {
            rows = SQLUtils.executeStatementReturningListOfObjects(statement);
        }

        for (SelectedField join : joins) {
            var foreignKey = foreignKeys.stream()
                    .filter(fk -> fk.sourceTable().table().equals(join.getName()) || fk.targetTable().table().equals(join.getName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not find foreign key for " + join.getName()));

            boolean tableIsTargetInJoin = foreignKey.targetTable() == fullyQualifiedTableName;
            FullyQualifiedTableName joinTable = tableIsTargetInJoin ? foreignKey.sourceTable() : foreignKey.targetTable();

            RelBuilder innerRelBuilder = databaseManager.getRelBuilder();
            innerRelBuilder = innerRelBuilder.scan(joinTable.database(), joinTable.schema(), joinTable.table());

            List<RexNode> joinConditions = new ArrayList<>();
            for (Pair<String, String> colPair : foreignKey.columns()) {
                List<RexLiteral> list = new ArrayList<>();
                if (tableIsTargetInJoin) {
                    for (Map<String, Object> row : rows) {
                        RexLiteral literal = innerRelBuilder.literal(row.get(colPair.right));
                        list.add(literal);
                    }
                    joinConditions.add(innerRelBuilder.in(innerRelBuilder.field(colPair.left), list));
                } else {
                    for (Map<String, Object> row : rows) {
                        RexLiteral literal = innerRelBuilder.literal(row.get(colPair.left));
                        list.add(literal);
                    }
                    joinConditions.add(innerRelBuilder.in(innerRelBuilder.field(colPair.right), list));
                }
            }

            List<Map<String, Object>> joinResults = handleField(
                    arguments,
                    join.getSelectionSet(),
                    innerRelBuilder.and(joinConditions)
            );

            if (tableIsTargetInJoin) {
                for (Map<String, Object> row : rows) {
                    row.put(join.getName(),
                            joinResults.stream()
                                    .filter(joinRow ->
                                            foreignKey.columns().stream().allMatch(colPair -> {
                                                return joinRow.get(colPair.left).equals(row.get(colPair.right));
                                            })
                                    )
                    );
                }
            } else {
                for (Map<String, Object> row : rows) {
                    row.put(join.getName(),
                            joinResults.stream()
                                    .filter(joinRow ->
                                            foreignKey.columns().stream().allMatch(colPair -> {
                                                return joinRow.get(colPair.right).equals(row.get(colPair.left));
                                            })
                                    )
                                    .findFirst()
                                    .orElse(null)
                    );
                }
            }
        }

        return rows;
    }

    private RelNode buildRelationalExpr(
            @Nullable RexNode joinFilterCondition,
            Map<String, Object> where,
            Integer offset,
            Integer limit,
            List<SelectedField> scalars
    ) {
        RelBuilder relBuilder = databaseManager.getRelBuilder();

        if (joinFilterCondition == null) {
            if (schemaName != null) {
                relBuilder = relBuilder.scan(databaseName, schemaName, tableName);
            } else {
                relBuilder = relBuilder.scan(databaseName, tableName);
            }
        }

        if (where != null) {
            Expression whereExpr = whereArgumentToExpression(where, scalars);
            if (whereExpr != null) {
                relBuilder = relBuilder.filter(whereExpr.toRexNode(relBuilder));
            }
        }

        if (joinFilterCondition != null) {
            relBuilder = relBuilder.filter(joinFilterCondition);
        }

        if (offset != 0 || limit != 0) {
            relBuilder = relBuilder.limit(offset, limit == 0 ? -1 : limit);
        }

        List<String> columns = scalars.stream().map(SelectedField::getName).toList();
        relBuilder = relBuilder.project(relBuilder.fields(columns));

        return relBuilder.build();
    }

    public Expression whereArgumentToExpression(Map<?, ?> whereArgument, List<SelectedField> fields) {
        // reduceRight
        Stream<Expression> expr = whereArgument.entrySet().stream().map((entry) -> {
            String key = (String) requireNonNull(entry.getKey());
            Object value = requireNonNull(entry.getValue());
            return switch (key) {
                case "_and" -> {
                    if (value instanceof List<?> list) {
                        yield reverse(list)
                                .map(e -> whereArgumentToExpression((Map) e, fields))
                                .reduce(Expression.AND::new)
                                .orElse(null);
                    }
                    throw new IllegalArgumentException("Expected list of maps");
                }
                case "_or" -> {
                    if (value instanceof List<?> list) {
                        yield reverse(list)
                                .map(e -> whereArgumentToExpression((Map) e, fields))
                                .reduce(Expression.OR::new)
                                .orElse(null);
                    }
                    throw new IllegalArgumentException("Expected list of maps");
                }
                case "_not" -> {
                    if (value instanceof Map m) {
                        yield new Expression.NOT(whereArgumentToExpression(m, fields));
                    }
                    throw new IllegalArgumentException("Expected map");
                }
                default -> {
                    if (value instanceof Map m) {
                        assert m.size() == 1 : "Expected single operator";

                        GraphQLInputTypeForScalar graphQLInputTypeForScalar = sqlToGraphQLConfiguration
                                .getGraphqlScalarTypeToGraphQLInputTypeMap()
                                .get(getScalarTypeOfField(key, fields));

                        if (graphQLInputTypeForScalar != null) {
                            String operator = (String) m.keySet().iterator().next();
                            Object operand = m.values().iterator().next();
                            QueryFilterExpression queryFilterExpression = new QueryFilterExpression(operator, key, operand);
                            yield graphQLInputTypeForScalar.handleQueryFilterExpression(queryFilterExpression);
                        }

                        throw new IllegalArgumentException("Expected scalar type");
                    }
                    throw new IllegalArgumentException("Expected map");
                }
            };
        });

        return reverse(expr.toList()).reduce(Expression.AND::new).orElse(null);
    }


    @Nullable GraphQLScalarType getScalarTypeOfField(String name, List<SelectedField> fields) {
        SelectedField field = fields.stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null);
        if (field == null) {
            return null;
        }

        GraphQLType fieldType = field.getType();
        if (fieldType instanceof GraphQLScalarType scalarType) {
            return scalarType;
        }

        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TableDataFetcher) obj;
        return Objects.equals(this.fullyQualifiedTableName, that.fullyQualifiedTableName) &&
                Objects.equals(this.sqlToGraphQLConfiguration, that.sqlToGraphQLConfiguration) &&
                Objects.equals(this.databaseManager, that.databaseManager);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullyQualifiedTableName, sqlToGraphQLConfiguration, databaseManager);
    }

    @Override
    public String toString() {
        return "TableDataFetcher[" +
                "fullyQualifiedTableName=" + fullyQualifiedTableName + ", " +
                "sqlToGraphQLConfiguration=" + sqlToGraphQLConfiguration + ", " +
                "calciteSchemaManager=" + databaseManager + ']';
    }


}
