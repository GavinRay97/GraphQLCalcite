package extensions

import graphql.language.ArrayValue
import graphql.language.BooleanValue
import graphql.language.FloatValue
import graphql.language.IntValue
import graphql.language.NullValue
import graphql.language.StringValue
import graphql.language.Value
import org.apache.calcite.rex.RexLiteral
import org.apache.calcite.tools.RelBuilder

fun Value<*>.toRexLiteral(builder: RelBuilder): RexLiteral {
    return when (this) {
        is StringValue -> builder.literal(this.value as String)
        is IntValue -> builder.literal(this.value)
        is FloatValue -> builder.literal(this.value)
        is BooleanValue -> builder.literal(this.isValue)
        is NullValue -> builder.literal(null)
        // FIXME: Figure out how to support arrays
        is ArrayValue -> throw IllegalArgumentException("Arrays are not supported")
        else -> throw IllegalArgumentException("Unsupported value type: ${this.javaClass}")
    }
}
