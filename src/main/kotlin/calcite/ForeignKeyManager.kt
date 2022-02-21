package calcite

import com.google.common.graph.MutableNetwork
import com.google.common.graph.NetworkBuilder
import entity.ForeignKey
import entity.FullyQualifiedTableName

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

    fun getIncomingForeignKeysForTable(table: FullyQualifiedTableName): Set<ForeignKey> {
        return try {
            foreignKeyGraph.inEdges(table)
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun getOutgoingForeignKeysForTable(table: FullyQualifiedTableName): Set<ForeignKey> {
        return try {
            foreignKeyGraph.outEdges(table)
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


data class PrimaryKey(val table: CalciteRootSchema.Table, val columns: List<String>) {
    override fun toString(): String {
        return "PrimaryKey2(table=$table, columns=$columns)"
    }
}

object PrimaryKeyManager {
    val primaryKeys: MutableMap<CalciteRootSchema.Table, PrimaryKey> = mutableMapOf()
}

