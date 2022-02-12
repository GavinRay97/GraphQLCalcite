import com.google.common.graph.MutableNetwork
import com.google.common.graph.NetworkBuilder

@Suppress("UnstableApiUsage")
object ForeignKeyManager {
    val foreignKeyGraph: MutableNetwork<FullyQualifiedTableName, ForeignKey> =
        NetworkBuilder.directed().allowsSelfLoops(false).allowsParallelEdges(false).build()

    fun addForeignKey(foreignKey: ForeignKey) {
        foreignKeyGraph.addEdge(foreignKey.sourceTable, foreignKey.targetTable, foreignKey)
    }

    fun getForeignKeysForTable(table: FullyQualifiedTableName): Set<ForeignKey> {
        return try {
            foreignKeyGraph.incidentEdges(table)
        } catch (_: Exception) {
            emptySet()
        }
    }
}
