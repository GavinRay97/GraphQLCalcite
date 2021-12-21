//package com.example.scala_version
//
//import graphql.Scalars.*
//import graphql.analysis.*
//import graphql.language.*
//import graphql.schema.*
//import graphql.schema.idl.SchemaPrinter
//import graphql.util.TraversalControl
//import graphql.{ExecutionInput, ParseAndValidate, ParseAndValidateResult}
//import org.apache.calcite.jdbc.JavaTypeFactoryImpl
//import org.apache.calcite.plan.RelOptCluster
//import org.apache.calcite.plan.volcano.VolcanoPlanner
//import org.apache.calcite.rel.RelNode
//import org.apache.calcite.rel.`type`.{RelDataType, RelDataTypeFactory, RelDataTypeField}
//import org.apache.calcite.rel.core.RelFactories
//import org.apache.calcite.rel.rel2sql.SqlImplementor.SimpleContext
//import org.apache.calcite.rel.rel2sql.{RelToSqlConverter, SqlImplementor}
//import org.apache.calcite.rex.{RexBuilder, RexNode}
//import org.apache.calcite.runtime.CalciteResource
//import org.apache.calcite.schema.impl.AbstractTable
//import org.apache.calcite.schema.{Schema, SchemaPlus, Table}
//import org.apache.calcite.sql.`type`.SqlTypeName
//import org.apache.calcite.sql.`type`.SqlTypeName.*
//import org.apache.calcite.sql.dialect.PostgresqlSqlDialect
//import org.apache.calcite.sql.pretty.SqlPrettyWriter
//import org.apache.calcite.sql.{SqlDialect, SqlNode}
//import org.apache.calcite.tools.{FrameworkConfig, Frameworks, RelBuilder}
//
//import java.util
//import java.util.Objects
//import scala.collection.mutable
//import scala.jdk.CollectionConverters.*
//import scala.jdk.StreamConverters.*
//
//object BaseGraphQLTypes {
//    final class ComparisonExpressionInputType(
//                                                 val `type`: GraphQLScalarType,
//                                                 val name: String,
//                                                 val description: String
//                                             ) {
//        def build: GraphQLInputObjectType =
//            GraphQLInputObjectType.newInputObject
//                .name(name)
//                .description(description)
//                .field(GraphQLInputObjectField.newInputObjectField.name("_is_null").`type`(GraphQLBoolean))
//                .field(GraphQLInputObjectField.newInputObjectField.name("_eq").`type`(`type`))
//                .field(GraphQLInputObjectField.newInputObjectField.name("_neq").`type`(`type`))
//                .field(GraphQLInputObjectField.newInputObjectField.name("_gt").`type`(`type`))
//                .field(GraphQLInputObjectField.newInputObjectField.name("_gte").`type`(`type`))
//                .field(GraphQLInputObjectField.newInputObjectField.name("_lt").`type`(`type`))
//                .field(GraphQLInputObjectField.newInputObjectField.name("_lte").`type`(`type`))
//                .field(
//                    GraphQLInputObjectField.newInputObjectField
//                        .name("_in")
//                        .`type`(new GraphQLList(new GraphQLNonNull(`type`)))
//                )
//                .field(
//                    GraphQLInputObjectField.newInputObjectField
//                        .name("_nin")
//                        .`type`(new GraphQLList(new GraphQLNonNull(`type`)))
//                )
//                .build
//    }
//
//    val IntComparisonExpressionType: GraphQLInputObjectType = new BaseGraphQLTypes.ComparisonExpressionInputType(
//        GraphQLInt,
//        "Int_comparison_exp",
//        "Boolean expression to compare columns of type \"Int\". All fields are combined with logical 'AND'."
//    ).build
//    val FloatComparisonExpressionType: GraphQLInputObjectType = new BaseGraphQLTypes.ComparisonExpressionInputType(
//        GraphQLFloat,
//        "Float_comparison_exp",
//        "Boolean expression to compare columns of type \"Float\". All fields are combined with logical 'AND'."
//    ).build
//    val BooleanComparisonExpressionType: GraphQLInputObjectType = new BaseGraphQLTypes.ComparisonExpressionInputType(
//        GraphQLBoolean,
//        "Boolean_comparison_exp",
//        "Boolean expression to compare columns of type \"Boolean\". All fields are combined with logical 'AND'."
//    ).build
//    val StringComparisonExpressionType: GraphQLInputObjectType = new BaseGraphQLTypes.ComparisonExpressionInputType(
//        GraphQLString,
//        "String_comparison_exp",
//        "Boolean expression to compare columns of type \"String\". All fields are combined with logical 'AND'."
//    ).build.transform { (t: GraphQLInputObjectType.Builder) =>
//        t.field(
//            GraphQLInputObjectField.newInputObjectField
//                .name("_ilike")
//                .description("does the column match the given case-insensitive pattern")
//                .`type`(GraphQLString)
//        )
//        t.field(
//            GraphQLInputObjectField.newInputObjectField
//                .name("_iregex")
//                .description("does the column match the given POSIX regular expression, case insensitive")
//                .`type`(GraphQLString)
//        )
//        t.field(
//            GraphQLInputObjectField.newInputObjectField
//                .name("_like")
//                .description("does the column match the given pattern")
//                .`type`(GraphQLString)
//        )
//        t.field(
//            GraphQLInputObjectField.newInputObjectField
//                .name("_nilike")
//                .description("does the column NOT match the given case-insensitive pattern")
//                .`type`(GraphQLString)
//        )
//        t.field(
//            GraphQLInputObjectField.newInputObjectField
//                .name("_niregex")
//                .description("does the column NOT match the given POSIX regular expression, case insensitive")
//                .`type`(GraphQLString)
//        )
//        t.field(
//            GraphQLInputObjectField.newInputObjectField
//                .name("_nlike")
//                .description("does the column NOT match the given pattern")
//                .`type`(GraphQLString)
//        )
//        t.field(
//            GraphQLInputObjectField.newInputObjectField
//                .name("_nregex")
//                .description("does the column NOT match the given POSIX regular expression, case sensitive")
//                .`type`(GraphQLString)
//        )
//        t.field(
//            GraphQLInputObjectField.newInputObjectField
//                .name("_nsimilar")
//                .description("does the column NOT match the given SQL regular expression")
//                .`type`(GraphQLString)
//        )
//        t.field(
//            GraphQLInputObjectField.newInputObjectField
//                .name("_regex")
//                .description("does the column match the given POSIX regular expression, case sensitive")
//                .`type`(GraphQLString)
//        )
//        t.field(
//            GraphQLInputObjectField.newInputObjectField
//                .name("_similar")
//                .description("does the column match the given SQL regular expression")
//                .`type`(GraphQLString)
//        )
//    }
//
//}
//
//object CalciteAdapterGraphQLSchemaGenerator {
//    def relDataTypeToGraphQLType(`type`: RelDataType): GraphQLType = `type`.getSqlTypeName match {
//        case BOOLEAN => GraphQLBoolean
//        case TINYINT | SMALLINT | INTEGER | BIGINT => GraphQLInt
//        case DECIMAL | FLOAT | REAL | DOUBLE => GraphQLFloat
//        case DATE => GraphQLString
//        case TIME => GraphQLString
//        case TIME_WITH_LOCAL_TIME_ZONE => GraphQLString
//        case TIMESTAMP => GraphQLString
//        case TIMESTAMP_WITH_LOCAL_TIME_ZONE => GraphQLString
//        case INTERVAL_YEAR => GraphQLInt
//        case INTERVAL_YEAR_MONTH => GraphQLInt
//        case INTERVAL_MONTH => GraphQLInt
//        case INTERVAL_DAY => GraphQLInt
//        case INTERVAL_DAY_HOUR => GraphQLInt
//        case INTERVAL_DAY_MINUTE => GraphQLInt
//        case INTERVAL_DAY_SECOND => GraphQLInt
//        case INTERVAL_HOUR => GraphQLInt
//        case INTERVAL_HOUR_MINUTE => GraphQLInt
//        case INTERVAL_HOUR_SECOND => GraphQLInt
//        case INTERVAL_MINUTE => GraphQLInt
//        case INTERVAL_MINUTE_SECOND => GraphQLInt
//        case INTERVAL_SECOND => GraphQLInt
//        case CHAR => GraphQLString
//        case VARCHAR => GraphQLString
//        case BINARY => GraphQLString
//        case VARBINARY => GraphQLString
//        case NULL => GraphQLString
//        case ANY => GraphQLString
//        case SYMBOL => GraphQLString
//        case MULTISET => GraphQLString
//        case ARRAY => GraphQLString
//        case MAP => GraphQLString
//        case DISTINCT => GraphQLString
//        case STRUCTURED => GraphQLString
//        case ROW => GraphQLString
//        case OTHER => GraphQLString
//        case CURSOR => GraphQLString
//        case COLUMN_LIST => GraphQLString
//        case DYNAMIC_STAR => GraphQLString
//        case GEOMETRY => GraphQLString
//        case SARG => GraphQLString
//    }
//
//    def relDataTypeToGraphQLComparisonExprType(`type`: RelDataType): GraphQLType = `type`.getSqlTypeName match {
//        case BOOLEAN => BaseGraphQLTypes.BooleanComparisonExpressionType
//        case TINYINT | SMALLINT | INTEGER | BIGINT => BaseGraphQLTypes.IntComparisonExpressionType
//        case DECIMAL | FLOAT | REAL | DOUBLE => BaseGraphQLTypes.FloatComparisonExpressionType
//        case DATE => BaseGraphQLTypes.StringComparisonExpressionType
//        case TIME => BaseGraphQLTypes.StringComparisonExpressionType
//        case TIME_WITH_LOCAL_TIME_ZONE => BaseGraphQLTypes.StringComparisonExpressionType
//        case TIMESTAMP => BaseGraphQLTypes.StringComparisonExpressionType
//        case TIMESTAMP_WITH_LOCAL_TIME_ZONE => BaseGraphQLTypes.StringComparisonExpressionType
//        // Not sure how to deal with these yet
//        case INTERVAL_YEAR => GraphQLInt
//        case INTERVAL_YEAR_MONTH => GraphQLInt
//        case INTERVAL_MONTH => GraphQLInt
//        case INTERVAL_DAY => GraphQLInt
//        case INTERVAL_DAY_HOUR => GraphQLInt
//        case INTERVAL_DAY_MINUTE => GraphQLInt
//        case INTERVAL_DAY_SECOND => GraphQLInt
//        case INTERVAL_HOUR => GraphQLInt
//        case INTERVAL_HOUR_MINUTE => GraphQLInt
//        case INTERVAL_HOUR_SECOND => GraphQLInt
//        case INTERVAL_MINUTE => GraphQLInt
//        case INTERVAL_MINUTE_SECOND => GraphQLInt
//        case INTERVAL_SECOND => GraphQLInt
//        case CHAR => BaseGraphQLTypes.StringComparisonExpressionType
//        case VARCHAR => BaseGraphQLTypes.StringComparisonExpressionType
//        case BINARY => BaseGraphQLTypes.StringComparisonExpressionType
//        case VARBINARY => BaseGraphQLTypes.StringComparisonExpressionType
//        case NULL => GraphQLString
//        case ANY => GraphQLString
//        case SYMBOL => GraphQLString
//        case MULTISET => GraphQLString
//        case ARRAY => GraphQLString
//        case MAP => GraphQLString
//        case DISTINCT => GraphQLString
//        case STRUCTURED => GraphQLString
//        case ROW => GraphQLString
//        case OTHER => GraphQLString
//        case CURSOR => GraphQLString
//        case COLUMN_LIST => GraphQLString
//        case DYNAMIC_STAR => GraphQLString
//        case GEOMETRY => GraphQLString
//        case SARG => GraphQLString
//    }
//
//    def calciteSchemaToGraphQLSchema(calciteSchema: Schema): GraphQLSchema =
//        GraphQLSchema.newSchema
//            .query(
//                GraphQLObjectType.newObject
//                    .name("Query")
//                    .fields(
//                        calciteSchema.getTableNames.stream
//                            .map((tableName: String) =>
//                                GraphQLFieldDefinition.newFieldDefinition
//                                    .name(tableName)
//                                    .`type`(
//                                        GraphQLObjectType.newObject
//                                            .name(tableName)
//                                            .fields(
//                                                Objects
//                                                    .requireNonNull(calciteSchema.getTable(tableName))
//                                                    .getRowType(new JavaTypeFactoryImpl)
//                                                    .getFieldList
//                                                    .stream
//                                                    .map((field: RelDataTypeField) =>
//                                                        GraphQLFieldDefinition.newFieldDefinition
//                                                            .name(field.getName)
//                                                            .`type`(
//                                                                relDataTypeToGraphQLType(field.getType)
//                                                                    .asInstanceOf[GraphQLOutputType]
//                                                            )
//                                                            .build
//                                                    )
//                                                    .toList
//                                            )
//                                    )
//                                    .arguments(
//                                        util.List.of(
//                                            GraphQLArgument.newArgument.name("limit").`type`(GraphQLInt).build,
//                                            GraphQLArgument.newArgument.name("offset").`type`(GraphQLInt).build,
//                                            GraphQLArgument.newArgument
//                                                .name("order_by")
//                                                .`type`(GraphQLList.list(GraphQLString))
//                                                .build,
//                                            GraphQLArgument.newArgument
//                                                .name("where")
//                                                .`type`(
//                                                    GraphQLInputObjectType.newInputObject
//                                                        .name(tableName + "_bool_exp")
//                                                        .fields(
//                                                            Objects
//                                                                .requireNonNull(calciteSchema.getTable(tableName))
//                                                                .getRowType(new JavaTypeFactoryImpl)
//                                                                .getFieldList
//                                                                .stream
//                                                                .map((field1: RelDataTypeField) =>
//                                                                    GraphQLInputObjectField.newInputObjectField
//                                                                        .name(field1.getName)
//                                                                        .`type`(
//                                                                            relDataTypeToGraphQLComparisonExprType(field1.getType)
//                                                                                .asInstanceOf[GraphQLInputType]
//                                                                        )
//                                                                        .build
//                                                                )
//                                                                .toList
//                                                        )
//                                                        .field(
//                                                            GraphQLInputObjectField.newInputObjectField
//                                                                .name("_and")
//                                                                .`type`(
//                                                                    GraphQLList.list(
//                                                                        new GraphQLNonNull(
//                                                                            GraphQLTypeReference.typeRef(tableName + "_bool_exp")
//                                                                        )
//                                                                    )
//                                                                )
//                                                        )
//                                                        .field(
//                                                            GraphQLInputObjectField.newInputObjectField
//                                                                .name("_or")
//                                                                .`type`(
//                                                                    GraphQLList.list(
//                                                                        new GraphQLNonNull(
//                                                                            GraphQLTypeReference.typeRef(tableName + "_bool_exp")
//                                                                        )
//                                                                    )
//                                                                )
//                                                        )
//                                                        .field(
//                                                            GraphQLInputObjectField.newInputObjectField
//                                                                .name("_not")
//                                                                .`type`(GraphQLTypeReference.typeRef(tableName + "_bool_exp"))
//                                                                .build
//                                                        )
//                                                        .build
//                                                )
//                                                .build
//                                        )
//                                    )
//                                    .build
//                            )
//                            .toList
//                    )
//            )
//            .build
//}
//
//object ExampleScala extends App {
//
//    val rootSchema: SchemaPlus = Frameworks.createRootSchema(true)
//    rootSchema.add(
//        "users",
//        new AbstractTable() {
//            override def getRowType(typeFactory: RelDataTypeFactory): RelDataType = typeFactory.builder
//                .add("id", SqlTypeName.INTEGER)
//                .add("email", SqlTypeName.VARCHAR)
//                .add("name", SqlTypeName.VARCHAR)
//                .add("account_confirmed", SqlTypeName.BOOLEAN)
//                .add("balance", SqlTypeName.DOUBLE)
//                .build
//        }
//    )
//
//    val graphqlSchema: GraphQLSchema = CalciteAdapterGraphQLSchemaGenerator.calciteSchemaToGraphQLSchema(rootSchema)
//    println(SchemaPrinter().print(graphqlSchema))
//
//    val graphqlQuery =
//        """
//            query {
//                users(
//                    where: {
//                        _and: [
//                            { account_confirmed: { _eq: true } }
//                            {
//                                _or: [
//                                    { balance: { _gt: 100 } },
//                                    { balance: { _lt: 200 } }
//                                ]
//                            }
//                        ],
//                        _or: [
//                            { id: { _eq: 1 } },
//                            { name: { _eq: "John" } }
//                        ]
//                    }
//                ) {
//                        id
//                        email
//                        name
//                        account_confirmed
//                        balance
//                }
//            }
//        """
//    val graphqlVariables: Map[String, Any] = Map.empty
//    val query: ParseAndValidateResult = ParseAndValidate.parse(ExecutionInput.newExecutionInput(graphqlQuery).build)
//    val queryDocument: Document = query.getDocument
//
//    import collection.convert.ImplicitConversions.*
//
//    def processGraphQLQuery(queryDocument: OperationDefinition, relBuilder: RelBuilder): RelBuilder = {
//        for topLevelQueryNode <- queryDocument.getSelectionSet.getSelections.asScala do
//            val whereNode: Option[Argument] =
//                topLevelQueryNode.asInstanceOf[Field].getArguments.asScala.find(_.getName.equals("where"))
//                relBuilder.not()
//
//        relBuilder
//    }
//
//    queryDocument.getDefinitions.asScala.foreach { it =>
//        println(s"Definition: $it")
//        it.getChildren.asScala.foreach(it => println(s"Child: $it"))
//    }
//}
//
