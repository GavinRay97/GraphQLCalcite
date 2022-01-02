import extensions.toRexLiteral
import graphql.language.ArrayValue
import graphql.language.ObjectField
import graphql.language.ObjectValue
import graphql.language.Value
import org.apache.calcite.rex.RexNode
import org.apache.calcite.sql.SqlOperator
import org.apache.calcite.sql.`fun`.SqlStdOperatorTable
import org.apache.calcite.tools.RelBuilder

sealed interface ComparisonPredicate {
    val column: String
    val value: Value<*>
}

sealed interface BooleanPredicate {
    val children: List<Predicate>
}

sealed class Predicate(val name: String, val stdSqlOperator: SqlOperator) {

    data class AND(override val children: List<Predicate>) :
        Predicate(name = "_and", stdSqlOperator = SqlStdOperatorTable.AND),
        BooleanPredicate

    data class OR(override val children: List<Predicate>) :
        Predicate(name = "_or", stdSqlOperator = SqlStdOperatorTable.OR),
        BooleanPredicate

    // Children shouldn't actually be a list, only one value expected
    // "NOT" is a "SqlPrefixOperator", others are all "SqlBinaryOperator"
    // TODO: Figure out the proper way to handle this
    data class NOT(override val children: List<Predicate>) :
        Predicate(name = "_not", stdSqlOperator = SqlStdOperatorTable.NOT),
        BooleanPredicate

    data class EQUALS(override val column: String, override val value: Value<*>) :
        Predicate(name = "_eq", stdSqlOperator = SqlStdOperatorTable.EQUALS),
        ComparisonPredicate

    data class NOT_EQUALS(override val column: String, override val value: Value<*>) :
        Predicate(name = "_neq", stdSqlOperator = SqlStdOperatorTable.NOT_EQUALS),
        ComparisonPredicate

    data class GREATER_THAN(override val column: String, override val value: Value<*>) :
        Predicate(name = "_gt", stdSqlOperator = SqlStdOperatorTable.GREATER_THAN),
        ComparisonPredicate

    data class GREATER_THAN_OR_EQUALS(override val column: String, override val value: Value<*>) :
        Predicate(name = "_gte", stdSqlOperator = SqlStdOperatorTable.GREATER_THAN_OR_EQUAL),
        ComparisonPredicate

    data class LESS_THAN(override val column: String, override val value: Value<*>) :
        Predicate(name = "_lt", stdSqlOperator = SqlStdOperatorTable.LESS_THAN),
        ComparisonPredicate

    data class LESS_THAN_OR_EQUALS(override val column: String, override val value: Value<*>) :
        Predicate(name = "_lte", stdSqlOperator = SqlStdOperatorTable.LESS_THAN_OR_EQUAL),
        ComparisonPredicate

    data class IN(override val column: String, override val value: Value<*>) :
        Predicate(name = "_in", stdSqlOperator = SqlStdOperatorTable.IN),
        ComparisonPredicate

    data class NOT_IN(override val column: String, override val value: Value<*>) :
        Predicate(name = "_nin", stdSqlOperator = SqlStdOperatorTable.NOT_IN),
        ComparisonPredicate

    fun toRexNode(builder: RelBuilder): RexNode {
        return when (this) {
            is ComparisonPredicate ->
                builder.call(
                    this.stdSqlOperator,
                    builder.field(this.column),
                    this.value.toRexLiteral(builder)
                )
            is BooleanPredicate ->
                when (this) {
                    is AND -> builder.and(this.children.map { it.toRexNode(builder) })
                    is OR -> builder.or(this.children.map { it.toRexNode(builder) })
                    is NOT -> builder.not(this.children.first().toRexNode(builder))
                }
        }
    }
}

/**
 * Converts a single field from a "where" clause into a predicate
 */
fun graphqlWhereArgComparisonExprToPredicate(node: ObjectField): Predicate {
    val firstField: ObjectField = requireNotNull((node.value as? ObjectValue)?.objectFields?.first()) {
        "Expected a single field in the where clause"
    }
    val value = requireNotNull(firstField.value) { "Expected value" }
    val column = requireNotNull(node.name) { "Expected column" }

    return when (firstField.name) {
        "_eq" -> Predicate.EQUALS(column = column, value = value)
        "_neq" -> Predicate.NOT_EQUALS(column = column, value = value)
        "_gt" -> Predicate.GREATER_THAN(column = column, value = value)
        "_gte" -> Predicate.GREATER_THAN_OR_EQUALS(column = column, value = value)
        "_lt" -> Predicate.LESS_THAN(column = column, value = value)
        "_lte" -> Predicate.LESS_THAN_OR_EQUALS(column = column, value = value)
        "_in" -> Predicate.IN(column = column, value = value)
        "_nin" -> Predicate.NOT_IN(column = column, value = value)
        else -> throw IllegalArgumentException("Unknown predicate name: ${firstField.name}")
    }
}

/**
 * Converts a GraphQL "where" object argument to a list of predicates.
 */
fun graphqlWhereArgToPredicates(node: ObjectValue): List<Predicate> {
    return node.objectFields.stream().parallel().map { objectField ->
        when (objectField.name) {
            "_and" -> {
                val andPredicates = requireNotNull(objectField.value as ArrayValue) { "Expected _and to be an array" }
                listOf(
                    Predicate.AND(
                        children = andPredicates.values.flatMap { value ->
                            graphqlWhereArgToPredicates(value as ObjectValue)
                        }
                    )
                )
            }
            "_or" -> {
                val orPredicates = requireNotNull(objectField.value as? ArrayValue) { "Expected _or to be an array" }
                listOf(
                    Predicate.OR(
                        children = orPredicates.values.flatMap { value ->
                            graphqlWhereArgToPredicates(value as ObjectValue)
                        }
                    )
                )
            }
            "_not" -> {
                val notPredicate = requireNotNull(objectField.value as? ObjectValue) { "Expected _not to be an object" }
                listOf(
                    Predicate.NOT(
                        children = listOf(graphqlWhereArgToPredicates(notPredicate).first())
                    )
                )
            }
            else -> {
                listOf(graphqlWhereArgComparisonExprToPredicate(objectField))
            }
        }
    }.toList().flatten()
}
