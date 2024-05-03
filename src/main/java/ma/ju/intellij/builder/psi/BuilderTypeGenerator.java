package ma.ju.intellij.builder.psi;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiVariable;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class BuilderTypeGenerator {
  private final PsiClass recordClass;
  private final PsiClass builderClass;
  private final Field[] components;
  private final PsiElementFactory elementFactory;
  private final BuilderSettings settings;
  private final BuilderDescriptor descriptor;

  private static final Map<String, TypeName> PRIMITIVES =
      Map.of(
          "void", TypeName.VOID,
          "boolean", TypeName.BOOLEAN,
          "byte", TypeName.BYTE,
          "short", TypeName.SHORT,
          "int", TypeName.INT,
          "long", TypeName.LONG,
          "char", TypeName.CHAR,
          "float", TypeName.FLOAT,
          "double", TypeName.DOUBLE);

  private final TypeSpec.Builder builder;

  private BuilderTypeGenerator(
      PsiClass recordClass, Field[] components, PsiClass builderClass, BuilderSettings settings) {
    this.recordClass = recordClass;
    this.components = components;
    this.builderClass = builderClass;
    this.elementFactory = JavaPsiFacade.getElementFactory(recordClass.getProject());
    this.settings = settings;
    this.descriptor = BuilderDescriptor.from(builderClass, recordClass);
    this.builder =
        TypeSpec.classBuilder("Builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

    if (builderClass != null) {
      if (descriptor.methods().containsKey("build")) {
        descriptor.methods().get("build").delete();
      }
      for (PsiMethod method : builderClass.getAllMethods()) {
        if ("Builder".equals(method.getName())) {
          method.delete();
        }
      }
    }
  }

  public static PsiClass generate(
      PsiClass recordClass, Field[] components, PsiClass builderClass, BuilderSettings settings) {
    return new BuilderTypeGenerator(recordClass, components, builderClass, settings).build();
  }

  public PsiClass build() {
    writeFields();
    writeConstructors();
    writeBuilderMethod();
    writeSetters();
    writeBuildMethod();

    if (builderClass != null) {
      return builderClass;
    }
    JavaFile.Builder javaFile = JavaFile.builder("", builder.build());
    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(recordClass.getProject());
    PsiClass dummyClass =
        elementFactory.createClassFromText(javaFile.build().toString(), recordClass);
    return dummyClass.getInnerClasses()[0];
  }

  private void writeFields() {
    for (Field component : components) {
      FieldSpec.Builder fieldBuilder =
          FieldSpec.builder(component.typeName(), component.name(), Modifier.PRIVATE);
      if (isCollection(component.typeName()) && isNotNull(component)) {
        if (component.typeName() instanceof ParameterizedTypeName pt) {
          fieldBuilder.initializer("$T.of()", pt.rawType);
        } else {
          fieldBuilder.initializer("$T.of()", component.typeName());
        }
      }

      FieldSpec field = fieldBuilder.build();
      builder.addField(field);
      if (builderClass != null) {
        Field existing = descriptor.fields().get(field.name);
        if (descriptor.fields().containsKey(field.name)
            && existing.typeName().equals(component.typeName())) {
          if (existing.source() instanceof PsiVariable var
              && field.initializer != null
              && !var.hasInitializer()) {
            existing.source().delete();
          } else {
            continue;
          }
        }

        builderClass.addBefore(
            elementFactory.createFieldFromText(field.toString(), builderClass), firstPos());
      }
    }
  }

  private PsiElement firstPos() {
    PsiField[] fields = builderClass.getFields();
    if (fields.length > 0) {
      return fields[fields.length - 1];
    }
    for (PsiMethod method : builderClass.getMethods()) {
      return method;
    }
    return builderClass.getLastChild();
  }

  private void writeConstructors() {
    ClassName type = ClassName.bestGuess(Objects.requireNonNull(recordClass.getName()));
    Modifier modifier = settings.generateBuilderMethod() ? Modifier.PRIVATE : Modifier.PUBLIC;
    MethodSpec method = MethodSpec.constructorBuilder().addModifiers(modifier).build();
    builder.addMethod(method);
    PsiElement el = null;
    if (builderClass != null) {
      el =
          builderClass.addAfter(
              elementFactory.createMethodFromText(method.toString(), builderClass), firstPos());
    }
    if (!settings.generateCopyMethod()) {
      return;
    }

    MethodSpec.Builder constructor =
        MethodSpec.constructorBuilder().addModifiers(modifier).addParameter(type, "record");
    for (Field component : components) {
      constructor.addStatement("this.$L = record.$L", component.name(), component.name());
    }
    builder.addMethod(constructor.build());
    if (builderClass != null) {
      builderClass.addAfter(
          elementFactory.createMethodFromText(constructor.build().toString(), builderClass), el);
    }
  }

  private void writeSetters() {
    for (Field component : components) {
      String key = "%s(%s)".formatted(component.name(), component.typeName());
      boolean exists = builderClass != null && descriptor.methods().containsKey(key);
      ParameterSpec.Builder param = ParameterSpec.builder(component.typeName(), component.name());
      if (settings.validateNulls()) {
        if (!component.typeName().isPrimitive()
            && settings.validateNulls()
            && isAnnotatedNull(component)) {
          nullAnnotation(component).ifPresent(param::addAnnotation);
        }
      }

      MethodSpec.Builder method =
          MethodSpec.methodBuilder(methodName(settings.methodPrefix(), component.name()))
              .addModifiers(Modifier.PUBLIC)
              .returns(ClassName.bestGuess("Builder"))
              .addParameter(param.build());

      CodeBlock.Builder body = CodeBlock.builder();

      if (isString(component.typeName()) && isNotEmpty(component)) {
        body.addStatement(
            "this.$L = ($L == null || $L.isBlank()) ? null : $L",
            component.name(),
            component.name(),
            component.name(),
            component.name());
      } else if (isCollection(component.typeName()) && isNotNull(component)) {
        TypeName typeName = component.typeName();
        if (component.typeName() instanceof ParameterizedTypeName pt) {
          typeName = pt.rawType;
        }
        body.addStatement(
            "this.$L = ($L == null) ? $T.of() : $L",
            component.name(),
            component.name(),
            typeName,
            component.name());
      } else if (!component.typeName().isPrimitive() && isNotNull(component)) {
        body.addStatement(
            "this.$L = $T.requireNonNull($L, $S)",
            component.name(),
            Objects.class,
            component.name(),
            "Null " + component.name());
      } else {
        body.addStatement("this.$L = $L", component.name(), component.name());
      }

      body.addStatement("return this");

      if (settings.generateJavaDoc()) {
        method.addJavadoc(
            CodeBlock.builder()
                .add(
                    "Sets the {@code $L} and returns a reference to this Builder enabling method chaining.\n",
                    component.name())
                .add("@param $L the {@code $L} to set\n", component.name(), component.name())
                .add("@see $L#$L\n", recordClass.getName(), component.name())
                .add("@return a reference to this Builder\n")
                .build());
      }

      if (!exists) {
        method.addCode(body.build());
      }
      builder.addMethod(method.build());
      if (builderClass != null) {
        if (exists) {
          PsiMethod existingMethod = descriptor.methods().get(key);
          if (existingMethod.getBody() != null && !BuilderDescriptor.builderMethodSame(existingMethod)) {
            CodeBlock.Builder cb = CodeBlock.builder();
            for (PsiStatement statement : existingMethod.getBody().getStatements()) {
              cb.add("$L\n", statement.getText());
            }
            method.addCode(cb.build());
            builderClass.addAfter(
                elementFactory.createMethodFromText(method.build().toString(), builderClass),
                existingMethod);
            existingMethod.delete();
            continue;
          }
          method.addCode(body.build());
          existingMethod.delete();
        }
        builderClass.addBefore(
            elementFactory.createMethodFromText(method.build().toString(), builderClass),
            builderClass.getLastChild());
      }
    }
  }

  private void writeBuilderMethod() {
    if (!(settings.generateBuilderMethod() && settings.builderMethodInBuilder())) {
      return;
    }
    ClassName type = ClassName.bestGuess("Builder");
    MethodSpec method =
        MethodSpec.methodBuilder(settings.builderMethodName(recordClass))
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(type)
            .addStatement("return new $T()", type)
            .build();
    builder.addMethod(method);
    if (builderClass != null) {
      builderClass.addAfter(
          elementFactory.createMethodFromText(method.toString(), builderClass), firstPos());
    }
  }

  private void writeBuildMethod() {
    ClassName type = ClassName.bestGuess(Objects.requireNonNull(recordClass.getName()));

    MethodSpec.Builder method =
        MethodSpec.methodBuilder("build").addModifiers(Modifier.PUBLIC).returns(type);

    if (recordClass.getContainingClass() != null
        && recordClass.getContainingClass().isInterface()
        && recordClass.getImplementsListTypes().length > 0) {
      ClassName interfaceType =
          ClassName.bestGuess(
              Optional.ofNullable(recordClass.getContainingClass().getQualifiedName())
                  .or(() -> Optional.ofNullable(recordClass.getContainingClass().getName()))
                  .orElse("Object"));
      if (Arrays.stream(recordClass.getImplementsListTypes())
          .map(Field::resolveType)
          .anyMatch(it -> it.equals(interfaceType))) {
        method.returns(ClassName.bestGuess(interfaceType.simpleName()));
      }
    }

    if (settings.generateJavaDoc()) {
      method.addJavadoc(
          CodeBlock.builder()
              .add(
                  "Returns a {@code $L} built from the parameters previously set.\n",
                  recordClass.getName())
              .add("@see $L\n", recordClass.getName())
              .add(
                  "@return a {@code $L} built with parameters of this {@code $L.Builder}",
                  recordClass.getName(),
                  recordClass.getName())
              .build());
    }
    CodeBlock.Builder block = CodeBlock.builder();

    List<Field> required = new ArrayList<>();
    Set<String> fieldNames = Arrays.stream(components).map(Field::name).collect(Collectors.toSet());
    List<Field> recordFields =
        recordClass.isRecord()
            ? Arrays.stream(recordClass.getRecordComponents()).map(Field::of).toList()
            : Arrays.stream(components).toList();

    for (int i = 0; i < recordFields.size(); i++) {
      if (i > 0) {
        block.add(",$Z");
      }
      Field component = recordFields.get(i);
      if (fieldNames.contains(component.name())) {
        block.add("this.$L", component.name());
      } else {
        block.add("null");
      }

      if (fieldNames.contains(component.name())
          && settings.validateNulls()
          && !component.typeName().isPrimitive()) {
        if (settings.nullHandlingRequired()) {
          if (!isAnnotatedNull(component)) {
            required.add(component);
          }
        } else {
          if (isAnnotatedNotNull(component)) {
            required.add(component);
          }
        }
      }
    }

    if (!required.isEmpty()) {
      method.addStatement("$T missing = new $T()", StringBuilder.class, StringBuilder.class);
      required.forEach(
          it ->
              method
                  .beginControlFlow("if (this.$L == null)", it.name())
                  .addStatement("missing.append($S)", " " + it.name())
                  .endControlFlow());
      method
          .beginControlFlow("if (!missing.isEmpty())")
          .addStatement(
              "throw new $T($S + missing)",
              IllegalStateException.class,
              "Missing required properties:")
          .endControlFlow();
    }

    if (descriptor.methods().containsKey("validate")) {
      method.addStatement("this.validate()");
    }

    if (recordClass.isRecord()) {
      method.addStatement("return new $T($L)", type, block.build());
    } else {
      method.addStatement("return new $T(this)", type);
    }
    builder.addMethod(method.build());
    if (builderClass != null) {
      builderClass.addBefore(
          elementFactory.createMethodFromText(method.build().toString(), builderClass),
          descriptor.methods().containsKey("validate")
              ? descriptor.methods().get("validate")
              : builderClass.getLastChild());
    }
  }

  public boolean isNotNull(Field component) {
    if (component.typeName().isPrimitive()) {
      return true;
    }
    if (settings.nullHandlingRequired()) {
      return !isAnnotatedNull(component);
    }
    return isAnnotatedNotNull(component);
  }

  public static boolean isAnnotatedNull(Field component) {
    if (isAnnotatedNotNull(component)) {
      return false;
    }
    for (PsiAnnotation annotation : component.source().getAnnotations()) {
      String qualifiedName = annotation.getQualifiedName();
      if (qualifiedName != null) {
        if (qualifiedName.endsWith("Nullable") || qualifiedName.endsWith("Null")) {
          return true;
        }
      }
    }
    return false;
  }

  public static Optional<AnnotationSpec> nullAnnotation(Field component) {
    if (!isAnnotatedNull(component)) {
      return Optional.empty();
    }
    for (PsiAnnotation annotation : component.source().getAnnotations()) {
      String qualifiedName = annotation.getQualifiedName();
      if (qualifiedName != null) {
        if (qualifiedName.endsWith("Nullable") || qualifiedName.endsWith("Null")) {
          AnnotationSpec.Builder builder =
              AnnotationSpec.builder(ClassName.bestGuess(qualifiedName));
          annotation
              .getAttributes()
              .forEach(
                  attr -> {
                    PsiAnnotationMemberValue value =
                        annotation.findAttributeValue(attr.getAttributeName());
                    if (value != null) {
                      builder.addMember(attr.getAttributeName(), "$L", value.getText());
                    }
                  });
          return Optional.of(builder.build());
        }
      }
    }
    return Optional.empty();
  }

  public static boolean isAnnotatedNotNull(Field component) {
    for (PsiAnnotation annotation : component.source().getAnnotations()) {
      String qualifiedName = annotation.getQualifiedName();
      if (qualifiedName != null) {
        if (qualifiedName.endsWith("NotNull") || qualifiedName.endsWith("NonNull")) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean isNotEmpty(Field component) {
    for (PsiAnnotation annotation : component.source().getAnnotations()) {
      if (annotation.getQualifiedName() != null) {
        if (annotation.getQualifiedName().contains("NotBlank")
            || annotation.getQualifiedName().contains("NotEmpty")) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean isString(TypeName type) {
    String value = type.toString();
    return "java.lang.String".equals(value) || "String".equals(value);
  }

  public static boolean isCollection(TypeName type) {
    if (type instanceof ParameterizedTypeName pt) {
      return isCollection(pt.rawType);
    }
    String value = type.toString();
    return "java.util.List".equals(value)
        || "List".equals(value)
        || "java.util.Set".equals(value)
        || "Set".equals(value)
        || "java.util.Map".equals(value)
        || "Map".equals(value);
  }

  public static String methodName(String prefix, String name) {
    if (prefix == null || prefix.trim().isBlank()) {
      return name;
    }
    return prefix + Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }
}
