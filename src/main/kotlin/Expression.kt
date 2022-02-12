import org.apache.calcite.rex.RexNode
import org.apache.calcite.sql.SqlBinaryOperator
import org.apache.calcite.sql.SqlOperator
import org.apache.calcite.sql.`fun`.SqlStdOperatorTable
import org.apache.calcite.tools.RelBuilder

sealed interface Expression {
    fun toRexNode(builder: RelBuilder): RexNode = when (this) {
        is BinaryOperation -> {
            val left = left.toRexNode(builder)
            val right = right.toRexNode(builder)
            builder.call(operation, left, right)
        }
        is UnaryOperation -> {
            val operand = operand.toRexNode(builder)
            builder.call(operation, operand)
        }
        is COLUMN -> builder.field(name)
        is LITERAL -> builder.literal(value)
    }
}

sealed interface BinaryOperation : Expression {
    val left: Expression
    val right: Expression
    val operation: SqlOperator
}

data class AND(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlBinaryOperator = SqlStdOperatorTable.AND
}

data class OR(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlBinaryOperator = SqlStdOperatorTable.OR
}

data class EQ(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlBinaryOperator = SqlStdOperatorTable.EQUALS
}

data class NEQ(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlBinaryOperator = SqlStdOperatorTable.NOT_EQUALS
}

data class LT(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlBinaryOperator = SqlStdOperatorTable.LESS_THAN
}

data class LTE(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlBinaryOperator = SqlStdOperatorTable.LESS_THAN_OR_EQUAL
}

data class GT(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlBinaryOperator = SqlStdOperatorTable.GREATER_THAN
}

data class GTE(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlBinaryOperator = SqlStdOperatorTable.GREATER_THAN_OR_EQUAL
}

data class IN(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlBinaryOperator = SqlStdOperatorTable.IN
}

data class NIN(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlBinaryOperator = SqlStdOperatorTable.NOT_IN
}

data class LIKE(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlOperator = SqlStdOperatorTable.LIKE
}

data class NLIKE(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlOperator = SqlStdOperatorTable.NOT_LIKE
}

data class REGEX(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlBinaryOperator = SqlStdOperatorTable.POSIX_REGEX_CASE_SENSITIVE
}

data class IREGEX(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlBinaryOperator = SqlStdOperatorTable.POSIX_REGEX_CASE_INSENSITIVE
}

data class NREGEX(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlBinaryOperator = SqlStdOperatorTable.NEGATED_POSIX_REGEX_CASE_SENSITIVE
}

data class NIREGEX(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlBinaryOperator = SqlStdOperatorTable.NEGATED_POSIX_REGEX_CASE_INSENSITIVE
}

data class SIMILAR(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlOperator = SqlStdOperatorTable.SIMILAR_TO
}

data class NSIMILAR(override val left: Expression, override val right: Expression) : BinaryOperation {
    override val operation: SqlOperator = SqlStdOperatorTable.NOT_SIMILAR_TO
}

sealed interface UnaryOperation : Expression {
    val operand: Expression
    val operation: SqlOperator
}

data class NOT(override val operand: Expression) : UnaryOperation {
    override val operation: SqlOperator = SqlStdOperatorTable.NOT
}

data class IS_NULL(override val operand: Expression) : UnaryOperation {
    override val operation: SqlOperator = SqlStdOperatorTable.IS_NULL
}

@JvmInline
value class COLUMN(val name: String) : Expression

@JvmInline
value class LITERAL(val value: Any) : Expression

