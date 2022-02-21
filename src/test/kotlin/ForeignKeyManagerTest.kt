import calcite.ForeignKeyManager
import entity.ForeignKey
import entity.FullyQualifiedTableName
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty

internal class ForeignKeyManagerTest {
    companion object {
        val todoUserForeignKey = ForeignKey(
            sourceTable = FullyQualifiedTableName("hsql", "PUBLIC", "TODO"),
            targetTable = FullyQualifiedTableName("hsql", "PUBLIC", "USER"),
            columns = listOf(
                "USER_ID" to "ID"
            )
        )
        val userHouseForeignKey = ForeignKey(
            sourceTable = FullyQualifiedTableName("hsql", "PUBLIC", "USER"),
            targetTable = FullyQualifiedTableName("hsql", "PUBLIC", "HOUSE"),
            columns = listOf(
                "HOUSE_ID" to "ID"
            )
        )

        @BeforeAll
        @JvmStatic
        fun setup() {
            ForeignKeyManager.addForeignKey(todoUserForeignKey)
            ForeignKeyManager.addForeignKey(userHouseForeignKey)
        }
    }

    @Test
    fun graphTest() {
        ForeignKeyManager.getForeignKeysForTable(FullyQualifiedTableName("hsql", "PUBLIC", "USER"))

        expectThat(ForeignKeyManager.getForeignKeysForTable(FullyQualifiedTableName("hsql", "PUBLIC", "TODO")))
            .isNotEmpty()
            .hasSize(1)
            .isEqualTo(
                setOf(todoUserForeignKey)
            )
    }

    @Test
    fun getForeignKeys() {
        val todo = FullyQualifiedTableName("hsql", "PUBLIC", "TODO")
        val foreignKeys = ForeignKeyManager.getForeignKeysForTable(todo)
        expectThat(foreignKeys) {
            isNotEmpty()
            hasSize(1)
        }
    }
}
