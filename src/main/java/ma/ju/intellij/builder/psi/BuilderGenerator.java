package ma.ju.intellij.builder.psi;

import com.intellij.codeInsight.NullableNotNullManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import ma.ju.intellij.builder.ide.RecordMemberChooser;
import org.jetbrains.annotations.NotNull;

public class BuilderGenerator {

  public static void generate(PsiClass recordClass) {
    generate(
        recordClass,
        RecordMemberChooser.mapRecordComponentNames(recordClass),
        BuilderSettings.init());
  }

  public static void generate(PsiClass recordClass, BuilderSettings settings) {
    generate(recordClass, RecordMemberChooser.mapRecordComponentNames(recordClass), settings);
  }

  public static void generate(PsiClass recordClass, List<Field> selected) {
    generate(recordClass, selected, BuilderSettings.init());
  }

  public static void generate(
      PsiClass recordClass, List<Field> selected, BuilderSettings settings) {
    if (recordClass.isInterface()) {
      CodeBlock.Builder block = CodeBlock.builder();

      String className = recordClass.getName() + "Record";
      for (PsiClass psiClass : recordClass.getInnerClasses()) {
        if (className.equals(psiClass.getName())) {
          generate(psiClass, selected, settings);
          return;
        }
      }

      block.add("public record $LRecord(", recordClass.getName());
      for (int i = 0; i < selected.size(); i++) {
        Field field = selected.get(i);
        if (i > 0) {
          block.add(", ");
        }
        block.add("$L $L", field.typeName(), field.name());
      }

      block.add(") implements $L {}\n", recordClass.getName());

      PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(recordClass.getProject());
      PsiClass dummyClass =
          elementFactory.createClassFromText(block.build().toString(), recordClass);
      dummyClass.setName(className);
      PsiClass record =
          dummyClass.getInnerClasses().length > 0 ? dummyClass.getInnerClasses()[0] : dummyClass;
      generate(
          (PsiClass) recordClass.addBefore(record, recordClass.getLastChild()), selected, settings);
      return;
    }

    PsiElement builderClass = null;
    Field[] selectedFields = selected.toArray(new Field[0]);
    for (PsiClass innerClass : recordClass.getInnerClasses()) {
      if ("Builder".equals(innerClass.getName())) {
        builderClass = innerClass;
      }
    }
    removeBuilderMethods(recordClass, settings);
    // denotes the `}` token that declares the end of the class
    PsiElement position = recordClass.getLastChild();
    // create builder pattern structures and add them to the record
    if (builderClass != null) {
      createBuilderClass(recordClass, selectedFields, (PsiClass) builderClass, settings);
    } else {
      builderClass =
          recordClass.addBefore(
              createBuilderClass(recordClass, selectedFields, null, settings), position);
    }

    PsiElement toStringMethod = null;
    for (PsiMethod m : recordClass.getAllMethods()) {
      if (m.getName().equals("toString")
          && m.getParameterList().getParametersCount() == 0
          && m.isWritable()) {
        toStringMethod = m.copy();
        m.delete();
        break;
      }
    }

    if (toStringMethod != null) {
      recordClass.addBefore(toStringMethod, builderClass);
    } else if (settings.generateToStringMethod()) {
      PsiMethod method = createToStringMethod(recordClass, selectedFields, settings);
      if (method != null) {
        recordClass.addBefore(method, builderClass);
      }
    }

    PsiElement builderMethod =
        settings.generateBuilderMethod() && !settings.builderMethodInBuilder()
            ? recordClass.addBefore(createBuilderMethod(recordClass), builderClass)
            : builderClass;

    PsiElement toBuilderMethod =
        settings.generateCopyMethod()
            ? recordClass.addBefore(createToBuilderMethod(recordClass), builderMethod)
            : builderMethod;

    Optional<PsiMethod> constructor =
        createRecordConstructor(recordClass, selectedFields, settings);
    PsiElement firstMethod =
        Arrays.stream(recordClass.getAllMethods())
            .findFirst()
            .map(it -> (PsiElement) it)
            .orElse(toBuilderMethod);

    constructor.ifPresent(psiMethod -> recordClass.addBefore(psiMethod, firstMethod));
    if (!recordClass.isRecord()) {
      writeGetters(recordClass, selectedFields, settings);
    }
    formatRecordCode(recordClass, builderClass);
  }

  public static void delete(PsiClass recordClass, List<Field> selected, BuilderSettings settings) {
    removeBuilderMethods(recordClass, settings);
    PsiClass recordClassCopy = (PsiClass) recordClass.copy();
    PsiClass generatedBuilder =
        (PsiClass)
            recordClassCopy.addBefore(
                createBuilderClass(recordClassCopy, selected.toArray(new Field[0]), null, settings),
                recordClassCopy.getLastChild());

    PsiClass builderClass =
        Arrays.stream(recordClass.getInnerClasses())
            .filter(it -> "Builder".equals(it.getName()))
            .findFirst()
            .orElse(null);

    if (builderClass == null) {
      return;
    }

    if (builderClass.getQualifiedName() != null) {
      for (PsiMethod method : recordClass.getAllMethods()) {
        if (method.getReturnType() == null) {
          continue;
        }
        TypeName typeName = Field.resolveType(method.getReturnType());

        if (method.isWritable()
            && typeName.equals(ClassName.bestGuess(builderClass.getQualifiedName()))) {
          method.delete();
        }
      }
    }

    BuilderDescriptor descriptor = BuilderDescriptor.from(builderClass, recordClass);
    BuilderDescriptor generatedDescriptor = BuilderDescriptor.from(generatedBuilder, recordClass);

    if (descriptor.methods().containsKey("build")) {
      descriptor.methods().get("build").delete();
    }

    for (PsiMethod method : builderClass.getAllMethods()) {
      if ("Builder".equals(method.getName())) {
        method.delete();
      }
    }
    Set<String> keys =
        selected.stream()
            .map(it -> "%s(%s)".formatted(it.name(), it.typeName()))
            .collect(Collectors.toSet());
    for (Field field : descriptor.fields().values()) {
      String key = "%s(%s)".formatted(field.name(), field.typeName());
      if (!keys.contains(key)) {
        continue;
      }
      if (descriptor.methods().containsKey(key)) {
        PsiMethod existing = descriptor.methods().get(key);
        PsiMethod generated = generatedDescriptor.methods().get(key);
        if (generated != null) {
          if (existing.getBody() != null && generated.getBody() != null) {
            if (!BuilderDescriptor.builderMethodSame(existing)) {
              continue;
            }
          }
        }

        descriptor.methods().get(key).delete();
        field.source().delete();
      }
    }

    List<Field> remaining = getComponents(builderClass);
    if (remaining.isEmpty()) {
      builderClass.delete();
      formatRecordCode(recordClass, builderClass);
      return;
    }
    Set<String> names = remaining.stream().map(Field::name).collect(Collectors.toSet());
    List<Field> regenerate =
        getComponents(recordClass).stream().filter(it -> names.contains(it.name())).toList();
    if (regenerate.isEmpty()) {
      formatRecordCode(recordClass, builderClass);
      return;
    }
    // Regenerate the builder
    generate(recordClass, regenerate, settings);
  }

  public static @NotNull List<Field> getComponents(PsiClass recordClass) {
    if (recordClass == null) {
      return List.of();
    }
    if (recordClass.isRecord()) {
      return Arrays.stream(recordClass.getRecordComponents()).map(Field::of).toList();
    }
    return BuilderCollector.collectFields(recordClass);
  }

  public static PsiMethod createEntityConstructor(PsiClass recordClass, Field[] components) {
    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(recordClass.getProject());
    MethodSpec.Builder builder =
        MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .addParameter(ClassName.bestGuess("Builder"), "builder");
    BuilderSettings settings = BuilderSettings.settings();
    for (Field component : components) {

      CodeBlock tpl = CodeBlock.builder().add("builder.$L", component.name()).build();
      boolean isCollectionType = BuilderTypeGenerator.isCollection(component.typeName());
      boolean isNotNull =
          settings.nullHandlingRequired()
              ? !NullableNotNullManager.isNullable(component.source())
              : NullableNotNullManager.isNotNull(component.source());

      if (settings.immutableCollections() && isCollectionType) {
        tpl =
            isNotNull
                ? CodeBlock.builder().add("$L.copyOf($L)", component.rawType(), tpl).build()
                : CodeBlock.builder()
                    .add(
                        "Optional.ofNullable($L).map($L::copyOf).orElse(null)",
                        tpl,
                        component.rawType())
                    .build();
      }
      builder.addStatement("this.$L = $L", component.name(), tpl);
    }

    return elementFactory.createMethodFromText(builder.build().toString(), recordClass);
  }

  public static void writeGetters(
      PsiClass recordClass, Field[] components, BuilderSettings settings) {
    PsiElement builder = recordClass.getConstructors()[recordClass.getConstructors().length - 1];
    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(recordClass.getProject());

    BuilderDescriptor descriptor = BuilderDescriptor.from(recordClass);
    for (Field component : components) {
      String key = "%s(%s)".formatted(component.name(), component.typeName());
      boolean exists = descriptor.methods().containsKey(key);
      String name = BuilderTypeGenerator.methodName("get", component.name());
      if (component.typeName().toString().equals("boolean")) {
        name = BuilderTypeGenerator.methodName("is", component.name());
      }

      if (exists) {
        name = descriptor.methods().get(key).getName();
      }

      MethodSpec.Builder method =
          MethodSpec.methodBuilder(name)
              .returns(component.typeName())
              .addModifiers(Modifier.PUBLIC);
      if (settings.validateNulls() && !component.typeName().isPrimitive()) {
        BuilderTypeGenerator.nullAnnotation(component).ifPresent(method::addAnnotation);
      }

      if (!exists) {
        method.addStatement("return this.$L", component.name());
      }

      if (settings.generateJavaDoc()) {
        method.addJavadoc(
            CodeBlock.builder()
                .add("Gets the {@code $L}\n", component.name())
                .add("@see $L#$L\n", recordClass.getName(), component.name())
                .add("@return the $L\n", component.name())
                .build());
      }

      if (exists) {
        PsiMethod existingMethod = descriptor.methods().get(key);
        if (existingMethod.getBody() != null) {
          CodeBlock.Builder cb = CodeBlock.builder();
          for (PsiStatement statement : existingMethod.getBody().getStatements()) {
            cb.add("$L\n", statement.getText());
          }
          method.addCode(cb.build());
          builder =
              recordClass.addAfter(
                  elementFactory.createMethodFromText(method.build().toString(), recordClass),
                  existingMethod);
          existingMethod.delete();
          continue;
        }
        existingMethod.delete();
        method.addStatement("return this.$L", component.name());
      }

      builder =
          recordClass.addAfter(
              elementFactory.createMethodFromText(method.build().toString(), recordClass), builder);
    }
  }

  public static Optional<PsiMethod> createRecordConstructor(
      PsiClass recordClass, Field[] components, BuilderSettings settings) {
    if (!recordClass.isRecord()) {
      return Optional.of(createEntityConstructor(recordClass, components));
    }
    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(recordClass.getProject());
    CodeBlock.Builder cb = CodeBlock.builder().beginControlFlow("public " + recordClass.getName());
    BuilderDescriptor descriptor =
        Arrays.stream(recordClass.getInnerClasses())
            .filter(it -> "Builder".equals(it.getName()))
            .findFirst()
            .map(it -> BuilderDescriptor.from(it, recordClass))
            .orElse(new BuilderDescriptor());

    int count = 0;
    for (Field component : components) {
      CodeBlock tpl = CodeBlock.builder().add("$L", component.name()).build();
      boolean isCollectionType = BuilderTypeGenerator.isCollection(component.typeName());
      boolean isNotNull =
          settings.nullHandlingRequired()
              ? !NullableNotNullManager.isNullable(component.source())
              : NullableNotNullManager.isNotNull(component.source());

      int index = count;
      if (settings.validateNulls() && isNotNull && !component.typeName().isPrimitive()) {
        tpl =
            CodeBlock.builder()
                .add(
                    "$T.requireNonNull($L, $S)",
                    Objects.class,
                    tpl,
                    "property :%s is required".formatted(component.name()))
                .build();

        count++;
      }

      if (settings.immutableCollections() && isCollectionType) {
        tpl =
            isNotNull
                ? CodeBlock.builder().add("$L.copyOf($L)", component.rawType(), tpl).build()
                : CodeBlock.builder()
                    .add(
                        "Optional.ofNullable($L).map($L::copyOf).orElse(null)",
                        tpl,
                        component.rawType())
                    .build();
        cb.addStatement("$L = $L", component.name(), tpl);
        count++;
      } else if (count > index) {
        cb.addStatement(tpl);
      }
    }

    if (settings.recordValidationStatements() && descriptor.methods().containsKey("validate")) {
      PsiMethod method = descriptor.methods().get("validate");
      if (method.getBody() != null) {
        for (PsiStatement expr : method.getBody().getStatements()) {
          cb.add("$L", expr.getText());
        }
      }
    }

    cb.endControlFlow();
    return count == 0
        ? Optional.empty()
        : Optional.of(elementFactory.createMethodFromText(cb.build().toString(), recordClass));
  }

  @NotNull
  public static PsiClass createBuilderClass(
      PsiClass recordClass, Field[] components, PsiClass builderType, BuilderSettings settings) {
    return BuilderTypeGenerator.generate(recordClass, components, builderType, settings);
  }

  public static PsiMethod createBuilderMethod(PsiClass recordClass) {
    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(recordClass.getProject());
    ClassName type = ClassName.bestGuess("Builder");
    return elementFactory.createMethodFromText(
        MethodSpec.methodBuilder(BuilderSettings.settings().builderMethodName(recordClass))
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(type)
            .addStatement("return new $T()", type)
            .build()
            .toString(),
        recordClass);
  }

  public static PsiMethod createToStringMethod(
      PsiClass recordClass, Field[] fields, BuilderSettings settings) {
    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(recordClass.getProject());

    MethodSpec.Builder method =
        MethodSpec.methodBuilder("toString")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(AnnotationSpec.builder(Override.class).build())
            .returns(ClassName.get(String.class));

    if (settings.generatePrettyToString()) {
      if (recordClass.isRecord()) {
        return null;
      }
      method.addCode("return $S +\n", recordClass.getName() + "{");
      for (int i = 0; i < fields.length; i++) {
        Field field = fields[i];
        String msg = i > 0 ? ", " : "";
        method.addCode("        $S + $L +\n", "%s%s=".formatted(msg, field.name()), field.name());
      }
      method.addCode("        $S;", "}");
    } else {
      method.addStatement(
          " return getClass().getName() + $S + $L",
          "@",
          "Integer.toHexString(System.identityHashCode(this))");
    }

    return elementFactory.createMethodFromText(method.build().toString(), recordClass);
  }

  public static PsiMethod createToBuilderMethod(PsiClass recordClass) {
    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(recordClass.getProject());
    ClassName type = ClassName.bestGuess("Builder");
    return elementFactory.createMethodFromText(
        MethodSpec.methodBuilder(BuilderSettings.settings().copyMethodName())
            .addModifiers(Modifier.PUBLIC)
            .returns(type)
            .addStatement("return new $T(this)", type)
            .build()
            .toString(),
        recordClass);
  }

  /**
   * Removes the following: - `builder()` instance method - `toBuilder()` instance method -
   * `Builder` nested class
   */
  public static void removeBuilderMethods(PsiClass recordClass, BuilderSettings settings) {
    PsiMethod[] methods = recordClass.getMethods();
    Set<String> copyMethodNames = settings.copyMethodNames();
    Set<String> builderMethodNames = settings.builderMethodNames(recordClass);

    for (PsiMethod method : methods) {
      if (method.getParameterList().getParametersCount() > 0) {
        continue;
      }

      if (copyMethodNames.contains(method.getName())
          || builderMethodNames.contains(method.getName())) {
        method.delete();
      }
    }

    int count = recordClass.getRecordComponents().length;
    for (PsiMethod method : recordClass.getConstructors()) {
      if (!recordClass.isRecord()) {
        if (method.isWritable()) {
          method.delete();
        }
      } else if (method.isWritable() && method.getParameterList().getParametersCount() == count) {
        method.delete();
      }
    }
  }

  /** Reformat code to adhere to project's code style settings */
  public static void formatRecordCode(PsiClass recordClass, PsiElement builderClass) {
    JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(recordClass.getProject());
    PsiFile javaFile = recordClass.getContainingFile();
    styleManager.shortenClassReferences(builderClass);

    if (javaFile instanceof PsiJavaFile psiJavaFile) {
      styleManager.removeRedundantImports(psiJavaFile);
      styleManager.shortenClassReferences(psiJavaFile);
      styleManager.optimizeImports(psiJavaFile);
    } else {
      styleManager.shortenClassReferences(javaFile);
      styleManager.optimizeImports(javaFile);
    }
    CodeStyleManager.getInstance(recordClass.getProject()).reformat(javaFile);
  }
}
