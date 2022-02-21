package graphql

import graphql.schema.DataFetcher
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLEnumValueDefinition
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLList
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import graphql.schema.GraphQLTypeReference
import graphql.schema.GraphQLUnionType

inline fun field(block: GraphQLFieldDefinitionBuilder.() -> Unit): GraphQLFieldDefinition {
    return GraphQLFieldDefinitionBuilder().apply(block).build()
}

class GraphQLFieldDefinitionBuilder {
    var name: String? = null
    var type: GraphQLOutputType? = null
    var arguments: List<GraphQLArgument>? = null
    var dataFetcher: DataFetcher<*>? = null
    var description: String? = null

    fun build(): GraphQLFieldDefinition {
        return GraphQLFieldDefinition
            .newFieldDefinition()
            .apply {
                if (name != null) name(name)
                if (type != null) type(type)
                if (arguments != null) arguments(arguments)
                if (dataFetcher != null) dataFetcher(dataFetcher)
                if (description != null) description(description)
            }
            .build()
    }
}

inline fun argument(block: GraphQLArgumentBuilder.() -> Unit): GraphQLArgument {
    return GraphQLArgumentBuilder().apply(block).build()
}

class GraphQLArgumentBuilder {
    var name: String? = null
    var type: GraphQLInputType? = null
    var description: String? = null

    fun build(): GraphQLArgument {
        return GraphQLArgument
            .newArgument()
            .apply {
                if (name != null) name(name)
                if (type != null) type(type)
                if (description != null) description(description)
            }
            .build()
    }
}

inline fun objectType(block: GraphQLObjectTypeBuilder.() -> Unit): GraphQLObjectType {
    return GraphQLObjectTypeBuilder().apply(block).build()
}

class GraphQLObjectTypeBuilder {
    var name: String? = null
    var description: String? = null
    var fields: List<GraphQLFieldDefinition>? = null

    fun build(): GraphQLObjectType {
        return GraphQLObjectType
            .newObject()
            .apply {
                if (name != null) name(name)
                if (description != null) description(description)
                if (fields != null) fields(fields)
            }
            .build()
    }
}

inline fun inputObjectType(block: GraphQLInputObjectTypeBuilder.() -> Unit): GraphQLInputObjectType {
    return GraphQLInputObjectTypeBuilder().apply(block).build()
}

class GraphQLInputObjectTypeBuilder {
    var name: String? = null
    var description: String? = null
    var fields: List<GraphQLInputObjectField>? = null

    fun build(): GraphQLInputObjectType {
        return GraphQLInputObjectType
            .newInputObject()
            .apply {
                println("name: $name")
                println("description: $description")
                println("fields: $fields")
                if (name != null) name(name)
                if (description != null) description(description)
                if (fields != null) fields(fields)
            }
            .build()
    }
}

inline fun inputObjectField(block: GraphQLInputObjectFieldBuilder.() -> Unit): GraphQLInputObjectField {
    return GraphQLInputObjectFieldBuilder().apply(block).build()
}

class GraphQLInputObjectFieldBuilder {
    var name: String? = null
    var type: GraphQLInputType? = null
    var description: String? = null

    fun build(): GraphQLInputObjectField {
        return GraphQLInputObjectField
            .newInputObjectField()
            .apply {
                if (name != null) name(name)
                if (type != null) type(type)
                if (description != null) description(description)
            }
            .build()
    }
}

inline fun scalarType(block: GraphQLScalarTypeBuilder.() -> Unit): GraphQLScalarType {
    return GraphQLScalarTypeBuilder().apply(block).build()
}

class GraphQLScalarTypeBuilder {
    var name: String? = null
    var description: String? = null

    fun build(): GraphQLScalarType {
        return GraphQLScalarType
            .newScalar()
            .apply {
                if (name != null) name(name)
                if (description != null) description(description)
            }
            .build()
    }
}

inline fun enumType(block: GraphQLEnumTypeBuilder.() -> Unit): GraphQLEnumType {
    return GraphQLEnumTypeBuilder().apply(block).build()
}

class GraphQLEnumTypeBuilder {
    var name: String? = null
    var description: String? = null
    var values: List<GraphQLEnumValueDefinition>? = null

    fun build(): GraphQLEnumType {
        return GraphQLEnumType
            .newEnum()
            .apply {
                if (name != null) name(name)
                if (description != null) description(description)
                if (values != null) values(values)
            }
            .build()
    }
}

inline fun interfaceType(block: GraphQLInterfaceTypeBuilder.() -> Unit): GraphQLInterfaceType {
    return GraphQLInterfaceTypeBuilder().apply(block).build()
}

class GraphQLInterfaceTypeBuilder {
    var name: String? = null
    var description: String? = null
    var fields: List<GraphQLFieldDefinition>? = null

    fun build(): GraphQLInterfaceType {
        return GraphQLInterfaceType
            .newInterface()
            .apply {
                if (name != null) name(name)
                if (description != null) description(description)
                if (fields != null) fields(fields)
            }
            .build()
    }
}

inline fun unionType(block: GraphQLUnionTypeBuilder.() -> Unit): GraphQLUnionType {
    return GraphQLUnionTypeBuilder().apply(block).build()
}

class GraphQLUnionTypeBuilder {
    var name: String? = null
    var description: String? = null
    var objectTypes: List<GraphQLObjectType>? = null
    var typeReferences: List<GraphQLTypeReference>? = null

    fun build(): GraphQLUnionType {
        return GraphQLUnionType
            .newUnionType()
            .apply {
                if (name != null) name(name)
                if (description != null) description(description)
                if (objectTypes != null) possibleTypes(*objectTypes.orEmpty().toTypedArray())
                if (typeReferences != null) possibleTypes(*typeReferences.orEmpty().toTypedArray())
            }
            .build()
    }
}

inline fun graphqlSchema(block: GraphQLSchemaBuilder.() -> Unit): GraphQLSchema {
    return GraphQLSchemaBuilder().apply(block).build()
}

class GraphQLSchemaBuilder {
    var query: GraphQLObjectType? = null
    var mutation: GraphQLObjectType? = null
    var subscription: GraphQLObjectType? = null
    var directives: Set<GraphQLDirective>? = emptySet()
    var scalars: Set<GraphQLScalarType>? = emptySet()

    fun build(): GraphQLSchema {
        return GraphQLSchema
            .newSchema()
            .apply {
                if (query != null) query(query)
                if (mutation != null) mutation(mutation)
                if (subscription != null) subscription(subscription)
                if (directives != null) additionalDirectives(directives)
                if (scalars != null) additionalTypes(scalars)
            }
            .build()
    }
}

inline fun graphqlList(type: () -> GraphQLType): GraphQLList {
    return GraphQLList(type.invoke())
}

inline fun graphqlNonNull(type: () -> GraphQLType): GraphQLNonNull {
    return GraphQLNonNull(type.invoke())
}







