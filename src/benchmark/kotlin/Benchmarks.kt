package com.example

import HrClusteredSchemaKotlin
import TEST_GRAPHQL_QUERY
import extensions.toGraphQLSchema
import gqlQuerySelectionSetFieldToTableQuery
import graphql.ExecutionInput
import graphql.ParseAndValidate
import graphql.language.Field
import graphql.language.OperationDefinition
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Blackhole
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@Warmup(iterations = 1)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
class Benchmarks {
    private val calciteSchema = HrClusteredSchemaKotlin()
    private val graphqlSchema = ParseAndValidate.parse(ExecutionInput.newExecutionInput(TEST_GRAPHQL_QUERY).build())

    private val topLevelQueryNodes = graphqlSchema.document.definitions
        .filterIsInstance<OperationDefinition>()
        .filter { it.operation == OperationDefinition.Operation.QUERY }

    private val queryNode = topLevelQueryNodes.first().selectionSet.selections.filterIsInstance<Field>().first()

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    fun Benchmark_calciteSchemaToGraphQLSchema(blackhole: Blackhole) {
        blackhole.consume(
            calciteSchema.toGraphQLSchema()
        )
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    fun Benchmark_parse_GraphQL_query(blackhole: Blackhole) {
        blackhole.consume(
            ParseAndValidate.parse(ExecutionInput.newExecutionInput(TEST_GRAPHQL_QUERY).build())
        )
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    fun Benchmark_convert_GraphQL_query_AST_to_TableQuery(blackhole: Blackhole) {
        blackhole.consume(
            gqlQuerySelectionSetFieldToTableQuery(queryNode)
        )
    }
}
