import graphql.GraphQL
import javax.sql.DataSource

// Creates sample data for the example application.
object ExampleQueryProvider {
    val datasource: DataSource = org.hsqldb.jdbc.JDBCDataSource().apply {
        setURL("jdbc:hsqldb:mem:test1")
        setUser("sa")
        setPassword("")
    }
    val graphql: GraphQL

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

    init {
        datasource.connection.createStatement().execute(
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
        datasource.connection.createStatement().execute(
            """
                insert into user (id, name) values (1, 'John Doe');
                insert into user (id, name) values (2, 'Jane Doe');
                insert into post (id, user_id, title, content) values (1, 1, 'Post 1', 'Content 1');
                insert into post (id, user_id, title, content) values (2, 1, 'Post 2', 'Content 2');
                insert into post (id, user_id, title, content) values (3, 2, 'Post 3', 'Content 3');
                insert into post (id, user_id, title, content) values (4, 1, 'Post 4', 'Content 3');
                """
        )
        
        CalciteSchemaManager.addDatabase("hsql", datasource)
        ForeignKeyManager.addForeignKey(
            ForeignKey(
                sourceTable = FullyQualifiedTableName("hsql", "PUBLIC", "POST"),
                targetTable = FullyQualifiedTableName("hsql", "PUBLIC", "USER"),
                columns = listOf(
                    "USER_ID" to "ID"
                ),
            )
        )

        graphql = GraphQL.newGraphQL(
            GraphQLSchemaGenerator.generateGraphQLSchemaFromRootSchema(
                CalciteSchemaManager.rootSchema
            )
        ).build()
    }

}
