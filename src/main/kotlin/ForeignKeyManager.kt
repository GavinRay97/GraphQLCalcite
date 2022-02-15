import com.google.common.graph.MutableNetwork
import com.google.common.graph.NetworkBuilder

// TODO: Auto-discovery of foreign keys
// Maybe using schema-crawler?
@Suppress("UnstableApiUsage")
object ForeignKeyManager {
    val foreignKeyGraph: MutableNetwork<FullyQualifiedTableName, ForeignKey> =
        NetworkBuilder
            .directed()
            .allowsSelfLoops(false)
            .allowsParallelEdges(false)
            .build()

    fun addForeignKey(foreignKey: ForeignKey) {
        foreignKeyGraph.addEdge(
            foreignKey.sourceTable,
            foreignKey.targetTable,
            foreignKey
        )
    }

    fun getForeignKeysForTable(table: FullyQualifiedTableName): Set<ForeignKey> {
        return try {
            foreignKeyGraph.incidentEdges(table)
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun isForeignKey(table: FullyQualifiedTableName): Boolean {
        return foreignKeyGraph.nodes().contains(table)
    }

    fun isPrimaryKey(table: FullyQualifiedTableName): Boolean {
        return foreignKeyGraph.nodes().contains(table)
    }
}

data class PrimaryKey(val fullyQualifiedTableName: FullyQualifiedTableName, val columns: List<String>) {
    override fun toString(): String {
        return "PrimaryKey(fullyQualifiedTableName=$fullyQualifiedTableName, columns=$columns)"
    }
}

// TODO: Auto-discovery of primary keys
// Maybe using schema-crawler?
object PrimaryKeyManager {
    val primaryKeys: MutableList<PrimaryKey> = mutableListOf()

    val primaryKeysByTable: Map<FullyQualifiedTableName, List<PrimaryKey>>
        get() = primaryKeys.groupBy { it.fullyQualifiedTableName }

    fun addPrimaryKey(primaryKey: PrimaryKey) {
        primaryKeys.add(primaryKey)
    }
}
