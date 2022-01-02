import graphql.language.ObjectValue
import org.apache.calcite.rex.RexNode

object GraphQLToCalcite {

    /**
     * Converts a GraphQL [ObjectValue] from the "where" clause in a query to a Calcite [RexNode]
     *
     * Example:
     *  query {
     *      users(where: {
     *          _and: [{ name: { _eq: "John" } }, { age: { _gte: 25 } }]
     *      }) {}
     *  }
     *
     * @param builder the builder to use to create the RexNode
     * @param gqlNode the GraphQL [ObjectValue] to convert
     * @return the Calcite [RexNode]
     */
    // fun recursiveGQLWherePredicatesToRexNodePredicates(
    //     builder: RelBuilder,
    //     gqlNode: ObjectValue
    // ): List<RexNode> {
    //     // We can safely parrallelize the map because order is not important
    //     return gqlNode.objectFields.stream().parallel().map {
    //         println("Processing field: ${it.name} on thread ${Thread.currentThread().name}")
    //         return@map when (it.name) {
    //             "_and" -> {
    //                 val andPredicates = it.value as? ArrayValue
    //                     ?: throw IllegalArgumentException("Expected _and to be an array")
    //                 println("AND: ${andPredicates.values}")
    //
    //                 listOf(
    //                     builder.and(
    //                         andPredicates.values.flatMap { innerValue ->
    //                             recursiveGQLWherePredicatesToRexNodePredicates(
    //                                 builder,
    //                                 innerValue as ObjectValue
    //                             )
    //                         }
    //                     )
    //                 )
    //             }
    //             "_or" -> {
    //                 val orPredicates = it.value as? ArrayValue
    //                     ?: throw IllegalArgumentException("Expected _or to be an array")
    //                 println("OR: ${orPredicates.values}")
    //                 listOf(
    //                     builder.or(
    //                         orPredicates.values.flatMap { innerValue ->
    //                             recursiveGQLWherePredicatesToRexNodePredicates(
    //                                 builder,
    //                                 innerValue as ObjectValue
    //                             )
    //                         }
    //                     )
    //                 )
    //             }
    //             "_not" -> {
    //                 val notPredicate = it.value as? ObjectValue
    //                     ?: throw IllegalArgumentException("Expected _not to be an object")
    //                 listOf(
    //                     builder.not(
    //                         recursiveGQLWherePredicatesToRexNodePredicates(
    //                             builder,
    //                             notPredicate
    //                         ).first()
    //                     )
    //                 )
    //             }
    //             else -> {
    //                 toRexNodes(it, builder)
    //             }
    //         }
    //     }.toList().flatten()
    // }

    // private fun toRexNodes(
    //     it: ObjectField,
    //     builder: RelBuilder
    // ): List<RexNode> {
    //     val firstField: ObjectField = (it.value as? ObjectValue)?.objectFields?.first()
    //         ?: throw IllegalArgumentException("Expected first field")
    //     val value = firstField.value
    //         ?: throw IllegalArgumentException("Value is null")
    //     val predicate = firstField.name.let(ComparisonOperator.Companion::fromValue)
    //     return listOf(
    //         builder.call(
    //             predicate.sqlOperator,
    //             builder.field(it.name),
    //             value.extensions.toRexLiteral(builder)
    //         )
    //     )
    // }
}

