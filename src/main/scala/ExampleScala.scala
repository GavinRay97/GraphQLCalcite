//
//import graphql.Scalars.*
//import graphql.analysis.QueryVisitor
//import graphql.language.{Document, Field, OperationDefinition}
//import graphql.schema as GraphQLSchema
//import graphql.schema.idl.SchemaPrinter
//import graphql.schema.{GraphQLArgument, GraphQLInputObjectType, GraphQLList, GraphQLNonNull}
//import org.apache.calcite.jdbc.JavaTypeFactoryImpl
//import org.apache.calcite.rel.RelNode
//import org.apache.calcite.rel.`type`.{RelDataType, RelDataTypeFactory, RelDataTypeField}
//import org.apache.calcite.rel.rel2sql.SqlImplementor.SimpleContext
//import org.apache.calcite.rel.rel2sql.{RelToSqlConverter, SqlImplementor}
//import org.apache.calcite.rex.RexNode
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
//
//import graphql.ExecutionInput
//import graphql.ParseAndValidate
//import graphql.analysis._
//import graphql.language._
//import graphql.schema._
//import graphql.schema.idl.SchemaPrinter
//import graphql.util.TraversalControl
//import org.apache.calcite.jdbc.JavaTypeFactoryImpl
//import org.apache.calcite.plan.RelOptCluster
//import org.apache.calcite.plan.volcano.VolcanoPlanner
//import org.apache.calcite.rel.RelNode
//import org.apache.calcite.rel.core.RelFactories
//
//
//
//
//import org.apache.calcite.rex.RexBuilder
//import org.apache.calcite.schema.Schema
//import org.apache.calcite.schema.SchemaPlus
//import org.apache.calcite.schema.impl.AbstractTable
//
//
//
//import org.apache.calcite.tools.Frameworks
//import org.apache.calcite.tools.RelBuilder
//import java.util
//import java.util.Objects
//import graphql.Scalars._
//
//
//import scala.collection.mutable
//import scala.jdk.CollectionConverters.*
//import scala.jdk.StreamConverters.*
//
//def ComparisonExpressionInputType(
//    name: String,
//    t: GraphQLSchema.GraphQLScalarType,
//    description: String = ""
//): GraphQLSchema.GraphQLInputObjectType =
//    GraphQLInputObject(
//      name,
//      List(
//        GraphQLInputObjectField("_is_null", GraphQLBoolean),
//        GraphQLInputObjectField("_eq", t),
//        GraphQLInputObjectField("_neq", t),
//        GraphQLInputObjectField("_gt", t),
//        GraphQLInputObjectField("_gte", t),
//        GraphQLInputObjectField("_lt", t),
//        GraphQLInputObjectField("_lte", t),
//        GraphQLInputObjectField("_in", GraphQLList(GraphQLNonNull(t))),
//        GraphQLInputObjectField("_nin", GraphQLList(GraphQLNonNull(t)))
//      )
//    )
//
//object ComparisonExpressionInputTypes:
//    val StringType: GraphQLSchema.GraphQLInputObjectType =
//        ComparisonExpressionInputType("String_comparison_exp", GraphQLString)
//
//    val IntType: GraphQLSchema.GraphQLInputObjectType =
//        ComparisonExpressionInputType("Int_comparison_exp", GraphQLInt)
//
//    val FloatType: GraphQLSchema.GraphQLInputObjectType =
//        ComparisonExpressionInputType("Float_comparison_exp", GraphQLFloat)
//
//    val BooleanType: GraphQLSchema.GraphQLInputObjectType =
//        ComparisonExpressionInputType("Boolean_comparison_exp", GraphQLBoolean)
//
//case class RelDataGraphQLTypes(
//    scalarType: GraphQLSchema.GraphQLType,
//    comparisonExpressionType: GraphQLSchema.GraphQLType
//)
//
//def relDataTypeToGraphQLTypes(t: RelDataType): RelDataGraphQLTypes = t.getSqlTypeName match {
//    case BOOLEAN => RelDataGraphQLTypes(GraphQLBoolean, ComparisonExpressionInputTypes.BooleanType)
//    case TINYINT | SMALLINT | INTEGER | BIGINT =>
//        RelDataGraphQLTypes(GraphQLInt, ComparisonExpressionInputTypes.IntType)
//    case DECIMAL | FLOAT | REAL | DOUBLE => RelDataGraphQLTypes(GraphQLFloat, ComparisonExpressionInputTypes.FloatType)
//    case DATE                           => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case TIME                           => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case TIME_WITH_LOCAL_TIME_ZONE      => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case TIMESTAMP                      => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case TIMESTAMP_WITH_LOCAL_TIME_ZONE => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case INTERVAL_YEAR                  => RelDataGraphQLTypes(GraphQLInt, ComparisonExpressionInputTypes.IntType)
//    case INTERVAL_YEAR_MONTH            => RelDataGraphQLTypes(GraphQLInt, ComparisonExpressionInputTypes.IntType)
//    case INTERVAL_MONTH                 => RelDataGraphQLTypes(GraphQLInt, ComparisonExpressionInputTypes.IntType)
//    case INTERVAL_DAY                   => RelDataGraphQLTypes(GraphQLInt, ComparisonExpressionInputTypes.IntType)
//    case INTERVAL_DAY_HOUR              => RelDataGraphQLTypes(GraphQLInt, ComparisonExpressionInputTypes.IntType)
//    case INTERVAL_DAY_MINUTE            => RelDataGraphQLTypes(GraphQLInt, ComparisonExpressionInputTypes.IntType)
//    case INTERVAL_DAY_SECOND            => RelDataGraphQLTypes(GraphQLInt, ComparisonExpressionInputTypes.IntType)
//    case INTERVAL_HOUR                  => RelDataGraphQLTypes(GraphQLInt, ComparisonExpressionInputTypes.IntType)
//    case INTERVAL_HOUR_MINUTE           => RelDataGraphQLTypes(GraphQLInt, ComparisonExpressionInputTypes.IntType)
//    case INTERVAL_HOUR_SECOND           => RelDataGraphQLTypes(GraphQLInt, ComparisonExpressionInputTypes.IntType)
//    case INTERVAL_MINUTE                => RelDataGraphQLTypes(GraphQLInt, ComparisonExpressionInputTypes.IntType)
//    case INTERVAL_MINUTE_SECOND         => RelDataGraphQLTypes(GraphQLInt, ComparisonExpressionInputTypes.IntType)
//    case INTERVAL_SECOND                => RelDataGraphQLTypes(GraphQLInt, ComparisonExpressionInputTypes.IntType)
//    case CHAR                           => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case VARCHAR                        => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case BINARY                         => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case VARBINARY                      => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case NULL                           => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case ANY                            => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case SYMBOL                         => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case MULTISET                       => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case ARRAY                          => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case MAP                            => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case DISTINCT                       => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case STRUCTURED                     => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case ROW                            => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case OTHER                          => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case CURSOR                         => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case COLUMN_LIST                    => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case DYNAMIC_STAR                   => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case GEOMETRY                       => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//    case SARG                           => RelDataGraphQLTypes(GraphQLString, ComparisonExpressionInputTypes.StringType)
//}
//
//object GraphQLField:
//    def apply(
//        name: String,
//        `type`: GraphQLSchema.GraphQLOutputType,
//        args: Seq[GraphQLSchema.GraphQLArgument] = Seq.empty,
//        description: String = ""
//    ): GraphQLSchema.GraphQLFieldDefinition =
//        GraphQLSchema.GraphQLFieldDefinition
//            .newFieldDefinition()
//            .name(name)
//            .`type`(`type`)
//            .description(description)
//            .arguments(args.asJava)
//            .build()
//
//object GraphQLObject:
//    def apply(
//        name: String,
//        fields: Seq[GraphQLSchema.GraphQLFieldDefinition] = Seq.empty,
//        description: String = "",
//        interfaces: Seq[GraphQLSchema.GraphQLInterfaceType] = Seq.empty
//    ): GraphQLSchema.GraphQLObjectType =
//        GraphQLSchema.GraphQLObjectType
//            .newObject()
//            .name(name)
//            .description(description)
//            .fields(fields.asJava)
//            .replaceInterfaces(interfaces.asJava)
//            .build()
//
//object GraphQLInputObject:
//    def apply(
//        name: String,
//        fields: Seq[GraphQLSchema.GraphQLInputObjectField] = Seq.empty,
//        description: String = "",
//        directives: Seq[GraphQLSchema.GraphQLDirective] = Seq.empty
//    ): GraphQLSchema.GraphQLInputObjectType =
//        GraphQLSchema.GraphQLInputObjectType
//            .newInputObject()
//            .name(name)
//            .description(description)
//            .fields(fields.asJava)
//            .replaceDirectives(directives.asJava)
//            .build()
//
//object GraphQLInputObjectField:
//    def apply(
//        name: String,
//        `type`: GraphQLSchema.GraphQLInputType,
//        defaultValue: AnyRef = null,
//        description: String = "",
//        directives: Seq[GraphQLSchema.GraphQLDirective] = Seq.empty
//    ): GraphQLSchema.GraphQLInputObjectField =
//        GraphQLSchema.GraphQLInputObjectField
//            .newInputObjectField()
//            .name(name)
//            .`type`(`type`)
//            .description(description)
//            .defaultValue(defaultValue)
//            .replaceDirectives(directives.asJava)
//            .build()
//
//object GraphQLArgument:
//    def apply(
//        name: String,
//        `type`: GraphQLSchema.GraphQLInputType,
//        description: String = "",
//        defaultValue: AnyRef = null,
//        directives: Seq[GraphQLSchema.GraphQLDirective] = Seq.empty
//    ): GraphQLSchema.GraphQLArgument =
//        GraphQLSchema.GraphQLArgument
//            .newArgument()
//            .name(name)
//            .`type`(`type`)
//            .description(description)
//            .defaultValue(defaultValue)
//            .replaceDirectives(directives.asJava)
//            .build()
//
//def calciteSchemaToGraphQLQueryType(schema: Schema): GraphQLSchema.GraphQLObjectType =
//    GraphQLObject(
//      "Query",
//      schema.getTableNames.asScala.flatMap { tableName =>
//          val table: Table                             = schema.getTable(tableName)
//          val rowtype: RelDataType                     = table.getRowType(JavaTypeFactoryImpl())
//          val fieldlist: mutable.Seq[RelDataTypeField] = rowtype.getFieldList.asScala
//          fieldlist.map(field =>
//              GraphQLField(
//                tableName,
//                relDataTypeToGraphQLTypes(field.getType).scalarType
//                    .asInstanceOf[GraphQLSchema.GraphQLOutputType],
//                List(
//                  GraphQLArgument("limit", GraphQLInt),
//                  GraphQLArgument("offset", GraphQLInt),
//                  GraphQLArgument("orderBy", GraphQLList(GraphQLNonNull(GraphQLString))),
//                  GraphQLArgument(
//                    "where",
//                    GraphQLInputObject(
//                      s"${tableName}_bool_exp",
//                      (fieldlist
//                          .map(field =>
//                              GraphQLInputObjectField(
//                                field.getName,
//                                relDataTypeToGraphQLTypes(field.getType).comparisonExpressionType
//                                    .asInstanceOf[GraphQLSchema.GraphQLInputType]
//                              )
//                          ) ++ List(
//                        GraphQLInputObjectField(
//                          "_and",
//                          GraphQLList(
//                            GraphQLNonNull(
//                              GraphQLSchema.GraphQLTypeReference.typeRef(
//                                s"${tableName}_bool_exp"
//                              )
//                            )
//                          )
//                        ),
//                        GraphQLInputObjectField(
//                          "_or",
//                          GraphQLList(
//                            GraphQLNonNull(
//                              GraphQLSchema.GraphQLTypeReference.typeRef(
//                                s"${tableName}_bool_exp"
//                              )
//                            )
//                          )
//                        ),
//                        GraphQLInputObjectField(
//                          "_not",
//                          GraphQLSchema.GraphQLTypeReference.typeRef(
//                            s"${tableName}_bool_exp"
//                          )
//                        )
//                      )).toSeq
//                    )
//                  )
//                )
//              )
//          )
//      }.toSeq
//    )
//
//def graphqlQueryToCalciteRelNode(
//    schema: GraphQLSchema.GraphQLSchema,
//    query: Document,
//    variables: Map[String, Any] = Map.empty,
//    operationName: String = ""
//): RelNode = {
//    val relBuilder = RelBuilder.create(Frameworks.newConfigBuilder.build)
//    val root       = query.getDefinitions.asScala.head.asInstanceOf[OperationDefinition]
//    val queryType  = schema.getQueryType
//    val queryField = queryType.getFields.asScala
//        .find(_.getName == root.getSelectionSet.getSelections.get(0).asInstanceOf[Field].getName)
//        .get
//    queryField.getArguments.asScala.foreach { arg =>
//        val argName = arg.getName
//        if argName != "limit" && argName != "offset" && argName != "orderBy" && argName != "where" then
//            throw new RuntimeException(s"Unknown argument $argName")
//
//        val argValue = variables.get(argName)
//        if argValue.isEmpty then throw new IllegalArgumentException(s"Variable $argName is not defined")
//
//        if argName == "where" then
//            relBuilder.project(
//              relBuilder.field(argName),
//              relBuilder.literal(argValue.get)
//            )
//    }
//
//    val t = RelToSqlConverter(PostgresqlSqlDialect.DEFAULT)
//    val x = t.visit(relBuilder.build())
//    import org.apache.calcite.sql.SqlWriterConfig
//    import org.apache.calcite.sql.pretty.SqlPrettyWriter
//    val config = SqlPrettyWriter.config
//        .withLineFolding(SqlWriterConfig.LineFolding.STEP)
//        .withSelectFolding(SqlWriterConfig.LineFolding.TALL)
//        .withFromFolding(SqlWriterConfig.LineFolding.TALL)
//        .withWhereFolding(SqlWriterConfig.LineFolding.TALL)
//        .withHavingFolding(SqlWriterConfig.LineFolding.TALL)
//        .withIndentation(4)
//        .withClauseEndsLine(true)
//    System.out.println(SqlPrettyWriter(config).format(x.asQueryOrValues()))
//
//    relBuilder.build()
//}
//
//@main def main() =
//    val rootSchema: SchemaPlus = Frameworks.createRootSchema(true)
//    println("rootSchema: " + rootSchema)
//
//    val x = HrClusteredSchema()
//
//    rootSchema.add(
//      "users",
//      new AbstractTable() {
//          override def getRowType(typeFactory: RelDataTypeFactory): RelDataType = typeFactory.builder
//              .add("id", SqlTypeName.INTEGER)
//              .add("email", SqlTypeName.VARCHAR)
//              .add("name", SqlTypeName.VARCHAR)
//              .add("account_confirmed", SqlTypeName.BOOLEAN)
//              .add("balance", SqlTypeName.DOUBLE)
//              .build
//      }
//    )
//
//    val graphqlSchema: GraphQLSchema.GraphQLSchema = GraphQLSchema.GraphQLSchema
//        .newSchema()
//        .query(calciteSchemaToGraphQLQueryType(x))
//        .build
//
//    println(SchemaPrinter().print(graphqlSchema))
