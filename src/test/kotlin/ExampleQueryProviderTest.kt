import org.junit.jupiter.api.Test

internal class ExampleQueryProviderTest {
    @Test
    fun `should return correct query`() {
        val result = ExampleQueryProvider.graphql.execute(
            ExampleQueryProvider.postToUsersQuery
        )
        println(result.toSpecification().prettyPrint())
    }
}
