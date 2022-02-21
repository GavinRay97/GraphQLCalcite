import entity.AND
import entity.COLUMN
import entity.EQ
import entity.IN
import entity.LITERAL
import entity.NOT
import entity.OR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExpressionTest {
    @Test
    fun `expression can be converted to SQL`() {
        val expression = AND(
            OR(
                EQ(COLUMN("a"), LITERAL(1)),
                EQ(COLUMN("b"), LITERAL(2))
            ),
            EQ(COLUMN("c"), LITERAL(3))
        )
        assertEquals("((a = 1 OR b = 2) AND c = 3)", expression.toSQL())
    }

    @Test
    fun `expression can be converted to SQL with nested expressions`() {
        val expression = AND(
            OR(
                EQ(COLUMN("a"), LITERAL(1)),
                EQ(COLUMN("b"), LITERAL(2))
            ),
            OR(
                EQ(COLUMN("c"), LITERAL(3)),
                AND(
                    NOT(
                        OR(
                            EQ(COLUMN("e"), LITERAL(5)),
                            EQ(COLUMN("f"), LITERAL(6))
                        )
                    ),
                    IN(COLUMN("g"), LITERAL(listOf(7, 8)))
                )
            ),
        )
        assertEquals(
            "((a = 1 OR b = 2) AND (c = 3 OR (entity.NOT (e = 5 OR f = 6) AND g IN (7, 8)))",
            expression.toSQL()
        )
    }
}
