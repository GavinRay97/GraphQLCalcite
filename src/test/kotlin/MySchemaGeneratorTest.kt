import calcite.CalciteRootSchema
import calcite.CalciteSchemaManager
import calcite.PrimaryKey
import calcite.PrimaryKeyManager
import graphql.GraphQL
import graphql.GraphQLSchemaGeneratorNew
import graphql.operationgenerators.FindAllQueryGenerator
import graphql.operationgenerators.FindByPkQueryGenerator
import graphql.operationgenerators.InsertManyMutationGenerator
import graphql.schema.GraphQLSchema
import graphql.schema.idl.SchemaPrinter
import operation_providers.DefaultSqlTypeToGraphQLMapping
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection
import javax.sql.DataSource

class MySchemaGeneratorTest {
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
        fun setUp() {
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
    fun testGenerateSchema() {
        CalciteSchemaManager.addDatabase("hsql1", datasource1)
        CalciteSchemaManager.addDatabase("hsql2", datasource2)

        val calciteRootSchema = CalciteRootSchema(CalciteSchemaManager.rootSchema)
        val usersTable = calciteRootSchema
            .databases().find { it.underlying.name == "hsql1" }!!
            .schemas().find { it.underlying.name == "PUBLIC" }!!
            .tables().find { it.name == "USER" }!!

        val postsTable = calciteRootSchema
            .databases().find { it.underlying.name == "hsql1" }!!
            .schemas().find { it.underlying.name == "PUBLIC" }!!
            .tables().find { it.name == "POST" }!!

        PrimaryKeyManager.primaryKeys[usersTable] = PrimaryKey(usersTable, listOf("ID"))
        PrimaryKeyManager.primaryKeys[postsTable] = PrimaryKey(postsTable, listOf("ID"))

        val schemaGenerator = GraphQLSchemaGeneratorNew(
            calciteRootSchema,
            DefaultSqlTypeToGraphQLMapping
        )

        val schema: GraphQLSchema = schemaGenerator.generate(
            queryGenerators = listOf(
                FindAllQueryGenerator,
                FindByPkQueryGenerator,
            ),
            mutationGenerators = listOf(
                InsertManyMutationGenerator
            ),
        )

        println(SchemaPrinter().print(schema))

        val query = """
            mutation {
              hsql1 {
                PUBLIC {
                  USER(objects: [{ID: 3, NAME: "Frank Person"}, { ID: 4, NAME: "Anne Human" }]) {
                    affected_rows
                    returning {
                      ID
                      NAME
                    }
                  }
                }
              }
            }
        """
        val graphql: GraphQL = GraphQL.newGraphQL(schema).build()
        val result = graphql.execute(query)
        println(result.toSpecification().prettyPrint())


        val query2 = """
            query {
              hsql1 {
                PUBLIC {
                  USER {
                    ID
                    NAME
                  }
                }
              }
            }
        """
        val result2 = graphql.execute(query2)
        println(result2.toSpecification().prettyPrint())

    }
}
