// Copyright 2015 Denis Itskovich
// Refer to LICENSE.txt for license details
package com.slimgears.slimrepo.apt;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.slimgears.slimrepo.apt.base.DataModelGenerator;
import com.slimgears.slimrepo.apt.base.TypeUtils;
import com.slimgears.slimrepo.core.annotations.BlobSemantics;
import com.slimgears.slimrepo.core.annotations.ComparableSemantics;
import com.slimgears.slimrepo.core.annotations.GenerateEntity;
import com.slimgears.slimrepo.core.annotations.Key;
import com.slimgears.slimrepo.core.annotations.ValueSemantics;
import com.slimgears.slimrepo.core.interfaces.entities.Entity;
import com.slimgears.slimrepo.core.interfaces.entities.EntityBuilder;
import com.slimgears.slimrepo.core.interfaces.entities.EntityType;
import com.slimgears.slimrepo.core.interfaces.entities.FieldValueLookup;
import com.slimgears.slimrepo.core.interfaces.entities.FieldValueMap;
import com.slimgears.slimrepo.core.interfaces.fields.BlobField;
import com.slimgears.slimrepo.core.interfaces.fields.ComparableField;
import com.slimgears.slimrepo.core.interfaces.fields.RelationalField;
import com.slimgears.slimrepo.core.interfaces.fields.StringField;
import com.slimgears.slimrepo.core.interfaces.fields.ValueField;
import com.slimgears.slimrepo.core.internal.AbstractEntityType;
import com.slimgears.slimrepo.core.internal.Fields;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypesException;

import static com.google.common.collect.Iterables.find;
import static com.google.common.collect.Iterables.transform;

/**
 * Created by Denis on 21-Apr-15
 * <File Description>
 */
public class EntityGenerator extends DataModelGenerator {
    private static final Map<TypeName, AbstractMetaFieldBuilder> META_FIELD_BUILDER_MAP = new HashMap<>();

    static {
        META_FIELD_BUILDER_MAP.put(TypeName.INT, ComparableMetaFieldBuilder.INSTANCE);
        META_FIELD_BUILDER_MAP.put(TypeName.SHORT, ComparableMetaFieldBuilder.INSTANCE);
        META_FIELD_BUILDER_MAP.put(TypeName.LONG, ComparableMetaFieldBuilder.INSTANCE);
        META_FIELD_BUILDER_MAP.put(TypeName.BYTE, ComparableMetaFieldBuilder.INSTANCE);
        META_FIELD_BUILDER_MAP.put(TypeName.DOUBLE, ComparableMetaFieldBuilder.INSTANCE);
        META_FIELD_BUILDER_MAP.put(TypeName.FLOAT, ComparableMetaFieldBuilder.INSTANCE);
        META_FIELD_BUILDER_MAP.put(TypeName.get(Date.class), ComparableMetaFieldBuilder.INSTANCE);
        META_FIELD_BUILDER_MAP.put(TypeName.get(String.class), StringMetaFieldBuilder.INSTANCE);
    }

    private static TypeUtils.AnnotationTypesGetter<ComparableSemantics> TYPES_FROM_COMPARABLE_SEMANTICS = new TypeUtils.AnnotationTypesGetter<ComparableSemantics>() {
        @Override
        public Class[] getTypes(ComparableSemantics annotation) throws MirroredTypesException {
            return annotation.value();
        }
    };

    private static TypeUtils.AnnotationTypesGetter<ValueSemantics> TYPES_FROM_VALUE_SEMANTICS = new TypeUtils.AnnotationTypesGetter<ValueSemantics>() {
        @Override
        public Class[] getTypes(ValueSemantics annotation) throws MirroredTypesException {
            return annotation.value();
        }
    };

    private static TypeUtils.AnnotationTypesGetter<BlobSemantics> TYPES_FROM_BLOB_SEMANTICS = new TypeUtils.AnnotationTypesGetter<BlobSemantics>() {
        @Override
        public Class[] getTypes(BlobSemantics annotation) throws MirroredTypesException {
            return annotation.value();
        }
    };

    static abstract class AbstractMetaFieldBuilder {
        public FieldSpec build(ClassName entityType, FieldInfo field) {
            TypeName type = metaFieldType(entityType, field.type);
            FieldSpec.Builder builder = FieldSpec.builder(type, getMetaFieldName(field.name), Modifier.STATIC, Modifier.PUBLIC, Modifier.FINAL);
            return initialize(builder, field, !field.type.isPrimitive()).build();
        }

        protected abstract TypeName metaFieldType(ClassName entityType, TypeName fieldType);
        protected abstract FieldSpec.Builder initialize(FieldSpec.Builder builder, FieldInfo field, boolean isNullable);
    }

    static class ValueMetaFieldBuilder extends AbstractMetaFieldBuilder {
        static final ValueMetaFieldBuilder INSTANCE = new ValueMetaFieldBuilder();

        @Override
        protected TypeName metaFieldType(ClassName entityType, TypeName fieldType) {
            return ParameterizedTypeName.get(ClassName.get(ValueField.class), entityType, box(fieldType));
        }

        @Override
        protected FieldSpec.Builder initialize(FieldSpec.Builder builder, FieldInfo field, boolean isNullable) {
            return builder.initializer("$T.valueField($S, $T.class, $L)", Fields.class, field.name, box(field.type), isNullable);
        }
    }

    static class ComparableMetaFieldBuilder extends AbstractMetaFieldBuilder {
        static final ComparableMetaFieldBuilder INSTANCE = new ComparableMetaFieldBuilder();

        @Override
        protected TypeName metaFieldType(ClassName entityType, TypeName fieldType) {
            return ParameterizedTypeName.get(ClassName.get(ComparableField.class), entityType, box(fieldType));
        }

        @Override
        protected FieldSpec.Builder initialize(FieldSpec.Builder builder, FieldInfo field, boolean isNullable) {
            return builder.initializer("$T.comparableField($S, $T.class, $L)", Fields.class, field.name, box(field.type), isNullable);
        }
    }

    static class StringMetaFieldBuilder extends AbstractMetaFieldBuilder {
        static final StringMetaFieldBuilder INSTANCE = new StringMetaFieldBuilder();

        @Override
        protected TypeName metaFieldType(ClassName entityType, TypeName fieldType) {
            return ParameterizedTypeName.get(ClassName.get(StringField.class), entityType);
        }

        @Override
        protected FieldSpec.Builder initialize(FieldSpec.Builder builder, FieldInfo field, boolean isNullable) {
            return builder.initializer("$T.stringField($S, $L)", Fields.class, field.name, isNullable);
        }
    }

    static class BlobMetaFieldBuilder extends AbstractMetaFieldBuilder {
        static final BlobMetaFieldBuilder INSTANCE = new BlobMetaFieldBuilder();

        @Override
        protected TypeName metaFieldType(ClassName entityType, TypeName fieldType) {
            return ParameterizedTypeName.get(ClassName.get(BlobField.class), entityType, fieldType);
        }

        @Override
        protected FieldSpec.Builder initialize(FieldSpec.Builder builder, FieldInfo field, boolean isNullable) {
            return builder.initializer("$T.blobField($S, $T.class, $L)", Fields.class, field.name, field.type, isNullable);
        }
    }

    static class RelationalMetaFieldBuilder extends AbstractMetaFieldBuilder {
        static final RelationalMetaFieldBuilder INSTANCE = new RelationalMetaFieldBuilder();

        @Override
        protected TypeName metaFieldType(ClassName entityType, TypeName fieldType) {
            return ParameterizedTypeName.get(ClassName.get(RelationalField.class), entityType, fieldType);
        }

        @Override
        protected FieldSpec.Builder initialize(FieldSpec.Builder builder, FieldInfo field, boolean isNullable) {
            return builder.initializer("$T.relationalField($S, $T.EntityMetaType, $L)", Fields.class, field.name, field.type, isNullable);
        }
    }

    private Map<TypeName, AbstractMetaFieldBuilder> metaFieldBuilderMap = new HashMap<>(META_FIELD_BUILDER_MAP);

    public EntityGenerator(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
    }

    @Override
    public EntityGenerator superClass(TypeName superClass) {
        super.superClass(superClass);
        className(TypeUtils.packageName(superClass.toString()), generateEntityTypeName(superClass));
        return this;
    }

    private String generateEntityTypeName(TypeName superClass) {
        return TypeUtils.simpleName(superClass.toString()).replace("Abstract", "");
    }

    private TypeName entityTypeFromAbstract(TypeName superClass) {
        String simpleName = generateEntityTypeName(superClass);
        String packageName = TypeUtils.packageName(superClass.toString());
        return ClassName.get(packageName, simpleName);
    }

    @Override
    protected TypeSpec.Builder createModelBuilder(String name) {
        return super.createModelBuilder(name)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(EntityBuilder.class), ClassName.get(getPackageName(), getClassName())));
    }

    @Override
    protected void build(TypeSpec.Builder builder, TypeElement type, List<FieldInfo> fields) {
        mapFieldTypesFromAnnotation(type, ComparableSemantics.class, TYPES_FROM_COMPARABLE_SEMANTICS, ComparableMetaFieldBuilder.INSTANCE);
        mapFieldTypesFromAnnotation(type, BlobSemantics.class, TYPES_FROM_BLOB_SEMANTICS, BlobMetaFieldBuilder.INSTANCE);
        mapFieldTypesFromAnnotation(type, ValueSemantics.class, TYPES_FROM_VALUE_SEMANTICS, ValueMetaFieldBuilder.INSTANCE);

        FieldInfo keyField = getKeyField(fields);
        fields.remove(keyField);
        fields.add(0, keyField);

        TypeName keyType = box(keyField.type);
        ClassName entityType = getTypeName();
        String keyFieldName = keyField.name;

        for (FieldInfo field : fields) {
            builder.addField(buildMetaField(entityType, field));
        }

        builder
            .addSuperinterface(ParameterizedTypeName.get(ClassName.get(Entity.class), keyType))
            .addMethod(MethodSpec.methodBuilder("getEntityId")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(keyType)
                .addCode("return this.$L;\n", keyFieldName)
                .build())
            .addField(FieldSpec
                .builder(ParameterizedTypeName.get(ClassName.get(EntityType.class), keyType, entityType), "EntityMetaType", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new MetaType()")
                .build())
            .addType(createMetaType(keyField, keyType, fields));

        super.build(builder, type, fields);
    }

    @Override
    protected FieldInfo createFieldInfo(VariableElement element) {
        FieldInfo field = new FieldInfo(element);
        if (isRelationalField(field)) field = field.replaceType(entityTypeFromAbstract(field.type));
        return field;
    }

    private TypeSpec createMetaType(FieldInfo keyField, TypeName keyType, Iterable<FieldInfo> fields) {
        ClassName entityType = getTypeName();

        return TypeSpec.classBuilder("MetaType")
            .superclass(ParameterizedTypeName.get(ClassName.get(AbstractEntityType.class), keyType, getTypeName()))
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addMethod(MethodSpec.constructorBuilder()
                .addCode("super($S, $T.class, ", getClassName(), getTypeName())
                .addCode(Joiner
                        .on(", ")
                        .join(transform(fields, new Function<FieldInfo, String>() {
                            @Override
                            public String apply(FieldInfo field) {
                                return getMetaFieldName(field.name);
                            }
                        })))
                .addCode(");\n")
                .build())
            .addMethod(MethodSpec.methodBuilder("newInstance")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(entityType)
                .addCode("return new $T();\n", entityType)
                .build())
            .addMethod(MethodSpec.methodBuilder("setKey")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(entityType, "entity")
                .addParameter(keyType, "key")
                .addCode("entity.$L(key);\n", getModelSetterName(keyField.name))
                .build())
            .addMethod(MethodSpec.methodBuilder("newInstance")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(entityType)
                .addParameter(ParameterizedTypeName.get(ClassName.get(FieldValueLookup.class), entityType), "lookup")
                .addCode("return new $T(\n", entityType)
                .addCode(Joiner.on(",\n").join(transform(fields, new Function<FieldInfo, String>() {
                    @Override
                    public String apply(FieldInfo field) {
                        return "    lookup.getValue(" + getMetaFieldName(field.name) + ")";
                    }
                })))
                .addCode(");\n")
                .build())
            .addMethod(MethodSpec.methodBuilder("entityToMap")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(entityType, "entity")
                .addParameter(ParameterizedTypeName.get(ClassName.get(FieldValueMap.class), entityType), "map")
                .addCode("map\n")
                .addCode(Joiner.on("\n").join(transform(fields, new Function<FieldInfo, String>() {
                    @Override
                    public String apply(FieldInfo field) {
                        return "    .putValue(" + getMetaFieldName(field.name) + ", entity." + getModelGetterName(field.name) + "())";
                    }
                })))
                .addCode(";\n")
                .build())
            .build();
    }

    private static String getMetaFieldName(String fieldName) {
        return TypeUtils.toCamelCase("", fieldName);
    }

    private FieldInfo getKeyField(Iterable<FieldInfo> fields) {
        FieldInfo field = findAnnotatedField(fields, Key.class);
        return field != null ? field : findFieldByName(fields, "id", getEntityIdFieldName());
    }

    private String getEntityIdFieldName() {
        return TypeUtils.toCamelCase(getClassName().replace("Entity", ""), "Id");
    }

    private FieldInfo findFieldByName(Iterable<FieldInfo> fields, String... names) {
        final Set<String> nameSet = new HashSet<>(Arrays.asList(names));
        return find(fields, new Predicate<FieldInfo>() {
            @Override
            public boolean apply(FieldInfo field) {
                return nameSet.contains(field.name);
            }
        });
    }

    private FieldInfo findAnnotatedField(Iterable<FieldInfo> fields, final Class annotationClass) {
        return find(fields, new Predicate<FieldInfo>() {
            @Override
            public boolean apply(FieldInfo input) {
                return input.element.getAnnotation(annotationClass) != null;
            }
        }, null);
    }

    private static TypeName box(TypeName type) {
        if (type == TypeName.INT) return TypeName.get(Integer.class);
        if (type == TypeName.SHORT) return TypeName.get(Short.class);
        if (type == TypeName.LONG) return TypeName.get(Long.class);
        if (type == TypeName.BOOLEAN) return TypeName.get(Boolean.class);
        if (type == TypeName.DOUBLE) return TypeName.get(Double.class);
        if (type == TypeName.FLOAT) return TypeName.get(Float.class);
        if (type == TypeName.BYTE) return TypeName.get(Byte.class);
        return type;
    }

    private FieldSpec buildMetaField(ClassName entityType, FieldInfo field) {
        return getMetaFieldBuilder(field).build(entityType, field);
    }

    private AbstractMetaFieldBuilder getMetaFieldBuilder(FieldInfo field) {
        if (isRelationalField(field)) return RelationalMetaFieldBuilder.INSTANCE;
        if (isEnumField(field)) return ComparableMetaFieldBuilder.INSTANCE;
        AbstractMetaFieldBuilder builder = metaFieldBuilderMap.get(field.type);
        return builder != null ? builder : BlobMetaFieldBuilder.INSTANCE;
    }

    private boolean isEnumField(FieldInfo field) {
        TypeElement type = typeElementForField(field);
        return type != null && type.getKind() == ElementKind.ENUM;
    }

    private boolean isRelationalField(FieldInfo field) {
        TypeElement type = typeElementForField(field);
        return (type != null) && type.getAnnotation(GenerateEntity.class) != null;
    }

    private TypeElement typeElementForField(FieldInfo field) {
        return getElementUtils().getTypeElement(field.element.asType().toString());
    }

    private <TAnnotation extends Annotation> void mapFieldTypesFromAnnotation(TypeElement typeElement, Class<TAnnotation> annotationType, TypeUtils.AnnotationTypesGetter<TAnnotation> getter, AbstractMetaFieldBuilder builder) {
        TAnnotation annotation = typeElement.getAnnotation(annotationType);
        if (annotation == null) return;

        for (TypeName type : TypeUtils.getTypesFromAnnotation(annotation, getter)) {
            metaFieldBuilderMap.put(type, builder);
        }
    }
}
