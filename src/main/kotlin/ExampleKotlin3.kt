import com.google.common.collect.ImmutableList
import graphql.ExecutionInput
import graphql.ParseAndValidate
import graphql.Scalars
import graphql.language.*
import graphql.schema.*
import graphql.schema.idl.SchemaPrinter
import org.apache.calcite.jdbc.JavaTypeFactoryImpl
import org.apache.calcite.plan.*
import org.apache.calcite.plan.visualizer.RuleMatchVisualizer
import org.apache.calcite.plan.volcano.VolcanoPlanner
import org.apache.calcite.prepare.RelOptTableImpl
import org.apache.calcite.rel.RelCollationTraitDef
import org.apache.calcite.rel.RelNode
import org.apache.calcite.rel.core.RelFactories
import org.apache.calcite.rel.type.RelDataType
import org.apache.calcite.rel.type.RelDataTypeFactory
import org.apache.calcite.rel.type.RelDataTypeField
import org.apache.calcite.rex.RexBuilder
import org.apache.calcite.rex.RexLiteral
import org.apache.calcite.rex.RexNode
import org.apache.calcite.schema.Schema
import org.apache.calcite.schema.SchemaPlus
import org.apache.calcite.schema.impl.AbstractTable
import org.apache.calcite.sql.type.SqlTypeName
import org.apache.calcite.tools.Frameworks
import org.apache.calcite.tools.RelBuilder


object BaseGraphQLTypes {
    val IntComparisonExpressionType = ComparisonExpressionInputType(
        Scalars.GraphQLInt,
        "Int_comparison_exp",
        "Boolean expression to compare columns of type \"Int\". All fields are combined with logical 'AND'."
    ).build()
    val FloatComparisonExpressionType = ComparisonExpressionInputType(
        Scalars.GraphQLFloat,
        "Float_comparison_exp",
        "Boolean expression to compare columns of type \"Float\". All fields are combined with logical 'AND'."
    ).build()
    val BooleanComparisonExpressionType = ComparisonExpressionInputType(
        Scalars.GraphQLBoolean,
        "Boolean_comparison_exp",
        "Boolean expression to compare columns of type \"Boolean\". All fields are combined with logical 'AND'."
    ).build()
    val StringComparisonExpressionType = ComparisonExpressionInputType(
        Scalars.GraphQLString,
        "String_comparison_exp",
        "Boolean expression to compare columns of type \"String\". All fields are combined with logical 'AND'."
    ).build().transform { t: GraphQLInputObjectType.Builder ->
        t.field(
            GraphQLInputObjectField.newInputObjectField()
                .name("_ilike")
                .description("does the column match the given case-insensitive pattern")
                .type(Scalars.GraphQLString)
        )
        t.field(
            GraphQLInputObjectField.newInputObjectField()
                .name("_iregex")
                .description("does the column match the given POSIX regular expression, case insensitive")
                .type(Scalars.GraphQLString)
        )
        t.field(
            GraphQLInputObjectField.newInputObjectField()
                .name("_like")
                .description("does the column match the given pattern")
                .type(Scalars.GraphQLString)
        )
        t.field(
            GraphQLInputObjectField.newInputObjectField()
                .name("_nilike")
                .description("does the column NOT match the given case-insensitive pattern")
                .type(Scalars.GraphQLString)
        )
        t.field(
            GraphQLInputObjectField.newInputObjectField()
                .name("_niregex")
                .description("does the column NOT match the given POSIX regular expression, case insensitive")
                .type(Scalars.GraphQLString)
        )
        t.field(
            GraphQLInputObjectField.newInputObjectField()
                .name("_nlike")
                .description("does the column NOT match the given pattern")
                .type(Scalars.GraphQLString)
        )
        t.field(
            GraphQLInputObjectField.newInputObjectField()
                .name("_nregex")
                .description("does the column NOT match the given POSIX regular expression, case sensitive")
                .type(Scalars.GraphQLString)
        )
        t.field(
            GraphQLInputObjectField.newInputObjectField()
                .name("_nsimilar")
                .description("does the column NOT match the given SQL regular expression")
                .type(Scalars.GraphQLString)
        )
        t.field(
            GraphQLInputObjectField.newInputObjectField()
                .name("_regex")
                .description("does the column match the given POSIX regular expression, case sensitive")
                .type(Scalars.GraphQLString)
        )
        t.field(
            GraphQLInputObjectField.newInputObjectField()
                .name("_similar")
                .description("does the column match the given SQL regular expression")
                .type(Scalars.GraphQLString)
        )
    }

    data class ComparisonExpressionInputType(
        val type: GraphQLInputType,
        val name: String,
        val description: String
    ) {
        fun build(): GraphQLInputObjectType {
            return GraphQLInputObjectType.newInputObject()
                .name(name)
                .description(description)
                .field(
                    GraphQLInputObjectField.newInputObjectField()
                        .name("_is_null")
                        .type(Scalars.GraphQLBoolean)
                )
                .field(
                    GraphQLInputObjectField.newInputObjectField()
                        .name("_eq")
                        .type(type)
                )
                .field(
                    GraphQLInputObjectField.newInputObjectField()
                        .name("_gt")
                        .type(type)
                )
                .field(
                    GraphQLInputObjectField.newInputObjectField()
                        .name("_gte")
                        .type(type)
                )
                .field(
                    GraphQLInputObjectField.newInputObjectField()
                        .name("_in")
                        .type(GraphQLList(GraphQLNonNull(type)))
                )
                .field(
                    GraphQLInputObjectField.newInputObjectField()
                        .name("_lt")
                        .type(type)
                )
                .field(
                    GraphQLInputObjectField.newInputObjectField()
                        .name("_lte")
                        .type(type)
                )
                .field(
                    GraphQLInputObjectField.newInputObjectField()
                        .name("_neq")
                        .type(type)
                )
                .field(
                    GraphQLInputObjectField.newInputObjectField()
                        .name("_nin")
                        .type(GraphQLList(GraphQLNonNull(type)))
                )
                .build()
        }
    }

    val graphqlTypeToComparsionExpressionTypeMap = mapOf(
        Scalars.GraphQLString::class.java to StringComparisonExpressionType,
        Scalars.GraphQLInt::class.java to IntComparisonExpressionType,
        Scalars.GraphQLFloat::class.java to FloatComparisonExpressionType,
        Scalars.GraphQLBoolean::class.java to BooleanComparisonExpressionType,
    )
}

object CalciteGraphQLUtils {

    private fun relDataTypeToGraphQLType(type: RelDataType): GraphQLType {
        println("relDataTypeToGraphQLType: $type")
        println("type.sqlTypeName: ${type.sqlTypeName}")
        return when (type.sqlTypeName) {
            SqlTypeName.BOOLEAN -> Scalars.GraphQLBoolean
            SqlTypeName.TINYINT, SqlTypeName.SMALLINT, SqlTypeName.INTEGER, SqlTypeName.BIGINT -> Scalars.GraphQLInt
            SqlTypeName.DECIMAL, SqlTypeName.FLOAT, SqlTypeName.REAL, SqlTypeName.DOUBLE -> Scalars.GraphQLFloat
            SqlTypeName.DATE -> Scalars.GraphQLString
            SqlTypeName.TIME -> Scalars.GraphQLString
            SqlTypeName.TIME_WITH_LOCAL_TIME_ZONE -> Scalars.GraphQLString
            SqlTypeName.TIMESTAMP -> Scalars.GraphQLString
            SqlTypeName.TIMESTAMP_WITH_LOCAL_TIME_ZONE -> Scalars.GraphQLString
            SqlTypeName.INTERVAL_YEAR -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_YEAR_MONTH -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_MONTH -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_DAY -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_DAY_HOUR -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_DAY_MINUTE -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_DAY_SECOND -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_HOUR -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_HOUR_MINUTE -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_HOUR_SECOND -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_MINUTE -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_MINUTE_SECOND -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_SECOND -> Scalars.GraphQLInt
            SqlTypeName.CHAR -> Scalars.GraphQLString
            SqlTypeName.VARCHAR -> Scalars.GraphQLString
            SqlTypeName.BINARY -> Scalars.GraphQLString
            SqlTypeName.VARBINARY -> Scalars.GraphQLString
            SqlTypeName.NULL -> Scalars.GraphQLString
            SqlTypeName.ANY -> Scalars.GraphQLString
            SqlTypeName.SYMBOL -> Scalars.GraphQLString
            SqlTypeName.MULTISET -> Scalars.GraphQLString
            SqlTypeName.ARRAY -> Scalars.GraphQLString
            SqlTypeName.MAP -> Scalars.GraphQLString
            SqlTypeName.DISTINCT -> Scalars.GraphQLString
            SqlTypeName.STRUCTURED -> Scalars.GraphQLString
            SqlTypeName.ROW -> Scalars.GraphQLString
            SqlTypeName.OTHER -> Scalars.GraphQLString
            SqlTypeName.CURSOR -> Scalars.GraphQLString
            SqlTypeName.COLUMN_LIST -> Scalars.GraphQLString
            SqlTypeName.DYNAMIC_STAR -> Scalars.GraphQLString
            SqlTypeName.GEOMETRY -> Scalars.GraphQLString
            SqlTypeName.SARG -> Scalars.GraphQLString
            else -> throw IllegalArgumentException("Unknown type: $type")
        }
    }

    private fun relDataTypeToGraphQLComparisonExprType(type: RelDataType): GraphQLType? {
        return when (type.sqlTypeName) {
            SqlTypeName.BOOLEAN -> BaseGraphQLTypes.BooleanComparisonExpressionType
            SqlTypeName.TINYINT, SqlTypeName.SMALLINT, SqlTypeName.INTEGER, SqlTypeName.BIGINT -> BaseGraphQLTypes.IntComparisonExpressionType
            SqlTypeName.DECIMAL, SqlTypeName.FLOAT, SqlTypeName.REAL, SqlTypeName.DOUBLE -> BaseGraphQLTypes.FloatComparisonExpressionType
            SqlTypeName.DATE -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeName.TIME -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeName.TIME_WITH_LOCAL_TIME_ZONE -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeName.TIMESTAMP -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeName.TIMESTAMP_WITH_LOCAL_TIME_ZONE -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeName.INTERVAL_YEAR -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_YEAR_MONTH -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_MONTH -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_DAY -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_DAY_HOUR -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_DAY_MINUTE -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_DAY_SECOND -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_HOUR -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_HOUR_MINUTE -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_HOUR_SECOND -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_MINUTE -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_MINUTE_SECOND -> Scalars.GraphQLInt
            SqlTypeName.INTERVAL_SECOND -> Scalars.GraphQLInt
            SqlTypeName.CHAR -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeName.VARCHAR -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeName.BINARY -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeName.VARBINARY -> BaseGraphQLTypes.StringComparisonExpressionType
            SqlTypeName.NULL -> Scalars.GraphQLString
            SqlTypeName.ANY -> Scalars.GraphQLString
            SqlTypeName.SYMBOL -> Scalars.GraphQLString
            SqlTypeName.MULTISET -> Scalars.GraphQLString
            SqlTypeName.ARRAY -> Scalars.GraphQLString
            SqlTypeName.MAP -> Scalars.GraphQLString
            SqlTypeName.DISTINCT -> Scalars.GraphQLString
            SqlTypeName.STRUCTURED -> Scalars.GraphQLString
            SqlTypeName.ROW -> Scalars.GraphQLString
            SqlTypeName.OTHER -> Scalars.GraphQLString
            SqlTypeName.CURSOR -> Scalars.GraphQLString
            SqlTypeName.COLUMN_LIST -> Scalars.GraphQLString
            SqlTypeName.DYNAMIC_STAR -> Scalars.GraphQLString
            SqlTypeName.GEOMETRY -> Scalars.GraphQLString
            SqlTypeName.SARG -> Scalars.GraphQLString
        }
    }

    fun calciteSchemaToGraphQLSchema(calciteSchema: Schema): GraphQLSchema {
        println("calciteSchemaToGraphQLSchema")
        return GraphQLSchema.newSchema()
            .query(
                GraphQLObjectType.newObject()
                    .name("Query")
                    .fields(calciteSchema.tableNames
                        .stream()
                        .map { tableName: String ->
                            GraphQLFieldDefinition.newFieldDefinition()
                                .name(tableName)
                                .type(
                                    GraphQLObjectType.newObject()
                                        .name(tableName)
                                        .fields(calciteSchema.getTable(tableName)
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
                                            .type(GraphQLList.list(Scalars.GraphQLString))
                                            .build(),
                                        GraphQLArgument
                                            .newArgument()
                                            .name("where")
                                            .type(
                                                GraphQLInputObjectType.newInputObject()
                                                    .name(tableName + "_bool_exp")
                                                    .fields(calciteSchema.getTable(tableName)
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
                                                                GraphQLList.list(
                                                                    GraphQLNonNull(
                                                                        GraphQLTypeReference.typeRef(tableName + "_bool_exp")
                                                                    )
                                                                )
                                                            )
                                                    )
                                                    .field(
                                                        GraphQLInputObjectField.newInputObjectField()
                                                            .name("_or")
                                                            .type(
                                                                GraphQLList.list(
                                                                    GraphQLNonNull(
                                                                        GraphQLTypeReference.typeRef(tableName + "_bool_exp")
                                                                    )
                                                                )
                                                            )
                                                    )
                                                    .field(
                                                        GraphQLInputObjectField.newInputObjectField()
                                                            .name("_not")
                                                            .type(GraphQLTypeReference.typeRef(tableName + "_bool_exp"))
                                                            .build()
                                                    )
                                                    .build()
                                            ).build()
                                    )
                                )
                                .build()
                        }
                        .toList()
                    )
            ).build()
    }

}

enum class ComparisonExpression(val value: String) {
    EQUAL("_eq"),
    NOT_EQUAL("_neq"),
    GREATER_THAN("_gt"),
    GREATER_THAN_OR_EQUAL("_gte"),
    LESS_THAN("_lt"),
    LESS_THAN_OR_EQUAL("_lte"),
    IN("_in"),
    NOT_IN("_not_in");
//    LIKE("_like"),
//    ILIKE("_ilike");

    companion object {
        fun fromValue(value: String): ComparisonExpression {
            return values().firstOrNull { it.value == value } ?: throw IllegalArgumentException("Unknown value: $value")
        }
    }
}

fun graphqlObjectValueToBuilderLiteralValue(builder: RelBuilder, value: Value<*>): RexLiteral {
    return when (value) {
        is StringValue -> builder.literal(value.value as String)
        is IntValue -> builder.literal(value.value)
        is FloatValue -> builder.literal(value.value)
        is BooleanValue -> builder.literal(value.isValue)
        is NullValue -> builder.literal(null)
        else -> throw IllegalArgumentException("Unsupported value type: ${value.javaClass}")
    }
}


fun gqlWherePredicatesToRexNodePredicates(
    builder: RelBuilder,
    gqlNode: ObjectValue
): List<RexNode> {
    return gqlNode.objectFields.map {
        return when (it.name) {
            "_and" -> {
                val andPredicates = it.value as? ArrayValue
                andPredicates?.values?.map { innerValue ->
                    builder.and(
                        gqlWherePredicatesToRexNodePredicates(
                            builder,
                            innerValue as ObjectValue
                        )
                    )
                } ?: emptyList()
            }
            "_or" -> {
                val orPredicates = it.value as? ArrayValue
                orPredicates?.values?.map { innerValue ->
                    builder.or(
                        gqlWherePredicatesToRexNodePredicates(
                            builder,
                            innerValue as ObjectValue
                        )
                    )
                } ?: emptyList()
            }
            "_not" -> {
                val notPredicate = it.value as? ObjectValue
                listOf(
                    builder.not(
                        gqlWherePredicatesToRexNodePredicates(
                            builder,
                            notPredicate!!
                        ).get(0)
                    )
                )
            }
            else -> {
                val property = it.name
                val firstField: ObjectField? = (it.value as? ObjectValue)?.objectFields?.first()
                val predicate = firstField?.name?.let(ComparisonExpression::fromValue)
                val value = firstField?.value ?: throw IllegalArgumentException("Value is null")
                return when (predicate) {
                    ComparisonExpression.EQUAL -> {
                        listOf(
                            builder.equals(
                                builder.field(property),
                                graphqlObjectValueToBuilderLiteralValue(builder, value)
                            )
                        )
                    }
                    ComparisonExpression.NOT_EQUAL -> {
                        listOf(
                            builder.notEquals(
                                builder.field(property),
                                graphqlObjectValueToBuilderLiteralValue(builder, value)
                            )
                        )
                    }
                    ComparisonExpression.GREATER_THAN -> {
                        listOf(
                            builder.greaterThan(
                                builder.field(property),
                                graphqlObjectValueToBuilderLiteralValue(builder, value)
                            )
                        )
                    }
                    ComparisonExpression.GREATER_THAN_OR_EQUAL -> {
                        listOf(
                            builder.greaterThanOrEqual(
                                builder.field(property),
                                graphqlObjectValueToBuilderLiteralValue(builder, value)
                            )
                        )
                    }
                    ComparisonExpression.LESS_THAN -> {
                        listOf(
                            builder.lessThan(
                                builder.field(property),
                                graphqlObjectValueToBuilderLiteralValue(builder, value)
                            )
                        )
                    }
                    ComparisonExpression.LESS_THAN_OR_EQUAL -> {
                        listOf(
                            builder.lessThanOrEqual(
                                builder.field(property),
                                graphqlObjectValueToBuilderLiteralValue(builder, value)
                            )
                        )
                    }
                    ComparisonExpression.IN -> {
                        listOf(
                            builder.`in`(
                                builder.field(property),
                                graphqlObjectValueToBuilderLiteralValue(builder, value)
                            )
                        )
                    }
                    ComparisonExpression.NOT_IN -> {
                        listOf(
                            builder.not(
                                builder.`in`(
                                    builder.field(property),
                                    graphqlObjectValueToBuilderLiteralValue(
                                        builder,
                                        value
                                    )
                                )
                            )
                        )
                    }
                    else -> {
                        throw IllegalArgumentException("Unsupported comparison expression")
                    }
                }
            }
        }
    }
}

fun schemaPlusToRelOptSchema(schema: SchemaPlus): RelOptSchema {
    val dataTypeFactory = JavaTypeFactoryImpl()
    return object : RelOptSchema {
        override fun getTableForMember(names: MutableList<String>): RelOptTable? {
            val fullyQualifiedTableName = names.joinToString(".")
            val table = schema.getTable(fullyQualifiedTableName)
            return if (table != null) {
                RelOptTableImpl.create(this, table.getRowType(dataTypeFactory), table, ImmutableList.copyOf(names))
            } else {
                null
            }
        }

        override fun getTypeFactory(): RelDataTypeFactory {
            return JavaTypeFactoryImpl()
        }

        override fun registerRules(planner: RelOptPlanner?) {

        }
    }
}

object ExampleKotlin3 {

    private const val graphqlQuery = """
            query {
                users(
                    where: {
                        _and: [
                            { account_confirmed: { _eq: true } }
                            {
                                _or: [
                                    { balance: { _gt: 100 } },
                                    { balance: { _lt: 200 } }
                                ]
                            }
                        ],
                        _or: [
                            { id: { _eq: 1 } },
                            { name: { _eq: "John" } }
                        ]
                    }
                ) {
                        id
                        email
                        name
                        account_confirmed
                        balance
                }
            }
        """

    @JvmStatic
    fun main(args: Array<String>) {
        val rootSchema = Frameworks.createRootSchema(true)
        println("rootSchema: $rootSchema")
        rootSchema.add("users", object : AbstractTable() {
            override fun getRowType(typeFactory: RelDataTypeFactory): RelDataType {
                return typeFactory.builder()
                    .add("id", SqlTypeName.INTEGER)
                    .add("email", SqlTypeName.VARCHAR)
                    .add("name", SqlTypeName.VARCHAR)
                    .add("account_confirmed", SqlTypeName.BOOLEAN)
                    .add("balance", SqlTypeName.DOUBLE)
                    .build()
            }
        })
        val graphqlSchema: GraphQLSchema = CalciteGraphQLUtils.calciteSchemaToGraphQLSchema(rootSchema)
        println("graphqlSchema: " + SchemaPrinter().print(graphqlSchema))

        val graphqlVariables = HashMap<String, Any>()
        val queryDocument = ParseAndValidate.parse(ExecutionInput.newExecutionInput(graphqlQuery).build())

        val topLevelQueryNodes = queryDocument.document.definitions.filter {
            (it is OperationDefinition) && (it.operation.equals(OperationDefinition.Operation.QUERY))
        } as List<OperationDefinition>

        val planner = VolcanoPlanner()
        planner.setTopDownOpt(false)
        planner.addRelTraitDef(ConventionTraitDef.INSTANCE)
        planner.addRelTraitDef(RelCollationTraitDef.INSTANCE)
        RelOptUtil.registerDefaultRules(planner, false, false)

        val viz = RuleMatchVisualizer()
        viz.attachTo(planner)

        val relBuilder: RelBuilder = RelFactories.LOGICAL_BUILDER.create(
            RelOptCluster.create(planner, RexBuilder(JavaTypeFactoryImpl())),
            schemaPlusToRelOptSchema(rootSchema)
        )

        for (node in topLevelQueryNodes) {
            for (selection in node.selectionSet.selections.filterIsInstance<Field>()) {
                val whereArgument: Argument? = selection.arguments.find { it.name == "where" }
                if (whereArgument != null) {
                    val whereArgumentValue = whereArgument.value as? ObjectValue
                    val whereArgumentFields: MutableList<ObjectField>? = whereArgumentValue?.objectFields

                    if (whereArgumentValue != null) {
                        relBuilder
                            .scan(selection.name)
                            .filter(
                                gqlWherePredicatesToRexNodePredicates(
                                    relBuilder,
                                    whereArgumentValue
                                )
                            )
                            .project(
                                relBuilder.field("email"),
                                relBuilder.field("name"),
                            )

                        println("RelBuilder result: ")
                        println(relBuilder.toString())

                        val relRoot: RelNode = relBuilder.build()
                        println("relRoot:")
                        println(relRoot.toString())

                        planner.root = relRoot
                        println(viz.jsonStringResult)

                        val planned = planner.findBestExp()
                        println("Planner result: ")
                        println(planned)
                    }

                }
            }
        }

    }
}