//import graphql.schema.*;
//
//import static graphql.Scalars.*;
//import static graphql.Scalars.GraphQLString;
//
//abstract class ComparisonBaseType {
//    private final GraphQLScalarType type;
//
//    ComparisonBaseType(GraphQLScalarType type) {
//        this.type = type;
//    }
//
//    public GraphQLScalarType type() {
//        return type;
//    }
//
//    abstract GraphQLObjectType getObjectType(String name);
//
//    GraphQLObjectType getBaseObjectType(String name) {
//        return GraphQLObjectType.newObject()
//                .name(name)
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("_is_null")
//                        .type(GraphQLBoolean))
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("_eq")
//                        .type(type))
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("_gt")
//                        .type(type))
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("_gte")
//                        .type(type))
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("_in")
//                        .type(new GraphQLList(new GraphQLNonNull(type))))
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("_lt")
//                        .type(type))
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("_lte")
//                        .type(type))
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("_neq")
//                        .type(type))
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("_nin")
//                        .type(new GraphQLList(new GraphQLNonNull(type))))
//                .build();
//    }
//}
//
//final class IntBaseType extends ComparisonBaseType {
//    IntBaseType() {
//        super(GraphQLInt);
//    }
//
//    @Override
//    GraphQLObjectType getObjectType(String name) {
//        return super.getBaseObjectType("Int_comparison_exp");
//    }
//}
//
//final class StringBaseType extends ComparisonBaseType {
//    StringBaseType() {
//        super(GraphQLString);
//    }
//
//    @Override
//    GraphQLObjectType getObjectType(String name) {
//        return getBaseObjectType("String_comparison_exp").transform(builder -> {
//            builder.field(GraphQLFieldDefinition.newFieldDefinition()
//                    .name("_ilike")
//                    .description("does the column match the given case-insensitive pattern")
//                    .type(GraphQLString));
//
//            builder.field(GraphQLFieldDefinition.newFieldDefinition()
//                    .name("_iregex")
//                    .description("does the column match the given POSIX regular expression, case insensitive")
//                    .type(GraphQLString));
//
//            builder.field(GraphQLFieldDefinition.newFieldDefinition()
//                    .name("_like")
//                    .description("does the column match the given pattern")
//                    .type(GraphQLString));
//
//            builder.field(GraphQLFieldDefinition.newFieldDefinition()
//                    .name("_nilike")
//                    .description("does the column NOT match the given case-insensitive pattern")
//                    .type(GraphQLString));
//
//            builder.field(GraphQLFieldDefinition.newFieldDefinition()
//                    .name("_niregex")
//                    .description("does the column NOT match the given POSIX regular expression, case insensitive")
//                    .type(GraphQLString));
//
//            builder.field(GraphQLFieldDefinition.newFieldDefinition()
//                    .name("_nlike")
//                    .description("does the column NOT match the given pattern")
//                    .type(GraphQLString));
//
//            builder.field(GraphQLFieldDefinition.newFieldDefinition()
//                    .name("_nregex")
//                    .description("does the column NOT match the given POSIX regular expression, case sensitive")
//                    .type(GraphQLString));
//
//            builder.field(GraphQLFieldDefinition.newFieldDefinition()
//                    .name("_nsimilar")
//                    .description("does the column NOT match the given SQL regular expression")
//                    .type(GraphQLString));
//
//            builder.field(GraphQLFieldDefinition.newFieldDefinition()
//                    .name("_regex")
//                    .description("does the column match the given POSIX regular expression, case sensitive")
//                    .type(GraphQLString));
//
//            builder.field(GraphQLFieldDefinition.newFieldDefinition()
//                    .name("_similar")
//                    .description("does the column match the given SQL regular expression")
//                    .type(GraphQLString));
//        });
//    }
//}
