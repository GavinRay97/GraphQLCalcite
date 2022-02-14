package com.example;

import com.example.calcitewrappers.*;
import graphql.schema.*;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.type.SqlTypeFamily;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static graphql.Scalars.GraphQLInt;
import static java.util.Objects.requireNonNull;


abstract class GraphQLOperationsProvider {
    protected final GraphQLSchemaGenerationContext context;

    GraphQLOperationsProvider(GraphQLSchemaGenerationContext context) {
        this.context = context;
    }

    public abstract List<GraphQLFieldDefinition> getQueryFields();

    public abstract List<GraphQLFieldDefinition> getMutationFields();

    public abstract List<GraphQLFieldDefinition> getSubscriptionFields();
}

class GraphQLFindAllQueryProvider extends GraphQLOperationsProvider {
    GraphQLFindAllQueryProvider(GraphQLSchemaGenerationContext context) {
        super(context);
    }

    public List<GraphQLFieldDefinition> getQueryFields() {
        return context.getDatabaseManager().getDatabases().stream()
                .map(this::convertDatabase)
                .toList();
    }

    public List<GraphQLFieldDefinition> getMutationFields() {
        return List.of();
    }

    public List<GraphQLFieldDefinition> getSubscriptionFields() {
        return List.of();
    }

    private GraphQLFieldDefinition convertDatabase(Database database) {
        return GraphQLFieldDefinition.newFieldDefinition()
                .name(database.name())
                .dataFetcher((env) -> "continue")
                .type(GraphQLObjectType.newObject()
                        .name(database.name())
                        .fields(database.schemas().stream().map(this::convertSchema).toList())
                        .fields(database.tables().stream().map(this::convertTable).toList())
                        .build()
                ).build();
    }

    private GraphQLFieldDefinition convertSchema(Schema schema) {
        return GraphQLFieldDefinition.newFieldDefinition()
                .name(schema.name())
                .dataFetcher((env) -> "continue")
                .type(GraphQLObjectType.newObject()
                        .name(schema.databaseName() + "_" + schema.name() + "_type")
                        .fields(schema.tables().stream().map(this::convertTable).toList())
                        .build())
                .build();
    }

    private GraphQLFieldDefinition convertTable(TableRecord table) {
        SchemaPlus schema = table.underlyingTable().schema();
        SchemaPlus parentSchema = schema.getParentSchema();

        String qualifiedName = parentSchema == null
                ? schema.getName() + "_" + table.name()
                : parentSchema.getName() + "_" + schema.getName() + "_" + table.name();

        FullyQualifiedTableName fullyQualifiedTableName = new FullyQualifiedTableName(
                table.getDatabase().getName(),
                table.getSchema() == null ? null : table.getSchema().getName(),
                table.name()
        );

        TableDataFetcher dataFetcher = new TableDataFetcher(fullyQualifiedTableName, context.getSqlToGraphQLConfiguration(), context.getDatabaseManager());

        List<GraphQLFieldDefinition> fields = new ArrayList<>(table.columns().stream().map(this::convertColumn).toList());
        fields.addAll(buildOutgoingForeignKeyFields(fullyQualifiedTableName));
        fields.addAll(buildIncomingForeignKeyFields(fullyQualifiedTableName));

        return GraphQLFieldDefinition.newFieldDefinition()
                .name(table.name())
                .dataFetcher(dataFetcher)
                .type(new GraphQLList(
                        GraphQLObjectType.newObject()
                                .name(qualifiedName + "_type")
                                .fields(fields)
                                .build()))
                .arguments(getTableFieldQueryArguments(table))
                .build();
    }

    private GraphQLFieldDefinition convertColumn(Column column) {
        return GraphQLFieldDefinition.newFieldDefinition()
                .name(column.name())
                .type(sqlTypeFamilyToGraphQLScalar(
                        (SqlTypeFamily) column.underlyingColumn().getType().getFamily()))
                .build();
    }

    private List<GraphQLArgument> getTableFieldQueryArguments(TableRecord table) {
        SchemaPlus schema = table.underlyingTable().schema();
        SchemaPlus parentSchema = schema.getParentSchema();
        SchemaPlus database = parentSchema == null ? schema : parentSchema;
        String schemaNameText = schema.getName();
        String boolExpTypeName = database.getName() + schemaNameText + "_" + table.name() + "_bool_exp";

        GraphQLArgument limit = GraphQLArgument.newArgument()
                .name("limit")
                .type(GraphQLInt)
                .build();

        GraphQLArgument offset = GraphQLArgument.newArgument()
                .name("offset")
                .type(GraphQLInt)
                .build();

        GraphQLArgument where = GraphQLArgument.newArgument()
                .name("where")
                .type(
                        GraphQLInputObjectType.newInputObject()
                                .name(boolExpTypeName)
                                .fields(
                                        table.columns().stream()
                                                .map(f -> GraphQLInputObjectField
                                                        .newInputObjectField()
                                                        .name(f.name())
                                                        .type(sqlTypeFamilyToGraphQLInputType(
                                                                (SqlTypeFamily) f
                                                                        .underlyingColumn()
                                                                        .getType()
                                                                        .getFamily()))
                                                        .build())
                                                .toList())
                                .field(
                                        GraphQLInputObjectField
                                                .newInputObjectField()
                                                .name("_and")
                                                .type(new GraphQLList(
                                                        new GraphQLNonNull(
                                                                new GraphQLTypeReference(
                                                                        boolExpTypeName))))
                                                .build())
                                .field(
                                        GraphQLInputObjectField
                                                .newInputObjectField()
                                                .name("_or")
                                                .type(new GraphQLList(
                                                        new GraphQLNonNull(
                                                                new GraphQLTypeReference(
                                                                        boolExpTypeName))))
                                                .build())
                                .field(
                                        GraphQLInputObjectField
                                                .newInputObjectField()
                                                .name("_not")
                                                .type(new GraphQLTypeReference(
                                                        boolExpTypeName))
                                                .build())
                                .build())
                .build();

        return List.of(limit, offset, where);
    }

    private List<GraphQLFieldDefinition> buildIncomingForeignKeyFields(FullyQualifiedTableName table) {
        Set<ForeignKey> foreignKeys = ForeignKeyManager.getForeignKeysForTable(table);
        List<ForeignKey> incomingForeignKeys = foreignKeys.stream().filter(fk -> fk.targetTable().equals(table)).toList();
        return foreignKeys.stream().map(fk -> {
            System.out.println(table);
            System.out.println("Incoming foreign key: " + fk.sourceTable().table());

            return GraphQLFieldDefinition.newFieldDefinition()
                    .name(fk.sourceTable().table())
                    .type(
                            new GraphQLNonNull(
                                    new GraphQLList(
                                            new GraphQLNonNull(
                                                    new GraphQLTypeReference(
                                                            fk.sourceTable().database() + "_" +
                                                                    (fk.sourceTable().schema() == null ? "" : fk.sourceTable().schema())
                                                                    + "_" + fk.sourceTable().table() + "_type"
                                                    )))))
                    .build();
        }).toList();
    }

    private List<GraphQLFieldDefinition> buildOutgoingForeignKeyFields(FullyQualifiedTableName table) {
        Set<ForeignKey> foreignKeys = ForeignKeyManager.getForeignKeysForTable(table);
        List<ForeignKey> outgoingForeignKeys = foreignKeys.stream().filter(fk -> fk.sourceTable().equals(table)).toList();
        return outgoingForeignKeys.stream().map(fk -> {
            System.out.println(table);
            System.out.println("Outgoing foreign key: " + fk.sourceTable().table());

            return GraphQLFieldDefinition.newFieldDefinition()
                    .name(fk.targetTable().table())
                    .type(new GraphQLTypeReference(
                            fk.targetTable().database() + "_" +
                                    (fk.targetTable().schema() == null ? "" : fk.targetTable().schema())
                                    + "_" + fk.targetTable().table() + "_type"
                    ))
                    .build();
        }).toList();
    }

    private GraphQLScalarType sqlTypeFamilyToGraphQLScalar(SqlTypeFamily family) {
        GraphQLScalarType entry = context.getSqlToGraphQLConfiguration()
                .getSqlTypeFamilyGraphQLScalarTypeMap()
                .get(family);

        if (entry != null) {
            return entry;
        }

        throw new IllegalArgumentException("Unknown type: " + family);
    }

    private GraphQLInputType sqlTypeFamilyToGraphQLInputType(SqlTypeFamily family) {
        GraphQLScalarType entry = requireNonNull(context.getSqlToGraphQLConfiguration()
                .getSqlTypeFamilyGraphQLScalarTypeMap()
                .get(family));

        GraphQLInputTypeForScalar graphQLInputTypeForScalar = context.getSqlToGraphQLConfiguration()
                .getGraphqlScalarTypeToGraphQLInputTypeMap()
                .get(entry);

        GraphQLInputType inputType = graphQLInputTypeForScalar.getInputObjectType();
        if (inputType == null) {
            throw new IllegalArgumentException("Unknown type" + family);
        }

        return inputType;
    }
}


// Contains the information needed when generating GraphQL Schema operations
interface GraphQLSchemaGenerationContext {
    SqlToGraphQLConfiguration getSqlToGraphQLConfiguration();

    DatabaseManager getDatabaseManager();
}

class SchemaGenerationProvider implements GraphQLSchemaGenerationContext {
    private final SqlToGraphQLConfiguration sqlToGraphQLConfiguration;
    private final DatabaseManager databaseManager;
    private List<GraphQLOperationsProvider> operationProviders;

    public SchemaGenerationProvider(DatabaseManager databaseManager,
                                    SqlToGraphQLConfiguration sqlToGraphQLConfiguration) {
        this.databaseManager = databaseManager;
        this.sqlToGraphQLConfiguration = sqlToGraphQLConfiguration;
    }

    public void setOperationProviders(List<GraphQLOperationsProvider> operationProviders) {
        this.operationProviders = operationProviders;
    }

    public void addOperationProvider(GraphQLOperationsProvider operationProvider) {
        if (operationProviders == null) {
            operationProviders = new ArrayList<>();
        }
        operationProviders.add(operationProvider);
    }

    public GraphQLSchema buildSchema() {
        GraphQLSchema.Builder builder = GraphQLSchema.newSchema();

        GraphQLObjectType.Builder queryTypeBuilder = GraphQLObjectType.newObject().name("Query");
        GraphQLObjectType.Builder mutationTypeBuilder = GraphQLObjectType.newObject().name("Mutation");
        GraphQLObjectType.Builder subscriptionTypeBuilder = GraphQLObjectType.newObject().name("Subscription");

        for (GraphQLOperationsProvider operationProvider : operationProviders) {
            operationProvider.getQueryFields().forEach(queryTypeBuilder::field);
            operationProvider.getMutationFields().forEach(mutationTypeBuilder::field);
            operationProvider.getSubscriptionFields().forEach(subscriptionTypeBuilder::field);
        }

        GraphQLObjectType queryType = queryTypeBuilder.build();
        GraphQLObjectType mutationType = mutationTypeBuilder.build();
        GraphQLObjectType subscriptionType = subscriptionTypeBuilder.build();

        if (!queryType.getFieldDefinitions().isEmpty()) {
            builder.query(queryType);
        }
        if (!mutationType.getFieldDefinitions().isEmpty()) {
            builder.mutation(mutationType);
        }
        if (!subscriptionType.getFieldDefinitions().isEmpty()) {
            builder.subscription(subscriptionType);
        }

        return builder.build();
    }

    @Override
    public SqlToGraphQLConfiguration getSqlToGraphQLConfiguration() {
        return sqlToGraphQLConfiguration;
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
