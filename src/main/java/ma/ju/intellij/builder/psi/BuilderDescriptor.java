package ma.ju.intellij.builder.psi;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record BuilderDescriptor(Map<String, PsiMethod> methods, Map<String, Field> fields) {
  public BuilderDescriptor() {
    this(new LinkedHashMap<>(), new LinkedHashMap<>());
  }

  public static BuilderDescriptor from(PsiClass recordClass) {
    BuilderDescriptor descriptor = new BuilderDescriptor();
    BuilderGenerator.getComponents(recordClass).forEach(f -> descriptor.fields.put(f.name(), f));
    for (PsiMethod method : recordClass.getAllMethods()) {
      if (!method.hasModifierProperty(PsiModifier.PUBLIC)
          || method.hasModifierProperty(PsiModifier.STATIC)
          || method.getReturnType() == null) {
        continue;
      }
      if (method.getParameterList().getParametersCount() > 0) {
        continue;
      }
      String field = getName(method.getName());

      TypeName typeName = Field.resolveType(method.getReturnType());
      if (descriptor.fields.containsKey(field) && !descriptor.fields.get(field).typeName().equals(typeName)) {
        continue;
      }
      descriptor.methods.put("%s(%s)".formatted(field, typeName), method);
    }
    return descriptor;
  }

  public static BuilderDescriptor from(PsiClass builderClass, PsiClass recordClass) {
    BuilderDescriptor descriptor = new BuilderDescriptor();
    if (builderClass == null) {
      return descriptor;
    }
    ClassName recordType =
        ClassName.bestGuess(Objects.requireNonNull(recordClass.getQualifiedName()));
    for (PsiField field : builderClass.getFields()) {
      if (field.hasModifierProperty(PsiModifier.FINAL)) {
        if (field.getInitializer() != null) {
          continue;
        }
      }
      if (!field.hasModifierProperty(PsiModifier.PRIVATE)) {
        continue;
      }

      Field f = Field.of(field);
      descriptor.fields.put(f.name(), f);
    }

    ClassName className =
        ClassName.bestGuess(
            Objects.requireNonNull(
                Optional.ofNullable(builderClass.getQualifiedName())
                    .orElse(builderClass.getName())));
    for (PsiMethod method : builderClass.getAllMethods()) {
      if (method.getName().equals("validate")
          && method.getParameterList().getParametersCount() == 0) {
        descriptor.methods.put("validate", method);
      }
      if (!method.hasModifierProperty(PsiModifier.PUBLIC) || method.getReturnType() == null) {
        continue;
      }

      TypeName type = Field.resolveType(method.getReturnType());
      if (method.getName().equals("build") && type.equals(recordType)) {
        descriptor.methods.put("build", method);
        continue;
      }

      if (!type.equals(className)) {
        continue;
      }

      if (method.getParameterList().getParametersCount() != 1) {
        continue;
      }

      TypeName parameterType =
          Field.resolveType(
              Objects.requireNonNull(method.getParameterList().getParameter(0)).getType());
      String field = getName(method.getName());

      if (descriptor.fields.containsKey(field)
          && !descriptor.fields.get(field).typeName().equals(parameterType)) {
        continue;
      }

      descriptor.methods.put("%s(%s)".formatted(field, parameterType), method);
    }

    return descriptor;
  }

  public static boolean builderMethodSame(PsiMethod source) {
    Set<String> methods = new HashSet<>();
    String name = getName(source.getName());
    if (source.getParameterList().getParametersCount() != 1) {
      return false;
    }
    TypeName type =
        Field.resolveType(
            Objects.requireNonNull(source.getParameterList().getParameter(0)).getType());
    methods.add(
        CodeBlock.builder()
            .addStatement("this.$L = $L", name, name)
            .addStatement("return this")
            .build()
            .toString());
    methods.add(
        CodeBlock.builder()
            .addStatement("this.$L = $T.requireNonNull($L)", name, Objects.class, name)
            .addStatement("return this")
            .build()
            .toString());
    methods.add(
        CodeBlock.builder()
            .addStatement("this.$L = Objects.requireNonNull($L)", name, name)
            .addStatement("return this")
            .build()
            .toString());
    if (BuilderTypeGenerator.isCollection(type)) {
      methods.add(
          CodeBlock.builder()
              .addStatement("this.$L = ($L == null) ? $T.of() : $L", name, name, type, name)
              .addStatement("return this")
              .build()
              .toString());
    }
    methods.add(
        CodeBlock.builder()
            .addStatement(
                "this.$L = ($L == null || $L.isBlank()) ? null : $L", name, name, name, name)
            .addStatement("return this")
            .build()
            .toString());
    String signature =
        source.getBody() == null ? "" : source.getBody().getText().replaceAll("\\s+", "");
    return methods.stream()
        .map(it -> "{" + it.replaceAll("\\s+", "") + "}")
        .anyMatch(it -> it.equals(signature));
  }

  public static String getName(String name) {
    for (String prefix : List.of("with", "set", "get", "is", "has")) {
      if (name.startsWith(prefix) && Character.isUpperCase(name.charAt(prefix.length()))) {
        String value = name.substring(prefix.length());
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
      }
    }
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }
}
