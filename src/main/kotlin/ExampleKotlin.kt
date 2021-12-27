import com.google.common.collect.ImmutableList
import graphql.ExecutionInput
import graphql.ParseAndValidate
import graphql.Scalars
import graphql.language.Argument
import graphql.language.ArrayValue
import graphql.language.BooleanValue
import graphql.language.Field
import graphql.language.FloatValue
import graphql.language.IntValue
import graphql.language.NullValue
import graphql.language.ObjectField
import graphql.language.ObjectValue
import graphql.language.OperationDefinition
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLList
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import graphql.schema.GraphQLTypeReference
import graphql.schema.idl.SchemaPrinter
import org.apache.calcite.adapter.enumerable.EnumerableConvention
import org.apache.calcite.jdbc.JavaTypeFactoryImpl
import org.apache.calcite.plan.ConventionTraitDef
import org.apache.calcite.plan.RelOptCluster
import org.apache.calcite.plan.RelOptPlanner
import org.apache.calcite.plan.RelOptSchema
import org.apache.calcite.plan.RelOptTable
import org.apache.calcite.plan.RelOptUtil
import org.apache.calcite.plan.visualizer.RuleMatchVisualizer
import org.apache.calcite.plan.volcano.VolcanoPlanner
import org.apache.calcite.prepare.RelOptTableImpl
import org.apache.calcite.rel.RelCollationTraitDef
import org.apache.calcite.rel.RelNode
import org.apache.calcite.rel.core.RelFactories
import org.apache.calcite.rel.rel2sql.RelToSqlConverter
import org.apache.calcite.rel.type.RelDataType
import org.apache.calcite.rel.type.RelDataTypeFactory
import org.apache.calcite.rel.type.RelDataTypeField
import org.apache.calcite.rex.RexBuilder
import org.apache.calcite.rex.RexLiteral
import org.apache.calcite.rex.RexNode
import org.apache.calcite.schema.Schema
import org.apache.calcite.schema.impl.AbstractSchema
import org.apache.calcite.sql.SqlBinaryOperator
import org.apache.calcite.sql.SqlExplainFormat
import org.apache.calcite.sql.SqlExplainLevel
import org.apache.calcite.sql.`fun`.SqlStdOperatorTable
import org.apache.calcite.sql.dialect.CalciteSqlDialect
import org.apache.calcite.sql.parser.SqlParser
import org.apache.calcite.sql.type.SqlTypeFamily
import org.apache.calcite.tools.FrameworkConfig
import org.apache.calcite.tools.Frameworks
import org.apache.calcite.tools.RelBuilder
import java.io.PrintWriter

object BaseGraphQLTypes {

    val IntComparisonExpressionType = mkComparisonExpressionInputType(
        Scalars.GraphQLInt,
        "Int_comparison_exp",
        "Boolean expression to compare columns of type \"Int\". All fields are combined with logical 'AND'."
    )

    val FloatComparisonExpressionType = mkComparisonExpressionInputType(
        Scalars.GraphQLFloat,
        "Float_comparison_exp",
        "Boolean expression to compare columns of type \"Float\". All fields are combined with logical 'AND'."
    )
    val BooleanComparisonExpressionType = mkComparisonExpressionInputType(
        Scalars.GraphQLBoolean,
        "Boolean_comparison_exp",
        "Boolean expression to compare columns of type \"Boolean\". All fields are combined with logical 'AND'."
    )

    val StringComparisonExpressionType: GraphQLInputObjectType = mkComparisonExpressionInputType(
        Scalars.GraphQLString,
        "String_comparison_exp",
        "Boolean expression to compare columns of type \"String\". All fields are combined with logical 'AND'."
    ).transform { t: GraphQLInputObjectType.Builder ->
        val fields = listOf(
            Pair("_ilike", "does the column match the given case-insensitive pattern"),
            Pair("_like", "does the column match the given pattern"),
            Pair("_nilike", "does the column NOT match the given case-insensitive pattern"),
            Pair("_nlike", "does the column NOT match the given pattern"),
            Pair("_regex", "does the column match the given regular expression"),
            Pair("_nregex", "does the column NOT match the given regular expression"),
            Pair("_iregex", "does the column match the given case-insensitive regular expression"),
            Pair("_niregex", "does the column NOT match the given case-insensitive regular expression"),
            Pair("_similar", "does the column match the given SQL regular expression"),
            Pair("_nsimilar", "does the column NOT match the given SQL regular expression"),
        )
        for ((name, description) in fields) {
            t.field(
                GraphQLInputObjectField.newInputObjectField()
                    .name(name)
                    .description(description)
                    .type(Scalars.GraphQLString)
            )
        }
    }

    private fun mkComparisonExpressionInputType(
        type: GraphQLInputType,
        name: String,
        description: String
    ): GraphQLInputObjectType {
        val inputObject = GraphQLInputObjectType.newInputObject()
            .name(name)
            .description(description)

        for (entry in listOf("_eq", "_ne", "_gt", "_gte", "_lt", "_lte")) {
            inputObject.field(
                GraphQLInputObjectField.newInputObjectField()
                    .name(entry)
                    .type(type)
            )
        }

        for (entry in listOf("_in", "_nin")) {
            inputObject.field(
                GraphQLInputObjectField.newInputObjectField()
                    .name(entry)
                    .type(GraphQLList(GraphQLNonNull(type)))
            )
        }

        return inputObject.build()
    }
}

object CalciteGraphQLUtils {

    private fun relDataTypeToGraphQLType(type: RelDataType): GraphQLType {
        return when (type.family) {
            SqlTypeFamily.BOOLEAN -> Scalars.GraphQLBoolean
            SqlTypeFamily.INTEGER -> Scalars.GraphQLInt

            SqlTypeFamily.NUMERIC, SqlTypeFamily.DECIMAL -> Scalars.GraphQLFloat
            SqlTypeFamily.CHARACTER, SqlTypeFamily.STRING -> Scalars.GraphQLString

            SqlTypeFamily.DATE, SqlTypeFamily.DATETIME -> Scalars.GraphQLString
            SqlTypeFamily.TIME, SqlTypeFamily.TIMESTAMP -> Scalars.GraphQLString

            else -> throw IllegalArgumentException("Unknown type: $type")
        }
    }

    private fun relDataTypeToGraphQLComparisonExprType(type: RelDataType): GraphQLType {
        return when (type.family) {
            SqlTypeFamily.BOOLEAN -> BaseGraphQLTypes.BooleanComparisonExpressionType
            SqlTypeFamily.INTEGER -> BaseGraphQLTypes.IntComparisonExpressionType

            SqlTypeFamily.NUMERIC, SqlTypeFamily.DECIMAL -> BaseGraphQLTypes.FloatComparisonExpressionType
            SqlTypeFamily.CHARACTER, SqlTypeFamily.STRING -> BaseGraphQLTypes.StringComparisonExpressionType

            SqlTypeFamily.DATE, SqlTypeFamily.DATETIME -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeFamily.TIME, SqlTypeFamily.TIMESTAMP -> BaseGraphQLTypes.StringComparisonExpressionType

            else -> throw IllegalArgumentException("Unknown type: $type")
        }
    }

    private fun graphqlObjectValueToRexLiteralValue(builder: RelBuilder, value: Value<*>): RexLiteral {
        return when (value) {
            is StringValue -> builder.literal(value.value as String)
            is IntValue -> builder.literal(value.value)
            is FloatValue -> builder.literal(value.value)
            is BooleanValue -> builder.literal(value.isValue)
            is NullValue -> builder.literal(null)
            else -> throw IllegalArgumentException("Unsupported value type: ${value.javaClass}")
        }
    }

    /**
     * Converts a GraphQL [ObjectValue] from the "where" clause in a query to a Calcite [RexNode]
     *
     * Example:
     *  query {
     *      users(where: {
     *          _and: [{ name: { _eq: "John" } }, { age: { _gte: 25 } }]
     *      }) {}
     *  }
     *
     * @param builder the builder to use to create the RexNode
     * @param gqlNode the GraphQL [ObjectValue] to convert
     * @return the Calcite [RexNode]
     */
    fun recursiveGQLWherePredicatesToRexNodePredicates(
        builder: RelBuilder,
        gqlNode: ObjectValue
    ): List<RexNode> {
        // We can safely parrallelize the map because order is not important
        return gqlNode.objectFields.stream().parallel().map {
            println("Processing field: ${it.name} on thread ${Thread.currentThread().name}")
            return@map when (it.name) {
                "_and" -> {
                    val andPredicates = it.value as? ArrayValue
                        ?: throw IllegalArgumentException("Expected _and to be an array")
                    println("AND: ${andPredicates.values}")

                    listOf(
                        builder.and(
                            andPredicates.values.flatMap { innerValue ->
                                recursiveGQLWherePredicatesToRexNodePredicates(
                                    builder,
                                    innerValue as ObjectValue
                                )
                            }
                        )
                    )
                }
                "_or" -> {
                    val orPredicates = it.value as? ArrayValue
                        ?: throw IllegalArgumentException("Expected _or to be an array")
                    println("OR: ${orPredicates.values}")
                    listOf(
                        builder.or(
                            orPredicates.values.flatMap { innerValue ->
                                recursiveGQLWherePredicatesToRexNodePredicates(
                                    builder,
                                    innerValue as ObjectValue
                                )
                            }
                        )
                    )
                }
                "_not" -> {
                    val notPredicate = it.value as? ObjectValue
                        ?: throw IllegalArgumentException("Expected _not to be an object")
                    listOf(
                        builder.not(
                            recursiveGQLWherePredicatesToRexNodePredicates(
                                builder,
                                notPredicate
                            ).first()
                        )
                    )
                }
                else -> {
                    toRexNodes(it, builder)
                }
            }
        }.toList().flatten()
    }

    private fun toRexNodes(
        it: ObjectField,
        builder: RelBuilder
    ): List<RexNode> {
        val firstField: ObjectField = (it.value as? ObjectValue)?.objectFields?.first()
            ?: throw IllegalArgumentException("Expected first field")
        val value = firstField.value
            ?: throw IllegalArgumentException("Value is null")
        val predicate = firstField.name.let(ComparisonOperator.Companion::fromValue)
        return listOf(
            builder.call(
                predicate.sqlOperator,
                builder.field(it.name),
                graphqlObjectValueToRexLiteralValue(builder, value)
            )
        )
    }

    fun calciteSchemaToGraphQLSchema(calciteSchema: Schema): GraphQLSchema {
        return GraphQLSchema.newSchema()
            .query(
                GraphQLObjectType.newObject()
                    .name("Query")
                    .fields(
                        calciteSchema.tableNames.map { tableName: String ->
                            GraphQLFieldDefinition.newFieldDefinition()
                                .name(tableName)
                                .type(
                                    GraphQLObjectType.newObject()
                                        .name(tableName)
                                        .fields(
                                            calciteSchema.getTable(tableName)
                                                ?.getRowType(JavaTypeFactoryImpl())
                                                ?.fieldList
                                                ?.map { field: RelDataTypeField ->
                                                    GraphQLFieldDefinition.newFieldDefinition()
                                                        .name(field.name)
                                                        .type(relDataTypeToGraphQLType(field.type) as GraphQLOutputType?)
                                                        .build()
                                                }
                                        )
                                )
                                .arguments(
                                    listOf(
                                        GraphQLArgument.newArgument()
                                            .name("limit")
                                            .type(Scalars.GraphQLInt)
                                            .build(),
                                        GraphQLArgument.newArgument()
                                            .name("offset")
                                            .type(Scalars.GraphQLInt)
                                            .build(),
                                        GraphQLArgument.newArgument()
                                            .name("order_by")
                                            .type(GraphQLList(Scalars.GraphQLString))
                                            .build(),
                                        GraphQLArgument
                                            .newArgument()
                                            .name("where")
                                            .type(
                                                GraphQLInputObjectType.newInputObject()
                                                    .name(tableName + "_bool_exp")
                                                    .fields(
                                                        calciteSchema.getTable(tableName)
                                                            ?.getRowType(JavaTypeFactoryImpl())
                                                            ?.fieldList
                                                            ?.map { field1: RelDataTypeField ->
                                                                GraphQLInputObjectField.newInputObjectField()
                                                                    .name(field1.name)
                                                                    .type(
                                                                        relDataTypeToGraphQLComparisonExprType(
                                                                            field1.type
                                                                        ) as GraphQLInputType?
                                                                    )
                                                                    .build()
                                                            }
                                                    )
                                                    .field(
                                                        GraphQLInputObjectField.newInputObjectField()
                                                            .name("_and")
                                                            .type(
                                                                GraphQLList(
                                                                    GraphQLNonNull(
                                                                        GraphQLTypeReference(tableName + "_bool_exp")
                                                                    )
                                                                )
                                                            )
                                                    )
                                                    .field(
                                                        GraphQLInputObjectField.newInputObjectField()
                                                            .name("_or")
                                                            .type(
                                                                GraphQLList(
                                                                    GraphQLNonNull(
                                                                        GraphQLTypeReference(tableName + "_bool_exp")
                                                                    )
                                                                )
                                                            )
                                                    )
                                                    .field(
                                                        GraphQLInputObjectField.newInputObjectField()
                                                            .name("_not")
                                                            .type(GraphQLTypeReference(tableName + "_bool_exp"))
                                                            .build()
                                                    )
                                                    .build()
                                            ).build()
                                    )
                                ).build()
                        }.toList()
                    )
            ).build()
    }
}

enum class ComparisonOperator(val gqlArgumentName: String, val sqlOperator: SqlBinaryOperator) {
    EQUAL("_eq", SqlStdOperatorTable.EQUALS),
    NOT_EQUAL("_neq", SqlStdOperatorTable.NOT_EQUALS),
    GREATER_THAN("_gt", SqlStdOperatorTable.GREATER_THAN),
    GREATER_THAN_OR_EQUAL("_gte", SqlStdOperatorTable.GREATER_THAN_OR_EQUAL),
    LESS_THAN("_lt", SqlStdOperatorTable.LESS_THAN),
    LESS_THAN_OR_EQUAL("_lte", SqlStdOperatorTable.LESS_THAN_OR_EQUAL),
    IN("_in", SqlStdOperatorTable.IN),
    NOT_IN("_not_in", SqlStdOperatorTable.NOT_IN);
//    LIKE("_like"),
//    ILIKE("_ilike");

    companion object {
        fun fromValue(value: String): ComparisonOperator {
            return values().firstOrNull { it.gqlArgumentName == value }
                ?: throw IllegalArgumentException("Unknown value: $value")
        }
    }
}

fun schemaToRelOptSchema(schema: Schema): RelOptSchema {
    val dataTypeFactory = JavaTypeFactoryImpl()

    return object : RelOptSchema {
        override fun getTableForMember(names: MutableList<String>): RelOptTable? {
            val fullyQualifiedTableName = names.joinToString(".")
            val table = schema.getTable(fullyQualifiedTableName) ?: return null
            return RelOptTableImpl.create(this, table.getRowType(dataTypeFactory), table, ImmutableList.copyOf(names))
        }

        override fun getTypeFactory(): RelDataTypeFactory {
            return JavaTypeFactoryImpl()
        }

        override fun registerRules(planner: RelOptPlanner) {
            return
        }
    }
}

fun executeQuery(
    config: FrameworkConfig,
    query: String,
    debug: Boolean
) {
    val planner = Frameworks.getPlanner(config)
    if (debug) {
        println("Query:$query")
    }
    var n = planner.parse(query)
    n = planner.validate(n)
    val root = planner.rel(n).project()
    if (debug) {
        println(
            RelOptUtil.dumpPlan(
                "-- Logical Plan", root, SqlExplainFormat.TEXT,
                SqlExplainLevel.DIGEST_ATTRIBUTES
            )
        )
    }
    val cluster = root.cluster
    val optPlanner = cluster.planner
    val desiredTraits = cluster.traitSet().replace(EnumerableConvention.INSTANCE)
    val newRoot = optPlanner.changeTraits(root, desiredTraits)
    if (debug) {
        println(
            RelOptUtil.dumpPlan(
                "-- Mid Plan", newRoot, SqlExplainFormat.TEXT,
                SqlExplainLevel.DIGEST_ATTRIBUTES
            )
        )
    }
    optPlanner.root = newRoot
    val bestExp = optPlanner.findBestExp()
    if (debug) {
        println(
            RelOptUtil.dumpPlan(
                "-- Best Plan", bestExp, SqlExplainFormat.TEXT,
                SqlExplainLevel.DIGEST_ATTRIBUTES
            )
        )
    }
}

object ExampleKotlin {

    private const val sqlQuery = """
        SELECT
            empid, name, salary, deptno, commission
        FROM
            emps
        WHERE
            deptno = 20
            AND
                (salary > 8000 AND salary < 10000)
            AND
                (name = 'Eric' OR commission = 10)
    """

    private const val graphqlQuery = """
        query {
            emps(
                where: {
                    _and: [
                        { deptno: { _eq: 20 } }
                        {
                            _and: [
                                { salary: { _gte: 8000 } },
                                { salary: { _lte: 10000 } }
                            ]
                        }
                    ],
                    _or: [
                        { name: { _eq: "Eric" } },
                        { commission: { _eq: 10 } }
                    ]
                }
            ) {
                   empid
                   deptno
                   name
                   salary
                   commission
            }
        }
        """

    fun testSqlQuery() {
        println("Testing Calcite plan for SQL query:")
        println(sqlQuery)

        val frameworkConfig: FrameworkConfig = Frameworks
            .newConfigBuilder()
            .parserConfig(
                // Need to set case-sensitive to false, or else it tries to look up capitalized table names and fails
                // IE: "EMPS" instead of "emps"
                SqlParser
                    .config()
                    .withCaseSensitive(false)
            )
            .defaultSchema(
                Frameworks
                    .createRootSchema(true)
                    .add("hr", HrClusteredSchemaKotlin())
            )
            .build()

        executeQuery(frameworkConfig, sqlQuery, true)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val rootSchema: AbstractSchema = HrClusteredSchemaKotlin()
        println(rootSchema)

        val graphqlSchema: GraphQLSchema = CalciteGraphQLUtils.calciteSchemaToGraphQLSchema(rootSchema)
        println("graphqlSchema: " + SchemaPrinter().print(graphqlSchema))

        val graphqlVariables = HashMap<String, Any>()
        val queryDocument = ParseAndValidate.parse(ExecutionInput.newExecutionInput(graphqlQuery).build())

        val planner = VolcanoPlanner()
        planner.setTopDownOpt(false)
        planner.addRelTraitDef(ConventionTraitDef.INSTANCE)
        planner.addRelTraitDef(RelCollationTraitDef.INSTANCE)
        RelOptUtil.registerDefaultRules(planner, false, false)

        val viz = RuleMatchVisualizer()
        viz.attachTo(planner)

        val relBuilder: RelBuilder = RelFactories.LOGICAL_BUILDER.create(
            RelOptCluster.create(planner, RexBuilder(JavaTypeFactoryImpl())),
            schemaToRelOptSchema(rootSchema)
        )

        val topLevelQueryNodes = queryDocument.document.definitions.filter {
            (it is OperationDefinition) && (it.operation.equals(OperationDefinition.Operation.QUERY))
        } as List<OperationDefinition>

        for (node in topLevelQueryNodes) {
            for (selection in node.selectionSet.selections.filterIsInstance<Field>()) {
                val whereArgument: Argument = selection.arguments.find { it.name == "where" } ?: continue

                val whereArgumentValue = whereArgument.value as ObjectValue
                val whereArgumentFields: MutableList<ObjectField> = whereArgumentValue.objectFields

                relBuilder
                    .scan(selection.name)
                    .filter(
                        CalciteGraphQLUtils.recursiveGQLWherePredicatesToRexNodePredicates(
                            relBuilder,
                            whereArgumentValue
                        )
                    )
                    .project(
                        relBuilder.field("empid"),
                        relBuilder.field("name"),
                    )

                println("RelBuilder result: ")
                println(relBuilder.toString())

                val relRoot: RelNode = relBuilder.build()
                val dialect = CalciteSqlDialect.DEFAULT
                val converter = RelToSqlConverter(dialect)
                val sqlNode = converter.visitRoot(relRoot).asStatement()
                println("RelToSqlConverter result: ")
                println(sqlNode.toSqlString(dialect))

                planner.root = relRoot
                println(viz.jsonStringResult)

                val printWriter = PrintWriter(System.out, true)
                planner.dump(printWriter)

                val planned = planner.findBestExp()
                println("Planner result: ")
                println(planned)
            }
        }
    }
}
