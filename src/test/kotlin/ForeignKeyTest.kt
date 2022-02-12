import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import graphql.GraphQL
import org.apache.calcite.rel.RelNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.JdbcDatabaseContainer
import java.sql.Connection
import javax.sql.DataSource
import kotlin.system.measureTimeMillis


typealias FullyQualifiedName = List<String>

class ForeignKeyTest {

    companion object {
        // Create in-memory H2 database
        val datasource1: DataSource = org.hsqldb.jdbc.JDBCDataSource().apply {
            setURL("jdbc:hsqldb:mem:test1")
            setUser("sa")
            setPassword("")
        }
        val datasource2: DataSource = org.hsqldb.jdbc.JDBCDataSource().apply {
            setURL("jdbc:hsqldb:mem:test2")
            setUser("sa")
            setPassword("")
        }
        val connection1: Connection = datasource1.connection
        val connection2: Connection = datasource2.connection

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            connection1.createStatement().execute(
                """
                create table user (
                    id int primary key,
                    name varchar(255)
                );
                create table post (
                    id int primary key,
                    user_id int,
                    title varchar(255),
                    content varchar(255),
                    foreign key (user_id) references user (id)
                );
                """
            )
            connection1.createStatement().execute(
                """
                insert into user (id, name) values (1, 'John Doe');
                insert into user (id, name) values (2, 'Jane Doe');
                insert into post (id, user_id, title, content) values (1, 1, 'Post 1', 'Content 1');
                insert into post (id, user_id, title, content) values (2, 1, 'Post 2', 'Content 2');
                insert into post (id, user_id, title, content) values (3, 2, 'Post 3', 'Content 3');
                insert into post (id, user_id, title, content) values (4, 1, 'Post 4', 'Content 3');
                
                """
            )

            connection2.createStatement().execute(
                """
                create table comments (
                    id int primary key,
                    post_id int,
                    content varchar(255)
                );
                """
            )
            connection2.createStatement().execute(
                """
                insert into comments (id, post_id, content) values (1, 1, 'Comment 1');
                insert into comments (id, post_id, content) values (2, 1, 'Comment 2');
                """
            )
        }

    }

    @Test
    fun manualTest() {
        var builder = CalciteSchemaManager.relBuilder
        CalciteSchemaManager.addDatabase("hsql", datasource1)

        ForeignKeyManager.addForeignKey(
            ForeignKey(
                sourceTable = FullyQualifiedTableName("hsql", "PUBLIC", "POST"),
                targetTable = FullyQualifiedTableName("hsql", "PUBLIC", "USER"),
                columns = listOf(
                    "USER_ID" to "ID"
                ),
            )
        )

        val relNode = builder.build()

        executeRelationalExpr(relNode)
    }

    fun getDatasourceFromJdbcTestcontainer(container: JdbcDatabaseContainer<Nothing>): DataSource {
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = container.jdbcUrl
        hikariConfig.username = container.username
        hikariConfig.password = container.password
        hikariConfig.driverClassName = container.driverClassName
        return HikariDataSource(hikariConfig)
    }

    private fun executeRelationalExpr(relNode: RelNode): MutableList<Map<String, Any>> {
        val resultSet = CalciteSchemaManager.executeQuery(relNode)
        val queryResult: MutableList<Map<String, Any>> = ArrayList()
        while (resultSet.next()) {
            val row: MutableMap<String, Any> = HashMap()
            // Iterate result columns
            for (i in 0 until resultSet.metaData.columnCount) {
                val columnName = resultSet.metaData.getColumnName(i + 1)
                val columnValue = resultSet.getObject(i + 1)
                row[columnName] = columnValue
            }
            queryResult.add(row)
        }
        println("Query result:\n$queryResult")
        return queryResult
    }

    @Test
    fun testOther() {
        val rootSchema = CalciteSchemaManager.rootSchema
        CalciteSchemaManager.addDatabase("hsql", datasource1)

        ForeignKeyManager.addForeignKey(
            ForeignKey(
                sourceTable = FullyQualifiedTableName("hsql", "PUBLIC", "POST"),
                targetTable = FullyQualifiedTableName("hsql", "PUBLIC", "USER"),
                columns = listOf(
                    "USER_ID" to "ID"
                ),
            )
        )


        val graphqlSchema = GraphQLSchemaGenerator.generateGraphQLSchemaFromRootSchema(rootSchema)
        val graphql = GraphQL.newGraphQL(graphqlSchema).build()

        val postToUsersQuery = """
              query {
                hsql {
                    PUBLIC {
                        POST(where: {ID: {_lte: 2}}) {
                            ID
                            TITLE
                            USER_ID
                            USER {
                                ID
                                NAME
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        val usersToPostsQuery = """
              query {
                hsql {
                    PUBLIC {
                        USER(where: {ID: {_lte: 4}}) {
                            ID
                            NAME
                            POST {
                                ID
                                TITLE
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        for (i in 0..10) {
            val time = measureTimeMillis {
                graphql.execute(usersToPostsQuery)
            }
            println("$i usersToPostsQuery: $time")
        }

        for (i in 0..10) {
            val time = measureTimeMillis {
                graphql.execute(postToUsersQuery)
            }
            println("$i postToUsersQuery: $time")
        }
    }

//    @Test
//    fun testForeignKey() {
//        val options: SchemaCrawlerOptions = SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions()
//        val catalog: Catalog = SchemaCrawlerUtility.getCatalog(connection1, options)
//        val fkeys = catalog.tables.flatMap { it.foreignKeys }.distinct()
//        fkeys.forEach {
//            println("${it.referencingTable.fullName} ${it.constrainedColumns.joinToString(",")} -> ${it.referencedTable.fullName}")
//            println(it.columnReferences)
//        }
//
//        CalciteSchemaManager.addDatabase("db1", datasource1)
//        CalciteSchemaManager.addDatabase("db2", datasource2)
//
//        println(CalciteSchemaManager.rootSchema.print())
//
//        val timeTaken = measureTimeMillis {
//            CalciteUtils.executeQuery(
//                CalciteSchemaManager.frameworkConfig,
//                """
//            select * from db1.PUBLIC.POSTS
//            inner join db2.PUBLIC.COMMENTS
//                    on db1.PUBLIC.POSTS.ID = db2.PUBLIC.COMMENTS.POST_ID
//        """,
//                false
//            )
//        }
//        println(timeTaken)
//    }
}
