package server

import ExampleQueryProvider
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.graphql.GraphQLHandler
import io.vertx.ext.web.handler.graphql.GraphiQLHandler
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions
import kotlinx.html.body
import kotlinx.html.html
import kotlinx.html.p
import kotlinx.html.stream.createHTML
import prettyPrint


object VertxServer {
    private const val PORT = 8080

    @JvmStatic
    fun main(args: Array<String>) {
        val vertx = Vertx.vertx()
        val router = Router.router(vertx)
        val exampleResult = ExampleQueryProvider.graphql.execute(ExampleQueryProvider.postToUsersQuery)

        router.route().handler(BodyHandler.create())

        router.get("/").handler { context ->
            val text = createHTML().html {
                body {
                    p {
                        +"Hello World!"
                    }
                    p {
                        +"This is a simple example of a GraphQL server written in Kotlin."
                    }
                    p {
                        +"Try to run the GraphiQL client to see how it works."
                    }
                    p {
                        +"The GraphQL endpoint is at /graphql"
                    }
                    p {
                        +"The GraphiQL client is at /graphiql"
                    }
                }
            }
            context.response().end(text)
        }

        router.get("/graphql-example").handler { context ->
            context.response().end(exampleResult.toSpecification().prettyPrint())
        }

        // TODO: Why the fuck is this broken?!
        // "Exception in metadataHandlerProvider"
        router.get("/graphql-example-dynamic").handler { context ->
            println("Dynamic query")
            context.response().end(
                ExampleQueryProvider.graphql
                    .execute(ExampleQueryProvider.postToUsersQuery)
                    .toSpecification()
                    .prettyPrint()
            )
        }

        val graphiqlOpts = GraphiQLHandlerOptions().setEnabled(true)
        val graphql = ExampleQueryProvider.graphql

        router.post("/graphql").handler(GraphQLHandler.create(graphql))
        router.route("/graphiql/*").handler(GraphiQLHandler.create(graphiqlOpts))

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(PORT)
    }
}

