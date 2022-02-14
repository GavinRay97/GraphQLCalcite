package com.example;

import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlBinaryOperator;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.tools.RelBuilder;

public interface Expression {

    default RexNode toRexNode(RelBuilder relBuilder) {
        if (this instanceof Literal literal) {
            return relBuilder.literal(literal.value());
        } else if (this instanceof Column column) {
            return relBuilder.field(column.name());
        } else if (this instanceof BinaryOperation binaryOperation) {
            return relBuilder.call(
                    binaryOperation.operator(),
                    binaryOperation.left().toRexNode(relBuilder),
                    binaryOperation.right().toRexNode(relBuilder));
        } else if (this instanceof UnaryOperation unaryOperation) {
            return relBuilder.call(
                    unaryOperation.operator(),
                    unaryOperation.operand().toRexNode(relBuilder));
        }
        // Impossible
        throw new RuntimeException("Unsupported expression type: " + this.getClass().getName());
    }

    record Literal(Object value) implements Expression {
        @Override
        public RexNode toRexNode(RelBuilder relBuilder) {
            return relBuilder.literal(value);
        }
    }

    record Column(String name) implements Expression {
        @Override
        public RexNode toRexNode(RelBuilder relBuilder) {
            return relBuilder.field(name);
        }
    }

    interface BinaryOperation extends Expression {
        SqlOperator operator();

        Expression left();

        Expression right();
    }

    interface UnaryOperation extends Expression {
        SqlOperator operator();

        Expression operand();
    }

    record AND(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlBinaryOperator operator() {
            return SqlStdOperatorTable.AND;
        }
    }

    record OR(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlBinaryOperator operator() {
            return SqlStdOperatorTable.OR;
        }
    }

    record EQ(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlBinaryOperator operator() {
            return SqlStdOperatorTable.EQUALS;
        }
    }

    record NEQ(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlBinaryOperator operator() {
            return SqlStdOperatorTable.NOT_EQUALS;
        }
    }

    record LT(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlBinaryOperator operator() {
            return SqlStdOperatorTable.LESS_THAN;
        }
    }

    record LTE(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlBinaryOperator operator() {
            return SqlStdOperatorTable.LESS_THAN_OR_EQUAL;
        }
    }

    record GT(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlBinaryOperator operator() {
            return SqlStdOperatorTable.GREATER_THAN;
        }
    }

    record GTE(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlBinaryOperator operator() {
            return SqlStdOperatorTable.GREATER_THAN_OR_EQUAL;
        }
    }

    record IN(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlBinaryOperator operator() {
            return SqlStdOperatorTable.IN;
        }
    }

    record NIN(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlBinaryOperator operator() {
            return SqlStdOperatorTable.NOT_IN;
        }
    }

    record LIKE(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlOperator operator() {
            return SqlStdOperatorTable.LIKE;
        }
    }

    record NLIKE(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlOperator operator() {
            return SqlStdOperatorTable.NOT_LIKE;
        }
    }

    record SIMILAR(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlOperator operator() {
            return SqlStdOperatorTable.SIMILAR_TO;
        }
    }

    record NSIMILAR(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlOperator operator() {
            return SqlStdOperatorTable.NOT_SIMILAR_TO;
        }
    }

    record REGEX(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlOperator operator() {
            return SqlStdOperatorTable.POSIX_REGEX_CASE_SENSITIVE;
        }
    }

    record NREGEX(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlOperator operator() {
            return SqlStdOperatorTable.NEGATED_POSIX_REGEX_CASE_SENSITIVE;
        }
    }

    record IREGEX(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlOperator operator() {
            return SqlStdOperatorTable.POSIX_REGEX_CASE_INSENSITIVE;
        }
    }

    record NIREGEX(Expression left, Expression right) implements BinaryOperation {
        @Override
        public SqlOperator operator() {
            return SqlStdOperatorTable.NEGATED_POSIX_REGEX_CASE_INSENSITIVE;
        }
    }

    record NOT(Expression operand) implements UnaryOperation {
        @Override
        public SqlOperator operator() {
            return SqlStdOperatorTable.NOT;
        }
    }

    record IS_NULL(Expression operand) implements UnaryOperation {
        @Override
        public SqlOperator operator() {
            return SqlStdOperatorTable.IS_NULL;
        }
    }
}
