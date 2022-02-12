package extensions

import java.sql.ResultSet

fun ResultSet.toListOfMaps(): MutableList<MutableMap<String, Any>> {
    val md = this.metaData
    val columns = md.columnCount
    val rows: MutableList<MutableMap<String, Any>> = ArrayList()
    while (this.next()) {
        val row: MutableMap<String, Any> = HashMap(columns)
        for (i in 1..columns) {
            row[md.getColumnLabel(i)] = this.getObject(i)
        }
        rows.add(row)
    }
    return rows
}
