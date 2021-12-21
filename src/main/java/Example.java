//import graphql.ExecutionInput;
//import graphql.ParseAndValidate;
//import graphql.analysis.*;
//import graphql.language.*;
//import graphql.schema.*;
//import graphql.schema.idl.SchemaPrinter;
//import graphql.util.TraversalControl;
//import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
//import org.apache.calcite.plan.RelOptCluster;
//import org.apache.calcite.plan.volcano.VolcanoPlanner;
//import org.apache.calcite.rel.RelNode;
//import org.apache.calcite.rel.core.RelFactories;
//import org.apache.calcite.rel.type.RelDataType;
//import org.apache.calcite.rel.type.RelDataTypeFactory;
//import org.apache.calcite.rex.RexBuilder;
//import org.apache.calcite.schema.Schema;
//import org.apache.calcite.schema.SchemaPlus;
//import org.apache.calcite.schema.impl.AbstractTable;
//import org.apache.calcite.sql.type.SqlTypeName;
//import org.apache.calcite.tools.Frameworks;
//import org.apache.calcite.tools.RelBuilder;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//
//import static graphql.Scalars.*;
//
//
//class BaseGraphQLTypes {
//
//    record ComparisonExpressionInputType(GraphQLScalarType type, String name, String description) {
//
//        public GraphQLInputObjectType build() {
//            return GraphQLInputObjectType.newInputObject()
//                    .name(name)
//                    .description(description)
//                    .field(GraphQLInputObjectField.newInputObjectField()
//                            .name("_is_null")
//                            .type(GraphQLBoolean))
//                    .field(GraphQLInputObjectField.newInputObjectField()
//                            .name("_eq")
//                            .type(type))
//                    .field(GraphQLInputObjectField.newInputObjectField()
//                            .name("_gt")
//                            .type(type))
//                    .field(GraphQLInputObjectField.newInputObjectField()
//                            .name("_gte")
//                            .type(type))
//                    .field(GraphQLInputObjectField.newInputObjectField()
//                            .name("_in")
//                            .type(new GraphQLList(new GraphQLNonNull(type))))
//                    .field(GraphQLInputObjectField.newInputObjectField()
//                            .name("_lt")
//                            .type(type))
//                    .field(GraphQLInputObjectField.newInputObjectField()
//                            .name("_lte")
//                            .type(type))
//                    .field(GraphQLInputObjectField.newInputObjectField()
//                            .name("_neq")
//                            .type(type))
//                    .field(GraphQLInputObjectField.newInputObjectField()
//                            .name("_nin")
//                            .type(new GraphQLList(new GraphQLNonNull(type))))
//                    .build();
//        }
//    }
//
//    public static final GraphQLInputObjectType IntComparisonExpressionType = new ComparisonExpressionInputType(
//            GraphQLInt,
//            "Int_comparison_exp",
//            "Boolean expression to compare columns of type \"Int\". All fields are combined with logical 'AND'.").build();
//
//    public static final GraphQLInputObjectType FloatComparisonExpressionType = new ComparisonExpressionInputType(
//            GraphQLFloat,
//            "Float_comparison_exp",
//            "Boolean expression to compare columns of type \"Float\". All fields are combined with logical 'AND'.").build();
//
//    public static final GraphQLInputObjectType BooleanComparisonExpressionType = new ComparisonExpressionInputType(
//            GraphQLBoolean,
//            "Boolean_comparison_exp",
//            "Boolean expression to compare columns of type \"Boolean\". All fields are combined with logical 'AND'.").build();
//    public static final GraphQLInputObjectType StringComparisonExpressionType = new ComparisonExpressionInputType(
//            GraphQLString,
//            "String_comparison_exp",
//            "Boolean expression to compare columns of type \"String\". All fields are combined with logical 'AND'.").build().transform(t -> {
//        t.field(GraphQLInputObjectField.newInputObjectField()
//                .name("_ilike")
//                .description("does the column match the given case-insensitive pattern")
//                .type(GraphQLString));
//
//        t.field(GraphQLInputObjectField.newInputObjectField()
//                .name("_iregex")
//                .description("does the column match the given POSIX regular expression, case insensitive")
//                .type(GraphQLString));
//
//        t.field(GraphQLInputObjectField.newInputObjectField()
//                .name("_like")
//                .description("does the column match the given pattern")
//                .type(GraphQLString));
//
//        t.field(GraphQLInputObjectField.newInputObjectField()
//                .name("_nilike")
//                .description("does the column NOT match the given case-insensitive pattern")
//                .type(GraphQLString));
//
//        t.field(GraphQLInputObjectField.newInputObjectField()
//                .name("_niregex")
//                .description("does the column NOT match the given POSIX regular expression, case insensitive")
//                .type(GraphQLString));
//
//        t.field(GraphQLInputObjectField.newInputObjectField()
//                .name("_nlike")
//                .description("does the column NOT match the given pattern")
//                .type(GraphQLString));
//
//        t.field(GraphQLInputObjectField.newInputObjectField()
//                .name("_nregex")
//                .description("does the column NOT match the given POSIX regular expression, case sensitive")
//                .type(GraphQLString));
//
//        t.field(GraphQLInputObjectField.newInputObjectField()
//                .name("_nsimilar")
//                .description("does the column NOT match the given SQL regular expression")
//                .type(GraphQLString));
//
//        t.field(GraphQLInputObjectField.newInputObjectField()
//                .name("_regex")
//                .description("does the column match the given POSIX regular expression, case sensitive")
//                .type(GraphQLString));
//
//        t.field(GraphQLInputObjectField.newInputObjectField()
//                .name("_similar")
//                .description("does the column match the given SQL regular expression")
//                .type(GraphQLString));
//    });
//}
//
//class CalciteAdapterGraphQLSchemaGenerator {
//
//    public static GraphQLType relDataTypeToGraphQLType(RelDataType type) {
//        return switch (type.getSqlTypeName()) {
//            case BOOLEAN -> GraphQLBoolean;
//
//            case TINYINT, SMALLINT, INTEGER, BIGINT -> GraphQLInt;
//            case DECIMAL, FLOAT, REAL, DOUBLE -> GraphQLFloat;
//
//            case DATE -> GraphQLString;
//            case TIME -> GraphQLString;
//            case TIME_WITH_LOCAL_TIME_ZONE -> GraphQLString;
//            case TIMESTAMP -> GraphQLString;
//            case TIMESTAMP_WITH_LOCAL_TIME_ZONE -> GraphQLString;
//
//            case INTERVAL_YEAR -> GraphQLInt;
//            case INTERVAL_YEAR_MONTH -> GraphQLInt;
//            case INTERVAL_MONTH -> GraphQLInt;
//            case INTERVAL_DAY -> GraphQLInt;
//            case INTERVAL_DAY_HOUR -> GraphQLInt;
//            case INTERVAL_DAY_MINUTE -> GraphQLInt;
//            case INTERVAL_DAY_SECOND -> GraphQLInt;
//            case INTERVAL_HOUR -> GraphQLInt;
//            case INTERVAL_HOUR_MINUTE -> GraphQLInt;
//            case INTERVAL_HOUR_SECOND -> GraphQLInt;
//            case INTERVAL_MINUTE -> GraphQLInt;
//            case INTERVAL_MINUTE_SECOND -> GraphQLInt;
//            case INTERVAL_SECOND -> GraphQLInt;
//
//            case CHAR -> GraphQLString;
//            case VARCHAR -> GraphQLString;
//            case BINARY -> GraphQLString;
//            case VARBINARY -> GraphQLString;
//
//            case NULL -> GraphQLString;
//            case ANY -> GraphQLString;
//            case SYMBOL -> GraphQLString;
//            case MULTISET -> GraphQLString;
//            case ARRAY -> GraphQLString;
//            case MAP -> GraphQLString;
//            case DISTINCT -> GraphQLString;
//            case STRUCTURED -> GraphQLString;
//            case ROW -> GraphQLString;
//            case OTHER -> GraphQLString;
//            case CURSOR -> GraphQLString;
//            case COLUMN_LIST -> GraphQLString;
//            case DYNAMIC_STAR -> GraphQLString;
//            case GEOMETRY -> GraphQLString;
//            case SARG -> GraphQLString;
//        };
//    }
//
//    public static GraphQLType relDataTypeToGraphQLComparisonExprType(RelDataType type) {
//        return switch (type.getSqlTypeName()) {
//            case BOOLEAN -> BaseGraphQLTypes.BooleanComparisonExpressionType;
//
//            case TINYINT, SMALLINT, INTEGER, BIGINT -> BaseGraphQLTypes.IntComparisonExpressionType;
//            case DECIMAL, FLOAT, REAL, DOUBLE -> BaseGraphQLTypes.FloatComparisonExpressionType;
//
//            case DATE -> BaseGraphQLTypes.StringComparisonExpressionType;
//            case TIME -> BaseGraphQLTypes.StringComparisonExpressionType;
//            case TIME_WITH_LOCAL_TIME_ZONE -> BaseGraphQLTypes.StringComparisonExpressionType;
//            case TIMESTAMP -> BaseGraphQLTypes.StringComparisonExpressionType;
//            case TIMESTAMP_WITH_LOCAL_TIME_ZONE -> BaseGraphQLTypes.StringComparisonExpressionType;
//
//            // Not sure how to deal with these yet
//            case INTERVAL_YEAR -> GraphQLInt;
//            case INTERVAL_YEAR_MONTH -> GraphQLInt;
//            case INTERVAL_MONTH -> GraphQLInt;
//            case INTERVAL_DAY -> GraphQLInt;
//            case INTERVAL_DAY_HOUR -> GraphQLInt;
//            case INTERVAL_DAY_MINUTE -> GraphQLInt;
//            case INTERVAL_DAY_SECOND -> GraphQLInt;
//            case INTERVAL_HOUR -> GraphQLInt;
//            case INTERVAL_HOUR_MINUTE -> GraphQLInt;
//            case INTERVAL_HOUR_SECOND -> GraphQLInt;
//            case INTERVAL_MINUTE -> GraphQLInt;
//            case INTERVAL_MINUTE_SECOND -> GraphQLInt;
//            case INTERVAL_SECOND -> GraphQLInt;
//
//            case CHAR -> BaseGraphQLTypes.StringComparisonExpressionType;
//            case VARCHAR -> BaseGraphQLTypes.StringComparisonExpressionType;
//            case BINARY -> BaseGraphQLTypes.StringComparisonExpressionType;
//            case VARBINARY -> BaseGraphQLTypes.StringComparisonExpressionType;
//
//            // Not sure how to deal with these yet
//            case NULL -> GraphQLString;
//            case ANY -> GraphQLString;
//            case SYMBOL -> GraphQLString;
//            case MULTISET -> GraphQLString;
//            case ARRAY -> GraphQLString;
//            case MAP -> GraphQLString;
//            case DISTINCT -> GraphQLString;
//            case STRUCTURED -> GraphQLString;
//            case ROW -> GraphQLString;
//            case OTHER -> GraphQLString;
//            case CURSOR -> GraphQLString;
//            case COLUMN_LIST -> GraphQLString;
//            case DYNAMIC_STAR -> GraphQLString;
//            case GEOMETRY -> GraphQLString;
//            case SARG -> GraphQLString;
//        };
//    }
//
//    public static GraphQLSchema calciteSchemaToGraphQLSchema(Schema calciteSchema) {
//        return GraphQLSchema.newSchema()
//                .query(
//                        GraphQLObjectType.newObject()
//                                .name("Query")
//                                .fields(calciteSchema.getTableNames()
//                                        .stream()
//                                        .map(tableName -> GraphQLFieldDefinition.newFieldDefinition()
//                                                .name(tableName)
//                                                .type(GraphQLObjectType.newObject()
//                                                        .name(tableName)
//                                                        .fields(
//                                                                Objects.requireNonNull(calciteSchema.getTable(tableName))
//                                                                        .getRowType(new JavaTypeFactoryImpl())
//                                                                        .getFieldList()
//                                                                        .stream()
//                                                                        .map(field -> GraphQLFieldDefinition.newFieldDefinition()
//                                                                                .name(field.getName())
//                                                                                .type((GraphQLOutputType) relDataTypeToGraphQLType(field.getType()))
//                                                                                .build())
//                                                                        .toList()
//                                                        ))
//                                                .arguments(
//                                                        List.of(
//                                                                GraphQLArgument.newArgument()
//                                                                        .name("limit")
//                                                                        .type(GraphQLInt)
//                                                                        .build(),
//                                                                GraphQLArgument.newArgument()
//                                                                        .name("offset")
//                                                                        .type(GraphQLInt)
//                                                                        .build(),
//                                                                GraphQLArgument.newArgument()
//                                                                        .name("order_by")
//                                                                        .type(GraphQLList.list(GraphQLString))
//                                                                        .build(),
//                                                                GraphQLArgument
//                                                                        .newArgument()
//                                                                        .name("where")
//                                                                        .type(
//                                                                                GraphQLInputObjectType.newInputObject()
//                                                                                        .name(tableName + "_bool_exp")
//                                                                                        .fields(
//                                                                                                Objects.requireNonNull(calciteSchema.getTable(tableName))
//                                                                                                        .getRowType(new JavaTypeFactoryImpl())
//                                                                                                        .getFieldList()
//                                                                                                        .stream()
//                                                                                                        .map(field1 -> GraphQLInputObjectField.newInputObjectField()
//                                                                                                                .name(field1.getName())
//                                                                                                                .type((GraphQLInputType) relDataTypeToGraphQLComparisonExprType(field1.getType()))
//                                                                                                                .build())
//                                                                                                        .toList()
//
//                                                                                        )
//                                                                                        .field(
//                                                                                                GraphQLInputObjectField.newInputObjectField()
//                                                                                                        .name("_and")
//                                                                                                        .type(GraphQLList.list(new GraphQLNonNull(
//                                                                                                                        GraphQLTypeReference.typeRef(tableName + "_bool_exp")
//                                                                                                                ))
//                                                                                                        )
//
//                                                                                        )
//                                                                                        .field(
//                                                                                                GraphQLInputObjectField.newInputObjectField()
//                                                                                                        .name("_or")
//                                                                                                        .type(GraphQLList.list(new GraphQLNonNull(
//                                                                                                                        GraphQLTypeReference.typeRef(tableName + "_bool_exp")
//                                                                                                                ))
//                                                                                                        )
//
//                                                                                        )
//                                                                                        .field(
//                                                                                                GraphQLInputObjectField.newInputObjectField()
//                                                                                                        .name("_not")
//                                                                                                        .type(GraphQLTypeReference.typeRef(tableName + "_bool_exp"))
//                                                                                                        .build()
//                                                                                        )
//                                                                                        .build()
//
//                                                                        ).build()
//                                                        )
//                                                )
//                                                .build()
//                                        )
//                                        .toList()
//                                )
//                ).build();
//    }
//
//
//    public static RelNode graphqlQueryToCalciteRelNode(GraphQLSchema schema, Document query, Map<String, Object> variables) {
//        QueryTraverser queryTraversal = QueryTraverser.newQueryTraverser()
//                .schema(schema)
//                .document(query)
//                .variables(variables)
//                .build();
//
//        NodeTraverser nodeTraverser = new NodeTraverser();
//
//        RelBuilder relBuilder = RelFactories.LOGICAL_BUILDER.create(
//                RelOptCluster.create(new VolcanoPlanner(), new RexBuilder(new JavaTypeFactoryImpl())),
//                null
//        );
//
//
//        queryTraversal.visitPostOrder(new QueryVisitor() {
//            @Override
//            public void visitField(QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {
//                GraphQLFieldDefinition fieldDefinition = queryVisitorFieldEnvironment.getFieldDefinition();
//                System.out.println("visitField: " + fieldDefinition.toString());
//
//                if (fieldDefinition.getType() instanceof GraphQLObjectType objectType) {
//                }
//            }
//
//            @Override
//            public TraversalControl visitArgument(QueryVisitorFieldArgumentEnvironment environment) {
//                Argument argument = environment.getArgument();
//                Value value = argument.getValue();
//
//                System.out.println("visitArgument - Argument: " + argument);
//                System.out.println("visitArgument - Value: " + value.toString());
//
//                switch (argument.getName()) {
//                    case "_and" -> {
//                    }
//                    case "_or" -> {
//                    }
//                    case "_not" -> {
//                    }
//                    case "where" -> {
//                        if (value instanceof ObjectValue objectValue) {
//
//                            objectValue.getObjectFields().forEach(objectField -> {
//                                System.out.println("\t visitArgument - ObjectField: " + objectField.toString());
//
//                                if (objectField.getValue() instanceof ObjectValue objectValue2) {
//                                    objectValue2.getObjectFields().forEach(objectField2 -> {
//                                        System.out.println("\t\t visitArgument - ObjectField2: " + objectField2.toString());
//                                    });
//                                }
//                            });
//
//                        }
//                    }
//                    case "orderBy" -> {
//                    }
//                    case "limit" -> {
//                    }
//                    case "offset" -> {
//                    }
//                    default -> {
//                    }
//                }
//
//                return TraversalControl.CONTINUE;
//            }
//
//            @Override
//            public void visitInlineFragment(QueryVisitorInlineFragmentEnvironment queryVisitorInlineFragmentEnvironment) {
//                return;
//            }
//
//            @Override
//            public void visitFragmentSpread(QueryVisitorFragmentSpreadEnvironment queryVisitorFragmentSpreadEnvironment) {
//                return;
//            }
//        });
//
////        GraphQLObjectType queryRoot = schema.getQueryType();
////
////        Optional<OperationDefinition> maybeQueryDefinition = query.getDefinitionsOfType(OperationDefinition.class).stream()
////                .filter(operationDefinition -> operationDefinition.getOperation().equals(OperationDefinition.Operation.QUERY))
////                .findFirst();
////
////        if (maybeQueryDefinition.isEmpty()) {
////            throw new IllegalArgumentException("graphqlQueryToCalciteRelNode: query must contain a query operation");
////        }
////
////        OperationDefinition queryDefinition = maybeQueryDefinition.get();
//
//
//        return null;
//    }
//
//}
//
//public class Example {
//    public static void main(String[] args) {
//
//        SchemaPlus rootSchema = Frameworks.createRootSchema(true);
//        System.out.println("rootSchema: " + rootSchema);
//
//
//        rootSchema.add("users", new AbstractTable() {
//            @Override
//            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
//                return typeFactory.builder()
//                        .add("id", SqlTypeName.INTEGER)
//                        .add("email", SqlTypeName.VARCHAR)
//                        .add("name", SqlTypeName.VARCHAR)
//                        .add("account_confirmed", SqlTypeName.BOOLEAN)
//                        .add("balance", SqlTypeName.DOUBLE)
//                        .build();
//            }
//        });
//
//        var graphqlSchema = CalciteAdapterGraphQLSchemaGenerator.calciteSchemaToGraphQLSchema(rootSchema);
//        System.out.println("graphqlSchema: " + new SchemaPrinter().print(graphqlSchema));
//
//        var graphqlQuery = """
//                query {
//                    users(
//                        where: {
//                            account_confirmed: { _eq: true },
//                            balance: { _gte: 100 },
//                            _or: [
//                                {
//                                  id: { _eq: 1 }
//                                },
//                                {
//                                  name: { _eq: "John" }
//                                }
//                            ]
//                        }
//                    ) {
//                            id
//                            email
//                            name
//                            account_confirmed
//                            balance
//                    }
//                }
//                """;
//        var graphqlVariables = new HashMap<String, Object>();
//        var queryDocument = ParseAndValidate.parse(ExecutionInput.newExecutionInput(graphqlQuery).build());
//        var graphqlRelNode = CalciteAdapterGraphQLSchemaGenerator.graphqlQueryToCalciteRelNode(
//                graphqlSchema, queryDocument.getDocument(), graphqlVariables);
//
//        System.out.println("graphqlRelNode: " + graphqlRelNode);
//    }
//}