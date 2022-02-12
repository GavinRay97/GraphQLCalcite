package com.example

import CalciteSchemaManager
import GraphQLSchemaGenerator
import graphql.GraphQL
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Blackhole
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup
import org.hsqldb.jdbc.JDBCDataSource
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@Warmup(iterations = 1)
@Measurement(iterations = 10, time = 3, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class Benchmarks {

    @State(Scope.Thread)
    class BenchmarkState {

        lateinit var datasource: JDBCDataSource
        lateinit var graphql: GraphQL

        @Setup
        fun setup() {
            datasource = JDBCDataSource()
            datasource.setURL("jdbc:hsqldb:mem:foreignkeytest1;shutdown=true")
            datasource.connection.createStatement().execute(
                """
                        create table users (
                            id int primary key,
                            name varchar(255)
                        );
                        create table posts (
                            id int primary key,
                            user_id int,
                            title varchar(255),
                            content varchar(255),
                            foreign key (user_id) references users(id)
                        );
                        """
            )
            datasource.connection.createStatement().execute(
                """
                        insert into users (id, name) values (1, 'John Doe');
                        insert into users (id, name) values (2, 'Jane Doe');
                        insert into posts (id, user_id, title, content) values (1, 1, 'Post 1', 'Content 1');
                        insert into posts (id, user_id, title, content) values (2, 1, 'Post 2', 'Content 2');
                        """
            )

            CalciteSchemaManager.addDatabase("hsql", datasource)
            val graphqlSchema = GraphQLSchemaGenerator.generateGraphQLSchemaFromRootSchema(
                CalciteSchemaManager.rootSchema
            )
            graphql = GraphQL.newGraphQL(graphqlSchema).build()
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @BenchmarkMode(Mode.SampleTime)
    @Warmup(iterations = 5)
    @Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
    fun endToEndQuery(blackhole: Blackhole, benchmarkState: BenchmarkState) {
        val executionResult = benchmarkState.graphql.execute(
            """
            query {
                hsql {
                    PUBLIC {
                        POSTS(where: { ID: { _lt: 2 } }) {
                            ID
                            TITLE
                        }
                    }
                  }
              }
            """
        )
        blackhole.consume(executionResult)
    }
}
