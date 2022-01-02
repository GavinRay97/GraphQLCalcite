import extensions.queryOperations
import extensions.toGraphQLSchema
import graphql.ExecutionInput
import graphql.ParseAndValidate
import graphql.schema.idl.SchemaPrinter
import org.apache.calcite.adapter.enumerable.EnumerableConvention
import org.apache.calcite.jdbc.CalciteConnection
import org.apache.calcite.plan.RelOptUtil
import org.apache.calcite.plan.visualizer.RuleMatchVisualizer
import org.apache.calcite.rel.RelNode
import org.apache.calcite.rel.rel2sql.RelToSqlConverter
import org.apache.calcite.sql.SqlExplainFormat
import org.apache.calcite.sql.SqlExplainLevel
import org.apache.calcite.sql.dialect.CalciteSqlDialect
import org.apache.calcite.sql.parser.SqlParser
import org.apache.calcite.test.CalciteAssert
import org.apache.calcite.tools.Frameworks
import org.apache.calcite.tools.RelBuilder
import org.apache.calcite.tools.RelRunner
import org.apache.calcite.util.TestUtil

object Application {

    @JvmStatic
    fun main(args: Array<String>) {
        runGraphQLQueryOnSchemaSpec(
            CalciteAssert.SchemaSpec.JDBC_SCOTT,
            """
        query {
          EMP(
            limit: 2,
            offset: 1,
            where: {
              _or: [
                { DEPTNO: { _eq: 20 } },
                { DEPTNO: { _eq: 30 } }
              ]
              _and: [
                { SAL: { _gte: 1500 } }
                {
                    _or: [
                        { JOB: { _eq: "SALESMAN" } },
                        { JOB: { _eq: "MANAGER" } }
                    ]
                }
              ]
            }
          ) {
            EMPNO
            ENAME
            JOB
            MGR
            HIREDATE
            SAL
            COMM
            DEPTNO
          }
        }
            """.trimIndent()
        )
    }

    // Uses one of the Calcite test-data schemas from the CalciteAssert.SchemaSpec enum to run a GraphQL query
    private fun runGraphQLQueryOnSchemaSpec(schemaSpec: CalciteAssert.SchemaSpec, query: String) {
        CalciteAssert.that()
            .with(schemaSpec)
            .doWithConnection { connection: CalciteConnection ->
                try {
                    val config = Frameworks.newConfigBuilder()
                        .defaultSchema(connection.rootSchema)
                        .parserConfig(
                            // Need to set case-sensitive to false, or else it tries to look up capitalized table names and fails
                            // IE: "EMPS" instead of "emps"
                            SqlParser
                                .config()
                                .withCaseSensitive(false)
                        )
                        .build()

                    val dialect = CalciteSqlDialect.DEFAULT

                    val relBuilder = RelBuilder.create(config)
                    val runner = connection.unwrap(RelRunner::class.java)

                    val graphqlSchema = connection.rootSchema.getSubSchema(schemaSpec.schemaName)?.toGraphQLSchema()
                    requireNotNull(graphqlSchema) { "Could not create GraphQL schema" }
                    println("graphqlSchema: " + SchemaPrinter().print(graphqlSchema))

                    val queryParseResult = ParseAndValidate.parse(
                        ExecutionInput.newExecutionInput(query).build()
                    )

                    val viz = RuleMatchVisualizer("./calcite-plan-visualization", "-" + schemaSpec.schemaName)
                    viz.attachTo(relBuilder.cluster.planner)

                    for (operation in queryParseResult.document.queryOperations()) {
                        for (tableQuery in tableQueryFromGQLQueryOperation(operation)) {
                            println(tableQuery.prettyPrint())

                            val relRoot: RelNode = tableQuery.toRelNode(relBuilder)
                            val sqlNode = RelToSqlConverter(dialect).visitRoot(relRoot).asStatement()
                            println("RelToSqlConverter result: ")
                            println(sqlNode.toSqlString(dialect))
                            println()
                            println(
                                RelOptUtil.dumpPlan(
                                    "-- Logical Plan", relRoot, SqlExplainFormat.TEXT,
                                    SqlExplainLevel.DIGEST_ATTRIBUTES
                                )
                            )

                            val cluster = relRoot.cluster
                            val desiredTraits = cluster.traitSet().replace(EnumerableConvention.INSTANCE)
                            val newRoot = cluster.planner.changeTraits(relRoot, desiredTraits)
                            println(
                                RelOptUtil.dumpPlan(
                                    "-- Mid Plan", newRoot, SqlExplainFormat.TEXT,
                                    SqlExplainLevel.DIGEST_ATTRIBUTES
                                )
                            )

                            cluster.planner.root = newRoot
                            val bestExp = cluster.planner.findBestExp()
                            println(
                                RelOptUtil.dumpPlan(
                                    "-- Best Plan", bestExp, SqlExplainFormat.TEXT,
                                    SqlExplainLevel.DIGEST_ATTRIBUTES
                                )
                            )

                            // Can dump visualizer representation only after calling "findBestExp()"
                            // println(viz.jsonStringResult)

                            runner.prepareStatement(bestExp).use {
                                println("ResultSet:")
                                val resultSet = it.executeQuery()
                                while (resultSet.next()) {
                                    for (i in 1..resultSet.metaData.columnCount) {
                                        print(resultSet.getObject(i).toString() + ",")
                                    }
                                    println()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    throw TestUtil.rethrow<RuntimeException>(e)
                }
            }
    }
}
