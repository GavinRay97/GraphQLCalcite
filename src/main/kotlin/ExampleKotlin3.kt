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
import org.apache.calcite.sql.type.SqlTypeFamily
import org.apache.calcite.sql.type.SqlTypeName
import org.apache.calcite.tools.Frameworks
import org.apache.calcite.tools.RelBuilder


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

    val StringComparisonExpressionType = mkComparisonExpressionInputType(
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

    fun mkComparisonExpressionInputType(
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

    val graphqlTypeToComparsionExpressionTypeMap: Map<Class<out GraphQLScalarType>, GraphQLInputObjectType> = mapOf(
        Scalars.GraphQLString::class.java to StringComparisonExpressionType,
        Scalars.GraphQLInt::class.java to IntComparisonExpressionType,
        Scalars.GraphQLFloat::class.java to FloatComparisonExpressionType,
        Scalars.GraphQLBoolean::class.java to BooleanComparisonExpressionType,
    )
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

    private fun graphqlObjectValueToBuilderLiteralValue(builder: RelBuilder, value: Value<*>): RexLiteral {
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

    fun calciteSchemaToGraphQLSchema(calciteSchema: Schema): GraphQLSchema {
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

        override fun registerRules(planner: RelOptPlanner) {

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

        val topLevelQueryNodes = queryDocument.document.definitions.filter {
            (it is OperationDefinition) && (it.operation.equals(OperationDefinition.Operation.QUERY))
        } as List<OperationDefinition>

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
                                CalciteGraphQLUtils.gqlWherePredicatesToRexNodePredicates(
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