package ma.ju.intellij.builder.psi;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiResolveHelper;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BuilderCollector {
  public static List<Field> collectFields(final PsiFile file, final Editor editor) {
    final int offset = editor.getCaretModel().getOffset();
    final PsiElement element = file.findElementAt(offset);
    if (element == null) {
      return List.of();
    }

    final PsiClass clazz = PsiTreeUtil.getParentOfType(element, PsiClass.class);
    if (clazz == null || clazz.hasModifierProperty(PsiModifier.ABSTRACT)) {
      return List.of();
    }
    final List<Field> allFields = new ArrayList<>();

    PsiClass classToExtractFieldsFrom = clazz;
    while (classToExtractFieldsFrom != null) {
      final List<Field> classFieldMembers =
          collectFieldsInClass(element, clazz, classToExtractFieldsFrom);
      allFields.addAll(0, classFieldMembers);
      classToExtractFieldsFrom = classToExtractFieldsFrom.getSuperClass();
    }

    return allFields;
  }

  public static List<Field> collectFields(final PsiClass psiClass) {
    return collectFieldsInClass(psiClass, psiClass, psiClass);
  }

  private static List<Field> collectFieldsInClass(
      final PsiElement element, final PsiClass accessObjectClass, final PsiClass clazz) {
    final List<Field> classFieldMembers = new ArrayList<>();
    final PsiResolveHelper helper =
        JavaPsiFacade.getInstance(clazz.getProject()).getResolveHelper();

    if (clazz.isInterface()) {
      return Arrays.stream(clazz.getMethods()).map(Field::of).toList();
    }

    for (final PsiField field : clazz.getFields()) {
      // check access to the field from the builder container class (e.g. private superclass fields)
      if ((helper.isAccessible(field, clazz, accessObjectClass)
              || hasSetter(clazz, field.getName()))
          && !PsiTreeUtil.isAncestor(field, element, false)) {
        // skip static fields
        if (field.hasModifierProperty(PsiModifier.STATIC)) {
          continue;
        }

        // skip any uppercase fields
        if (!hasLowerCaseChar(field.getName())) {
          continue;
        }

        // skip eventual logging fields
        final String fieldType = field.getType().getCanonicalText();
        if ("org.apache.log4j.Logger".equals(fieldType)
            || "org.apache.logging.log4j.Logger".equals(fieldType)
            || "java.util.logging.Logger".equals(fieldType)
            || "org.slf4j.Logger".equals(fieldType)
            || "ch.qos.logback.classic.Logger".equals(fieldType)
            || "net.sf.microlog.core.Logger".equals(fieldType)
            || "org.apache.commons.logging.Log".equals(fieldType)
            || "org.pmw.tinylog.Logger".equals(fieldType)
            || "org.jboss.logging.Logger".equals(fieldType)
            || "jodd.log.Logger".equals(fieldType)) {
          continue;
        }

        if (field.hasModifierProperty(PsiModifier.FINAL)) {
          if (field.getInitializer() != null) {
            continue; // skip final fields that are assigned in the declaration
          }

          if (!accessObjectClass.isEquivalentTo(clazz)) {
            continue; // skip final superclass fields
          }
        }

        final PsiClass containingClass = field.getContainingClass();
        if (containingClass != null) {
          classFieldMembers.add(Field.of(field));
        }
      }
    }
    return classFieldMembers;
  }

  public static boolean hasSetter(PsiClass clazz, String name) {
    Set<String> names = Set.of(BuilderTypeGenerator.methodName("set", name));

    for (PsiMethod method : clazz.getAllMethods()) {
      if (names.contains(method.getName()) && method.getParameterList().getParametersCount() == 1) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasLowerCaseChar(String str) {
    for (int i = 0; i < str.length(); i++) {
      if (Character.isLowerCase(str.charAt(i))) {
        return true;
      }
    }

    return false;
  }
}
