import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import org.apache.calcite.DataContext
import org.apache.calcite.linq4j.Enumerable
import org.apache.calcite.linq4j.Linq4j
import org.apache.calcite.rel.RelCollations
import org.apache.calcite.rel.RelFieldCollation
import org.apache.calcite.rel.type.RelDataType
import org.apache.calcite.rel.type.RelDataTypeFactory
import org.apache.calcite.schema.ScannableTable
import org.apache.calcite.schema.Statistic
import org.apache.calcite.schema.Statistics
import org.apache.calcite.schema.Table
import org.apache.calcite.schema.impl.AbstractSchema
import org.apache.calcite.schema.impl.AbstractTable
import org.apache.calcite.util.ImmutableBitSet
import java.util.*
import java.util.function.Function

/**
 * A typical HR schema with employees (emps) and departments (depts) tables that are naturally
 * ordered based on their primary keys representing clustered tables.
 */
class HrClusteredSchemaKotlin : AbstractSchema() {
    private val tables: ImmutableMap<String, Table>

    init {
        tables = ImmutableMap.builder<String, Table>()
            .put(
                "emps",
                PkClusteredTable(
                    { factory: RelDataTypeFactory ->
                        RelDataTypeFactory.Builder(factory)
                            .add("empid", factory.createJavaType(Int::class.javaPrimitiveType))
                            .add("deptno", factory.createJavaType(Int::class.javaPrimitiveType))
                            .add("name", factory.createJavaType(String::class.java))
                            .add("salary", factory.createJavaType(Int::class.javaPrimitiveType))
                            .add("commission", factory.createJavaType(Int::class.java))
                            .build()
                    },
                    ImmutableBitSet.of(0),
                    Arrays.asList(
                        arrayOf(100, 10, "Bill", 10000, 1000),
                        arrayOf(110, 10, "Theodore", 11500, 250),
                        arrayOf(150, 10, "Sebastian", 7000, null),
                        arrayOf(200, 20, "Eric", 8000, 500)
                    )
                )
            )
            .put(
                "depts",
                PkClusteredTable(
                    { factory: RelDataTypeFactory ->
                        RelDataTypeFactory.Builder(factory)
                            .add("deptno", factory.createJavaType(Int::class.javaPrimitiveType))
                            .add("name", factory.createJavaType(String::class.java))
                            .build()
                    },
                    ImmutableBitSet.of(0),
                    Arrays.asList(
                        arrayOf(10, "Sales"),
                        arrayOf(30, "Marketing"),
                        arrayOf(40, "HR")
                    )
                )
            )
            .build()
    }

    override fun getTableMap(): Map<String, Table> {
        return tables
    }

    /**
     * A table sorted (ascending direction and nulls last) on the primary key.
     */
    private class PkClusteredTable(
        private val typeBuilder: Function<RelDataTypeFactory, RelDataType>,
        private val pkColumns: ImmutableBitSet,
        private val data: List<Array<Any?>>
    ) : AbstractTable(), ScannableTable {
        override fun getStatistic(): Statistic {
            val collationFields: MutableList<RelFieldCollation> = ArrayList()
            for (key in pkColumns) {
                collationFields.add(
                    RelFieldCollation(
                        key!!,
                        RelFieldCollation.Direction.ASCENDING,
                        RelFieldCollation.NullDirection.LAST
                    )
                )
            }
            return Statistics.of(
                data.size.toDouble(), ImmutableList.of(pkColumns),
                ImmutableList.of(RelCollations.of(collationFields))
            )
        }

        override fun getRowType(typeFactory: RelDataTypeFactory): RelDataType {
            return typeBuilder.apply(typeFactory)
        }

        override fun scan(root: DataContext): Enumerable<Array<Any?>> {
            return Linq4j.asEnumerable(data)
        }
    }
}