package ma.ju.intellij.builder.psi;

import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiRecordComponent;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiTypesUtil;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record Field(
    PsiModifierListOwner source, PsiClass containingClass, PsiType type, TypeName typeName) {

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

  public TypeName rawType() {
    if (typeName instanceof ParameterizedTypeName pt) {
      return pt.rawType;
    }
    return typeName;
  }

  public String signature() {
    StringBuilder sb = new StringBuilder();
    if (containingClass.getQualifiedName() != null) {
      sb.append(containingClass.getQualifiedName());
    } else if (containingClass.getName() != null) {
      sb.append(containingClass.getName());
    }
    sb.append(" {");
    if (source instanceof PsiVariable variable) {
      if (variable.getModifierList() != null) {
        sb.append(variable.getModifierList().getText());
      }
    }
    return sb.append(" ").append(typeName).append(" ").append(name()).append("}").toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Field field = (Field) o;
    return Objects.equals(signature(), field.signature());
  }

  @Override
  public int hashCode() {
    return Objects.hash(signature());
  }

  @Override
  public String toString() {
    return signature();
  }

  public String name() {
    if (source instanceof PsiVariable variable) {
      return variable.getName();
    } else if (source instanceof PsiMethod method) {
      return method.getName();
    }
    throw new IllegalStateException(source.getText());
  }

  public static TypeName resolveType(PsiType type) {
    try {
      if (type instanceof PsiClassReferenceType classReferenceType) {
        if (classReferenceType.getParameterCount() > 0) {
          return ParameterizedTypeName.get(
              ClassName.bestGuess(
                  Optional.ofNullable(PsiTypesUtil.getPsiClass(type))
                      .map(PsiClass::getQualifiedName)
                      .orElse(classReferenceType.getClassName())),
              Arrays.stream(classReferenceType.getParameters())
                  .map(Field::resolveType)
                  .toArray(TypeName[]::new));
        }
        return ClassName.bestGuess(
            Optional.ofNullable(PsiTypesUtil.getPsiClass(type))
                .map(PsiClass::getQualifiedName)
                .orElse(classReferenceType.getClassName()));
      } else if (type instanceof PsiPrimitiveType primitiveType) {
        return PRIMITIVES.get(primitiveType.getName());
      } else if (type instanceof PsiArrayType arrayType) {
        return ArrayTypeName.of(resolveType(arrayType.getComponentType()));
      }
      return ClassName.bestGuess(
          Optional.ofNullable(PsiTypesUtil.getPsiClass(type))
              .map(PsiClass::getQualifiedName)
              .orElse(type.getCanonicalText()));
    } catch (Exception e) {
      // If we can't determine the type, we will assume everything is an object
      return TypeName.get(Object.class);
    }
  }

  public static Field of(PsiRecordComponent record) {
    return new Field(
        record, record.getContainingClass(), record.getType(), resolveType(record.getType()));
  }

  public static Field of(PsiField field) {
    return new Field(
        field, field.getContainingClass(), field.getType(), resolveType(field.getType()));
  }

  public static Field of(PsiMethod method) {
    return new Field(
        method,
        method.getContainingClass(),
        method.getReturnType(),
        resolveType(method.getReturnType()));
  }
}
