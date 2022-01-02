import graphql.language.Field
import graphql.language.ObjectValue
import graphql.language.OperationDefinition
import org.apache.calcite.rel.RelNode
import org.apache.calcite.tools.RelBuilder

data class Column(
    val name: String
)

data class WhereClause(
    val children: List<Predicate>
)

data class Selection(
    val columns: List<Column>,
    val join: List<TableQuery> = emptyList()
)

data class TableQuery(
    val table: String,
    val where: WhereClause? = null,
    val select: Selection
)

fun TableQuery.toRelNode(builder: RelBuilder): RelNode {
    return builder.apply {
        // NOTE: This only works if there's a single schema, the full syntax is:
        // scan("schemaName", "tableName")
        this.scan("JDBC_SCOTT", table)
        requireNotNull(select.columns.firstOrNull()) { "Error in TableQuery.toRelNode(): No columns selected" }
        this.project(select.columns.map { this.field(it.name) })
        if (where != null) {
            this.filter(where.children.map { it.toRexNode(this) })
        }
    }.build()
}

fun tableQueryFromGQLQueryOperation(operation: OperationDefinition): List<TableQuery> {
    val selectionSetFields = operation.selectionSet.selections.filterIsInstance<Field>()
    return selectionSetFields.map(::gqlQuerySelectionSetFieldToTableQuery)
}

fun gqlQuerySelectionSetFieldToTableQuery(selectionSetField: Field): TableQuery {
    val fields = selectionSetField.selectionSet.selections.filterIsInstance<Field>()
    return TableQuery(
        table = selectionSetField.name,
        where = selectionSetField.arguments.find { it.name == "where" }?.value?.let {
            WhereClause(children = graphqlWhereArgToPredicates(it as ObjectValue))
        },
        select = Selection(
            columns = fields.filter { it.arguments.isEmpty() }.map { Column(it.name) },
            join = fields.filter { it.arguments.isNotEmpty() }.map { gqlQuerySelectionSetFieldToTableQuery(it) }
        ),
    )
}
