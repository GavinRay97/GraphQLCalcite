import org.apache.calcite.adapter.enumerable.EnumerableConvention
import org.apache.calcite.plan.RelOptUtil
import org.apache.calcite.sql.SqlExplainFormat
import org.apache.calcite.sql.SqlExplainLevel
import org.apache.calcite.tools.FrameworkConfig
import org.apache.calcite.tools.Frameworks

object CalciteUtils {

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
}

