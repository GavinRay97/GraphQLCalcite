//import com.google.common.collect.ImmutableList;
//import com.google.common.collect.ImmutableMap;
//import graphql.ExecutionInput;
//import graphql.ParseAndValidate;
//import graphql.Scalars;
//import graphql.language.*;
//import graphql.schema.*;
//import graphql.schema.idl.SchemaPrinter;
//import org.apache.calcite.DataContext;
//import org.apache.calcite.adapter.enumerable.EnumerableConvention;
//import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
//import org.apache.calcite.linq4j.Enumerable;
//import org.apache.calcite.linq4j.Linq4j;
//import org.apache.calcite.plan.*;
//import org.apache.calcite.plan.visualizer.RuleMatchVisualizer;
//import org.apache.calcite.plan.volcano.VolcanoPlanner;
//import org.apache.calcite.prepare.RelOptTableImpl;
//import org.apache.calcite.rel.RelCollationTraitDef;
//import org.apache.calcite.rel.RelCollations;
//import org.apache.calcite.rel.RelFieldCollation;
//import org.apache.calcite.rel.RelNode;
//import org.apache.calcite.rel.core.RelFactories;
//import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
//import org.apache.calcite.rel.type.RelDataType;
//import org.apache.calcite.rel.type.RelDataTypeFactory;
//import org.apache.calcite.rex.RexBuilder;
//import org.apache.calcite.rex.RexLiteral;
//import org.apache.calcite.rex.RexNode;
//import org.apache.calcite.schema.*;
//import org.apache.calcite.schema.impl.AbstractSchema;
//import org.apache.calcite.schema.impl.AbstractTable;
//import org.apache.calcite.sql.SqlExplainFormat;
//import org.apache.calcite.sql.SqlExplainLevel;
//import org.apache.calcite.sql.SqlNode;
//import org.apache.calcite.sql.SqlOperator;
//import org.apache.calcite.sql.dialect.CalciteSqlDialect;
//import org.apache.calcite.sql.fun.SqlStdOperatorTable;
//import org.apache.calcite.sql.parser.SqlParseException;
//import org.apache.calcite.sql.parser.SqlParser;
//import org.apache.calcite.sql.type.SqlTypeFamily;
//import org.apache.calcite.tools.*;
//import org.apache.calcite.util.ImmutableBitSet;
//import org.apache.calcite.util.Pair;
//import org.checkerframework.checker.nullness.qual.Nullable;
//
//import java.io.PrintWriter;
//import java.util.*;
//import java.util.function.Function;
//
//import static graphql.Scalars.*;
//
///**
// * A typical HR schema with employees (emps) and departments (depts) tables that are naturally
// * ordered based on their primary keys representing clustered tables.
// */
//final class HrClusteredSchema extends AbstractSchema {
//
//    private final ImmutableMap<String, Table> tables;
//
//    public HrClusteredSchema() {
//        tables = ImmutableMap.<String, Table>builder()
//                .put("emps",
//                        new PkClusteredTable(
//                                factory ->
//                                        new RelDataTypeFactory.Builder(factory)
//                                                .add("empid", factory.createJavaType(int.class))
//                                                .add("deptno", factory.createJavaType(int.class))
//                                                .add("name", factory.createJavaType(String.class))
//                                                .add("salary", factory.createJavaType(int.class))
//                                                .add("commission", factory.createJavaType(Integer.class))
//                                                .build(),
//                                ImmutableBitSet.of(0),
//                                Arrays.asList(
//                                        new Object[]{100, 10, "Bill", 10000, 1000},
//                                        new Object[]{110, 10, "Theodore", 11500, 250},
//                                        new Object[]{150, 10, "Sebastian", 7000, null},
//                                        new Object[]{200, 20, "Eric", 8000, 500})))
//                .put("depts",
//                        new PkClusteredTable(
//                                factory ->
//                                        new RelDataTypeFactory.Builder(factory)
//                                                .add("deptno", factory.createJavaType(int.class))
//                                                .add("name", factory.createJavaType(String.class))
//                                                .build(),
//                                ImmutableBitSet.of(0),
//                                Arrays.asList(
//                                        new Object[]{10, "Sales"},
//                                        new Object[]{30, "Marketing"},
//                                        new Object[]{40, "HR"})))
//                .build();
//    }
//
//    @Override
//    protected Map<String, Table> getTableMap() {
//        return tables;
//    }
//
//    /**
//     * A table sorted (ascending direction and nulls last) on the primary key.
//     */
//    private static class PkClusteredTable extends AbstractTable implements ScannableTable {
//        private final ImmutableBitSet pkColumns;
//        private final List<Object[]> data;
//        private final Function<RelDataTypeFactory, RelDataType> typeBuilder;
//
//        PkClusteredTable(
//                Function<RelDataTypeFactory, RelDataType> dataTypeBuilder,
//                ImmutableBitSet pkColumns,
//                List<Object[]> data) {
//            this.data = data;
//            this.typeBuilder = dataTypeBuilder;
//            this.pkColumns = pkColumns;
//        }
//
//        @Override
//        public Statistic getStatistic() {
//            List<RelFieldCollation> collationFields = new ArrayList<>();
//            for (Integer key : pkColumns) {
//                collationFields.add(
//                        new RelFieldCollation(
//                                key,
//                                RelFieldCollation.Direction.ASCENDING,
//                                RelFieldCollation.NullDirection.LAST));
//            }
//            return Statistics.of(data.size(), ImmutableList.of(pkColumns),
//                    ImmutableList.of(RelCollations.of(collationFields)));
//        }
//
//        @Override
//        public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
//            return typeBuilder.apply(typeFactory);
//        }
//
//        @Override
//        public Enumerable<@Nullable Object[]> scan(final DataContext root) {
//            return Linq4j.asEnumerable(data);
//        }
//
//    }
//}
//
//class BaseGraphQLTypes {
//
//    record ComparisonExpressionInputType(GraphQLScalarType type, String name, String description) {
//        public GraphQLInputObjectType build() {
//            var inputType = GraphQLInputObjectType
//                    .newInputObject()
//                    .name(name)
//                    .description(description);
//
//            for (var entry : List.of("_eq", "_ne", "_gt", "_gte", "_lt", "_lte")) {
//                inputType.field(
//                        GraphQLInputObjectField
//                                .newInputObjectField()
//                                .name(entry)
//                                .type(type).build()
//                );
//            }
//
//            for (var entry : List.of("_in", "_nin")) {
//                inputType.field(
//                        GraphQLInputObjectField
//                                .newInputObjectField()
//                                .name(entry).type(new GraphQLList(type))
//                                .build()
//                );
//            }
//
//            return inputType.build();
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
//        var fields = List.of(
//                new Pair<>("_ilike", "does the column match the given case-insensitive pattern"),
//                new Pair<>("_like", "does the column match the given pattern"),
//                new Pair<>("_nilike", "does the column NOT match the given case-insensitive pattern"),
//                new Pair<>("_nlike", "does the column NOT match the given pattern"),
//                new Pair<>("_regex", "does the column match the given regular expression"),
//                new Pair<>("_nregex", "does the column NOT match the given regular expression"),
//                new Pair<>("_iregex", "does the column match the given case-insensitive regular expression"),
//                new Pair<>("_niregex", "does the column NOT match the given case-insensitive regular expression"),
//                new Pair<>("_similar", "does the column match the given SQL regular expression"),
//                new Pair<>("_nsimilar", "does the column NOT match the given SQL regular expression")
//        );
//
//        for (Pair<String, String> field : fields) {
//            t.field(GraphQLInputObjectField.newInputObjectField()
//                    .name(field.getKey())
//                    .type(GraphQLString)
//                    .description(field.getValue()));
//        }
//    });
//}
//
//class CalciteAdapterGraphQLSchemaGenerator {
//
//    public static GraphQLType relDataTypeToGraphQLType(RelDataType type) {
//        SqlTypeFamily family = (SqlTypeFamily) type.getFamily();
//        return switch (family) {
//            case BOOLEAN -> Scalars.GraphQLBoolean;
//            case INTEGER -> Scalars.GraphQLInt;
//
//            case NUMERIC, DECIMAL -> Scalars.GraphQLFloat;
//            case CHARACTER, STRING -> Scalars.GraphQLString;
//
//            case DATE, DATETIME -> Scalars.GraphQLString;
//            case TIME, TIMESTAMP -> Scalars.GraphQLString;
//            default -> throw new IllegalArgumentException("Unknown type" + type);
//        };
//    }
//
//    public static GraphQLType relDataTypeToGraphQLComparisonExprType(RelDataType type) {
//        SqlTypeFamily family = (SqlTypeFamily) type.getFamily();
//        return switch (family) {
//            case BOOLEAN -> BaseGraphQLTypes.BooleanComparisonExpressionType;
//            case INTEGER -> BaseGraphQLTypes.IntComparisonExpressionType;
//
//            case NUMERIC, DECIMAL -> BaseGraphQLTypes.FloatComparisonExpressionType;
//            case CHARACTER, STRING -> BaseGraphQLTypes.StringComparisonExpressionType;
//
//            case DATE, DATETIME -> BaseGraphQLTypes.StringComparisonExpressionType;
//            case TIME, TIMESTAMP -> BaseGraphQLTypes.StringComparisonExpressionType;
//            default -> throw new IllegalArgumentException("Unknown type" + type);
//        };
//    }
//
//    private static RexLiteral graphqlObjectValueToRexLiteralValue(RelBuilder builder, Value<?> value) {
//        return switch (value) {
//            case StringValue it -> builder.literal(it.getValue());
//            case IntValue it -> builder.literal(it.getValue());
//            case FloatValue it -> builder.literal(it.getValue());
//            case BooleanValue it -> builder.literal(it.isValue());
//            case NullValue it -> builder.literal(null);
//            default -> throw new IllegalArgumentException("Unsupported value type" + value.getClass());
//        };
//    }
//
//    public static List<RexNode> recursiveGQLWherePredicatesToRexNodePredicates(RelBuilder builder, ObjectValue
//            gqlNode) {
//        return gqlNode.getObjectFields().stream().map(field -> switch (field.getName()) {
//            case "_and" -> {
//                ArrayValue predicates = (ArrayValue) field.getValue();
//                yield List.of(
//                        builder.and(
//                                predicates.getValues().stream().map(it ->
//                                        recursiveGQLWherePredicatesToRexNodePredicates(
//                                                builder,
//                                                (ObjectValue) it
//                                        )
//                                ).flatMap(List::stream).toList()
//                        )
//                );
//            }
//            case "_or" -> {
//                ArrayValue predicates = (ArrayValue) field.getValue();
//                yield List.of(
//                        builder.or(
//                                predicates.getValues().stream().map(it ->
//                                        recursiveGQLWherePredicatesToRexNodePredicates(
//                                                builder,
//                                                (ObjectValue) it
//                                        )
//                                ).flatMap(List::stream).toList()
//                        )
//                );
//
//            }
//            case "_not" -> {
//                ObjectValue predicate = (ObjectValue) field.getValue();
//                yield List.of(
//                        builder.not(
//                                recursiveGQLWherePredicatesToRexNodePredicates(
//                                        builder,
//                                        predicate
//                                ).get(0)
//                        )
//                );
//            }
//            default -> toRexNode(builder, field);
//        }).flatMap(List::stream).toList();
//    }
//
//    private static List<RexNode> toRexNode(RelBuilder builder, ObjectField it) {
//        ObjectField firstField = ((ObjectValue) it.getValue()).getObjectFields().get(0);
//        var value = firstField.getValue();
//        ComparisonOperator operator = ComparisonOperator.fromGqlArgumentName(firstField.getName());
//        return List.of(
//                builder.call(
//                        operator.getSqlOperator(),
//                        builder.field(it.getName()),
//                        graphqlObjectValueToRexLiteralValue(builder, value)
//                )
//        );
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
//}
//
//enum ComparisonOperator {
//    EQUAL("_eq", SqlStdOperatorTable.EQUALS),
//    NOT_EQUAL("_neq", SqlStdOperatorTable.NOT_EQUALS),
//    GREATER_THAN("_gt", SqlStdOperatorTable.GREATER_THAN),
//    GREATER_THAN_OR_EQUAL("_gte", SqlStdOperatorTable.GREATER_THAN_OR_EQUAL),
//    LESS_THAN("_lt", SqlStdOperatorTable.LESS_THAN),
//    LESS_THAN_OR_EQUAL("_lte", SqlStdOperatorTable.LESS_THAN_OR_EQUAL),
//    IN("_in", SqlStdOperatorTable.IN),
//    NOT_IN("_not_in", SqlStdOperatorTable.NOT_IN);
////    LIKE("_like"),
////    ILIKE("_ilike");
//
//    private final String gqlArgumentName;
//    private final SqlOperator sqlOperator;
//
//    ComparisonOperator(String gqlArgumentName, SqlOperator sqlOperator) {
//        this.gqlArgumentName = gqlArgumentName;
//        this.sqlOperator = sqlOperator;
//    }
//
//    public String getGqlArgumentName() {
//        return gqlArgumentName;
//    }
//
//    public SqlOperator getSqlOperator() {
//        return sqlOperator;
//    }
//
//    public static ComparisonOperator fromGqlArgumentName(String gqlArgumentName) {
//        for (ComparisonOperator comparisonOperator : values()) {
//            if (comparisonOperator.getGqlArgumentName().equals(gqlArgumentName)) {
//                return comparisonOperator;
//            }
//        }
//        throw new IllegalArgumentException("ComparisonOperator not found for gqlArgumentName: " + gqlArgumentName);
//    }
//}
//
//public class Example {
//
//    private final static String sqlQuery = """
//                SELECT
//                    empid, name, salary, deptno, commission
//                FROM
//                    emps
//                WHERE
//                    deptno = 20
//                    AND (salary > 8000 AND salary < 10000)
//                    AND (name = 'Eric' OR commission = 10)
//            """;
//
//    private final static String graphqlQuery = """
//                query {
//                    emps(
//                        where: {
//                            _and: [
//                                { deptno: { _eq: 20 } }
//                                {
//                                    _and: [
//                                        { salary: { _gte: 8000 } },
//                                        { salary: { _lte: 10000 } }
//                                    ]
//                                }
//                            ],
//                            _or: [
//                                { name: { _eq: "Eric" } },
//                                { commission: { _eq: 10 } }
//                            ]
//                        }
//                    ) {
//                           empid
//                           deptno
//                           name
//                           salary
//                           commission
//                    }
//                }
//            """;
//
//    static RelOptSchema schemaToRelOptSchema(Schema schema) {
//        var dataTypeFactory = new JavaTypeFactoryImpl();
//        return new RelOptSchema() {
//            @Override
//            public RelOptTable getTableForMember(List<String> names) {
//                var fullyQualifiedTableName = String.join(".", names);
//                var table = schema.getTable(fullyQualifiedTableName);
//
//                Objects.requireNonNull(table, "table not found for fullyQualifiedTableName: " + fullyQualifiedTableName);
//
//                return RelOptTableImpl.create(
//                        this,
//                        table.getRowType(dataTypeFactory),
//                        table,
//                        ImmutableList.copyOf(names)
//                );
//            }
//
//            @Override
//            public RelDataTypeFactory getTypeFactory() {
//                return new JavaTypeFactoryImpl();
//            }
//
//            @Override
//            public void registerRules(RelOptPlanner planner) {
//            }
//        };
//    }
//
//    static void executeQuery(FrameworkConfig config, String query, boolean debug)
//            throws RelConversionException, SqlParseException, ValidationException {
//        Planner planner = Frameworks.getPlanner(config);
//        if (debug) {
//            System.out.println("Query: \n" + query);
//        }
//        SqlNode n = planner.parse(query);
//        n = planner.validate(n);
//        RelNode root = planner.rel(n).project();
//        if (debug) {
//            System.out.println(
//                    RelOptUtil.dumpPlan("-- Logical Plan", root, SqlExplainFormat.TEXT,
//                            SqlExplainLevel.DIGEST_ATTRIBUTES));
//        }
//        RelOptCluster cluster = root.getCluster();
//        final RelOptPlanner optPlanner = cluster.getPlanner();
//
//        RelTraitSet desiredTraits =
//                cluster.traitSet().replace(EnumerableConvention.INSTANCE);
//        final RelNode newRoot = optPlanner.changeTraits(root, desiredTraits);
//        if (debug) {
//            System.out.println(
//                    RelOptUtil.dumpPlan("-- Mid Plan", newRoot, SqlExplainFormat.TEXT,
//                            SqlExplainLevel.DIGEST_ATTRIBUTES));
//        }
//        optPlanner.setRoot(newRoot);
//        RelNode bestExp = optPlanner.findBestExp();
//        if (debug) {
//            System.out.println(
//                    RelOptUtil.dumpPlan("-- Best Plan", bestExp, SqlExplainFormat.TEXT,
//                            SqlExplainLevel.DIGEST_ATTRIBUTES));
//        }
//    }
//
//    private static void testSqlQuery(String query)
//            throws ValidationException, SqlParseException, RelConversionException {
//        FrameworkConfig frameworkConfig = Frameworks
//                .newConfigBuilder()
//                .parserConfig(
//                        // Need to set case-sensitive to false, or else it tries to look up capitalized table names and fails
//                        // IE: "EMPS" instead of "emps"
//                        SqlParser
//                                .config()
//                                .withCaseSensitive(false)
//                )
//                .defaultSchema(
//                        Frameworks
//                                .createRootSchema(true)
//                                .add("hr", new HrClusteredSchema())
//                )
//                .build();
//
//        executeQuery(frameworkConfig, query, true);
//    }
//
//    public static void main(String[] args) {
//        var rootSchema = new HrClusteredSchema();
//        System.out.println(rootSchema);
//
//        try {
//            testSqlQuery(sqlQuery);
//        } catch (ValidationException | SqlParseException | RelConversionException e) {
//            e.printStackTrace();
//        }
//
//        var graphqlSchema = CalciteAdapterGraphQLSchemaGenerator.calciteSchemaToGraphQLSchema(rootSchema);
//        System.out.println("graphqlSchema: " + new SchemaPrinter().print(graphqlSchema));
//
//        var graphqlVariables = new HashMap<String, Object>();
//        var queryDocument = ParseAndValidate.parse(ExecutionInput.newExecutionInput(graphqlQuery).build());
//
//        var planner = new VolcanoPlanner();
//        planner.setTopDownOpt(false);
//        planner.addRelTraitDef(ConventionTraitDef.INSTANCE);
//        planner.addRelTraitDef(RelCollationTraitDef.INSTANCE);
//        RelOptUtil.registerDefaultRules(planner, false, false);
//
//        var viz = new RuleMatchVisualizer();
//        viz.attachTo(planner);
//
//        var relBuilder = RelFactories.LOGICAL_BUILDER.create(
//                RelOptCluster.create(planner, new RexBuilder(new JavaTypeFactoryImpl())),
//                schemaToRelOptSchema(rootSchema)
//        );
//
//        var topLevelQueryNodes = queryDocument
//                .getDocument()
//                .getDefinitionsOfType(OperationDefinition.class)
//                .stream()
//                .filter(it -> it.getOperation().equals(OperationDefinition.Operation.QUERY))
//                .toList();
//
//        for (OperationDefinition node : topLevelQueryNodes) {
//            for (Selection selection : node.getSelectionSet().getSelections().stream().filter(it -> it instanceof Field).toList()) {
//                var field = (Field) selection;
//                Argument whereArgument = field.getArguments().stream().filter(it -> it.getName().equals("where")).findFirst().orElse(null);
//                ObjectValue whereArgumentValue = (ObjectValue) whereArgument.getValue();
//
//                relBuilder
//                        .scan(field.getName())
//                        .filter(
//                                CalciteAdapterGraphQLSchemaGenerator.recursiveGQLWherePredicatesToRexNodePredicates(
//                                        relBuilder,
//                                        whereArgumentValue
//                                )
//                        )
//                        .project(
//                                relBuilder.field("empid"),
//                                relBuilder.field("name")
//                        );
//
//                System.out.println("RelBuilder result: ");
//                System.out.println(relBuilder);
//
//                RelNode relRoot = relBuilder.build();
//                var dialect = CalciteSqlDialect.DEFAULT;
//                var converter = new RelToSqlConverter(dialect);
//                var sqlNode = converter.visitRoot(relRoot).asStatement();
//                System.out.println("RelToSqlConverter result: ");
//                System.out.println(sqlNode.toSqlString(dialect).getSql());
//
//                planner.setRoot(relRoot);
//                System.out.println(viz.getJsonStringResult());
//
//                var printWriter = new PrintWriter(System.out, true);
//                planner.dump(printWriter);
//
//                var planned = planner.findBestExp();
//                System.out.println("Planner result: ");
//                System.out.println(planned);
//            }
//        }
//    }
//}