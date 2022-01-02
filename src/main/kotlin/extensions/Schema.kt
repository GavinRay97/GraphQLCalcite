package extensions

import JAVA_TYPE_FACTORY_IMPL
import argument
import com.google.common.collect.ImmutableList
import field
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLList.list
import graphql.schema.GraphQLNonNull.nonNull
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLTypeReference.typeRef
import graphqlSchema
import inputObjectField
import inputObjectType
import objectType
import org.apache.calcite.plan.RelOptPlanner
import org.apache.calcite.plan.RelOptSchema
import org.apache.calcite.plan.RelOptTable
import org.apache.calcite.prepare.RelOptTableImpl
import org.apache.calcite.rel.type.RelDataTypeFactory
import org.apache.calcite.rel.type.RelDataTypeField
import org.apache.calcite.schema.Schema

fun Schema.toRelOptSchema(): RelOptSchema {
    val schema = this
    return object : RelOptSchema {
        override fun getTableForMember(names: MutableList<String>): RelOptTable? {
            val fullyQualifiedTableName = names.joinToString(".")
            val table = schema.getTable(fullyQualifiedTableName) ?: return null
            return RelOptTableImpl.create(
                this,
                table.getRowType(JAVA_TYPE_FACTORY_IMPL),
                table,
                ImmutableList.copyOf(names)
            )
        }

        override fun getTypeFactory(): RelDataTypeFactory {
            return JAVA_TYPE_FACTORY_IMPL
        }

        override fun registerRules(planner: RelOptPlanner) {
            return
        }
    }
}

fun Schema.toGraphQLSchema(): GraphQLSchema {
    val schema = this

    return graphqlSchema {
        query = objectType {
            name = "Query"
            fields = schema.tableNames.map { tableName: String ->
                with(schema.getTable(tableName)?.getRowType(JAVA_TYPE_FACTORY_IMPL)?.fieldList) {
                    val fieldList = this
                    field {
                        name = tableName
                        type = objectType {
                            name = tableName
                            fields = fieldList?.map { field: RelDataTypeField ->
                                field {
                                    name = field.name
                                    type = field.type.toGraphQLType() as GraphQLOutputType
                                }
                            }
                            arguments = listOf(
                                argument {
                                    name = "limit"
                                    type = graphql.Scalars.GraphQLInt
                                },
                                argument {
                                    name = "offset"
                                    type = graphql.Scalars.GraphQLInt
                                },
                                argument {
                                    name = "order_by"
                                    type = graphql.Scalars.GraphQLString
                                },
                                argument {
                                    name = "where"
                                    type = inputObjectType {
                                        name = tableName + "_bool_exp"
                                        fields = fieldList?.map { field: RelDataTypeField ->
                                            inputObjectField {
                                                name = field.name
                                                type = field.type.toGraphQLComparisonExprType() as GraphQLInputType
                                            }
                                        }?.plus(
                                            listOf(
                                                inputObjectField {
                                                    name = "_and"
                                                    type = list(
                                                        nonNull(
                                                            typeRef(tableName + "_bool_exp")
                                                        )
                                                    )
                                                },
                                                inputObjectField {
                                                    name = "_or"
                                                    type = list(nonNull(typeRef(tableName + "_bool_exp")))
                                                },
                                                inputObjectField {
                                                    name = "_not"
                                                    type = typeRef(tableName + "_bool_exp")
                                                },
                                            )
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
