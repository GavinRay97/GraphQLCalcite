package com.example;

import org.apache.calcite.adapter.enumerable.EnumerableConvention;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.sql.SqlExplainFormat;
import org.apache.calcite.sql.SqlExplainLevel;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.tools.*;

public final class CalciteUtils {

    private CalciteUtils() {
    }

    public static RelNode parseSql(String sql, FrameworkConfig frameworkConfig)
            throws SqlParseException, ValidationException, RelConversionException {

        final Planner planner = Frameworks.getPlanner(frameworkConfig);
        final SqlNode sqlNode = planner.parse(sql);
        final SqlNode validated = planner.validate(sqlNode);
        final RelRoot relRoot = planner.rel(validated);
        return relRoot.project();
    }

    public static void executeQuery(FrameworkConfig config,
                                    @SuppressWarnings("SameParameterValue") String query, boolean debug)
            throws RelConversionException, SqlParseException, ValidationException {

        Planner planner = Frameworks.getPlanner(config);
        if (debug) {
            System.out.println("Query:" + query);
        }
        SqlNode n = planner.parse(query);
        n = planner.validate(n);
        RelNode root = planner.rel(n).project();
        if (debug) {
            System.out.println(
                    RelOptUtil.dumpPlan("-- Logical Plan", root, SqlExplainFormat.TEXT,
                            SqlExplainLevel.DIGEST_ATTRIBUTES));
        }
        RelOptCluster cluster = root.getCluster();
        final RelOptPlanner optPlanner = cluster.getPlanner();

        RelTraitSet desiredTraits = cluster.traitSet().replace(EnumerableConvention.INSTANCE);
        final RelNode newRoot = optPlanner.changeTraits(root, desiredTraits);
        if (debug) {
            System.out.println(
                    RelOptUtil.dumpPlan("-- Mid Plan", newRoot, SqlExplainFormat.TEXT,
                            SqlExplainLevel.DIGEST_ATTRIBUTES));
        }
        optPlanner.setRoot(newRoot);
        RelNode bestExp = optPlanner.findBestExp();
        if (debug) {
            System.out.println(
                    RelOptUtil.dumpPlan("-- Best Plan", bestExp, SqlExplainFormat.TEXT,
                            SqlExplainLevel.DIGEST_ATTRIBUTES));
        }
    }
}
