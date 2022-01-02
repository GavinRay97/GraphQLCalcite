package extensions

import graphql.language.Document
import graphql.language.OperationDefinition

fun Document.operationsAssociatedByType(): Map<OperationDefinition.Operation, List<OperationDefinition>> {
    val operations = this.definitions.filterIsInstance<OperationDefinition>()
    return operations.groupBy { it.operation }
}

fun Document.queryOperations(): List<OperationDefinition> {
    return this.operationsAssociatedByType()[OperationDefinition.Operation.QUERY] ?: emptyList()
}

fun Document.mutationOperations(): List<OperationDefinition> {
    return this.operationsAssociatedByType()[OperationDefinition.Operation.MUTATION] ?: emptyList()
}

fun Document.subscriptionOperations(): List<OperationDefinition> {
    return this.operationsAssociatedByType()[OperationDefinition.Operation.SUBSCRIPTION] ?: emptyList()
}
