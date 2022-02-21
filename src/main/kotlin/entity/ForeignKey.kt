package entity

import calcite.CalciteSchemaManager
import org.apache.calcite.rel.RelReferentialConstraint
import org.apache.calcite.util.mapping.IntPair

data class ForeignKey(
    val sourceTable: FullyQualifiedTableName,
    val targetTable: FullyQualifiedTableName,
    val columns: List<Pair<String, String>>
) : RelReferentialConstraint {

    /**The qualified name of the referencing table, e.g. DEPT.  */
    override fun getSourceQualifiedName(): List<String> {
        return sourceTable.toStringPathList()
    }

    /** The qualified name of the referenced table, e.g. EMP.  */
    override fun getTargetQualifiedName(): List<String> {
        return targetTable.toStringPathList()
    }

    /** The (source, target) column ordinals.  */
    override fun getColumnPairs(): List<IntPair> {
        return columns.map {
            IntPair(
                CalciteSchemaManager.getOrdinalForColumn(CalciteSchemaManager.rootSchema, sourceTable, it.first),
                CalciteSchemaManager.getOrdinalForColumn(CalciteSchemaManager.rootSchema, sourceTable, it.first)
            )
        }
    }
}
