package ma.ju.intellij.builder.inspection;

import static com.intellij.codeInspection.options.OptPane.checkbox;

import com.intellij.codeInsight.NullableNotNullManager;
import com.intellij.codeInsight.options.JavaInspectionButtons;
import com.intellij.codeInsight.options.JavaInspectionControls;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.options.OptPane;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.impl.source.PsiMethodImpl;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.Query;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import ma.ju.intellij.builder.psi.BuilderDescriptor;
import ma.ju.intellij.builder.psi.BuilderGenerator;
import ma.ju.intellij.builder.psi.Field;
import org.jetbrains.annotations.NotNull;

public class BuilderMissingRequiredFieldsInspection extends AbstractBaseJavaLocalInspectionTool {
  @SuppressWarnings("PublicField")
  public boolean requireNotNullAnnotation = false;

  public boolean ignoreExternalBuilders = true;

  private final ReplaceWithRequired myQuickFix = new ReplaceWithRequired();

  private PsiClass getContainingBuilderClass(PsiMethod element) {
    PsiClass aClass = element.getContainingClass();
    while (aClass != null && !isBuilder(aClass)) {
      aClass = aClass.getContainingClass();
    }

    return aClass;
  }

  private ClassName resolveType(PsiClass psiClass) {
    try {
      if (psiClass.getQualifiedName() != null) {
        return ClassName.bestGuess(psiClass.getQualifiedName());
      } else if (psiClass.getName() != null) {
        return ClassName.bestGuess(psiClass.getName());
      }
      return ClassName.get(Object.class);
    } catch (Exception e) {
      return ClassName.get(Object.class);
    }
  }

  private Boolean isBuilder(PsiClass psiClass) {
    if (psiClass == null
        || !Objects.equals(psiClass.getName(), "Builder")
        || psiClass.getContainingClass() == null) {
      return false;
    }

    ClassName type = resolveType(psiClass.getContainingClass());
    for (PsiMethod method : psiClass.getMethods()) {
      if (method.getName().equals("build")
          && !method.hasParameters()
          && method.getReturnType() != null) {
        if (Field.resolveType(method.getReturnType()).equals(type)) {
          return true;
        }
      }
    }
    return false;
  }

  private List<Field> setterFieldsFor(PsiMethod method) {
    Queue<PsiElement> queue = new LinkedList<>();
    Set<PsiElement> seenElements = new HashSet<>();
    Set<Field> fields = new LinkedHashSet<>();

    queue.offer(method);
    while (!queue.isEmpty()) {
      PsiElement cur = queue.poll();
      if (cur == null) {
        continue;
      }
      seenElements.add(cur);

      if (cur instanceof PsiMethod psiMethod) {
        if (psiMethod.getBody() != null) {
          for (PsiStatement statement : psiMethod.getBody().getStatements()) {
            if (!seenElements.contains(statement)) {
              queue.offer(statement);
            }
          }
        }
      }

      if (cur instanceof PsiAssignmentExpression assignmentExpression) {
        if (!seenElements.contains(assignmentExpression.getLExpression())) {
          queue.offer(assignmentExpression.getLExpression());
        }
      }

      if (cur instanceof PsiReferenceExpressionImpl) {
        PsiElement resolvedElement = ((PsiReferenceExpressionImpl) cur).resolve();

        if (resolvedElement instanceof PsiField field
            && field.getContainingClass() == method.getContainingClass()) {
          fields.add(Field.of(field));
        }
      }

      for (PsiElement child : cur.getChildren()) {
        if (!seenElements.contains(child)) {
          queue.offer(child);
        }
      }
    }

    return new ArrayList<>(fields);
  }

  private BuilderDescriptor descriptorFor(PsiClass recordClass) {
    BuilderDescriptor descriptor = new BuilderDescriptor();
    ClassName className = resolveType(recordClass);
    BuilderGenerator.getComponents(recordClass)
        .forEach(
            f -> {
              if (!f.typeName().isPrimitive()
                  && !f.source().hasModifierProperty(PsiModifier.FINAL)
                  && f.source() instanceof PsiVariable variable
                  && !variable.hasInitializer()) {
                descriptor.fields().put(f.name(), f);
              }
            });

    for (PsiMethod method : recordClass.getAllMethods()) {
      if (method.hasModifierProperty(PsiModifier.PRIVATE)
          || method.hasModifierProperty(PsiModifier.STATIC)
          || method.getReturnType() == null) {
        continue;
      }

      TypeName typeName = Field.resolveType(method.getReturnType());
      if (!className.equals(typeName)) {
        continue;
      }

      List<Field> fields = setterFieldsFor(method);
      fields.forEach(
          it -> {
            if (descriptor.fields().containsKey(it.name())
                && !descriptor.methods().containsKey(it.name())) {
              descriptor.methods().put(it.name(), method);
            }
          });

      String field = BuilderDescriptor.getName(method.getName());

      if (!descriptor.fields().containsKey(field)) {
        continue;
      }
      descriptor.methods().put(field, method);
    }
    return descriptor;
  }

  private List<String> processMissingFields(PsiElement expression, PsiClass builderClass) {
    ClassName builderType = resolveType(builderClass);
    List<String> mandatoryFields = new ArrayList<>(getMandatoryFields(builderClass));
    Queue<PsiElement> queue = new LinkedList<>();
    Set<PsiElement> seenElements = new HashSet<>();

    queue.offer(expression);

    while (!queue.isEmpty()) {
      PsiElement cur = queue.poll();
      if (mandatoryFields.isEmpty()) {
        break;
      }

      if (cur != null) {
        seenElements.add(cur);

        if (cur.getText().contains("this.defaultValue")) {
          int i = 0;
        }

        if (cur instanceof PsiReturnStatement statement) {
          if (!seenElements.contains(statement.getReturnValue())) {
            queue.offer(statement.getReturnValue());
          }
        }

        if (cur instanceof PsiExpressionStatement statement) {
          if (!seenElements.contains(statement.getExpression())) {
            queue.offer(statement.getExpression());
          }
        }

        if (cur instanceof PsiAssignmentExpression assignmentExpression) {
          if (!seenElements.contains(assignmentExpression.getLExpression())) {
            queue.offer(assignmentExpression.getLExpression());
          }
        }

        if (cur instanceof PsiNewExpression newExpression) {
          if (Field.resolveType(newExpression.getType()).equals(builderType)) {
            PsiMethod method = newExpression.resolveMethod();
            if (method != null && !seenElements.contains(method)) {
              queue.offer(method);
            }
          }
        }

        if (cur instanceof PsiExpressionList expressionList) {
          if (expressionList.getParent() != null) {
            if (!seenElements.contains(expressionList.getParent())) {
              queue.offer(expressionList.getParent());
            }
          }
        }

        if (cur instanceof PsiMethodCallExpression methodCallExpression
            && !"(null)".equals(methodCallExpression.getArgumentList().getText())) {
          for (PsiExpression psiExpression :
              methodCallExpression.getArgumentList().getExpressions()) {
            if (!seenElements.contains(psiExpression)) {
              queue.offer(psiExpression);
            }
          }

          PsiMethod resolvedMethod = methodCallExpression.resolveMethod();
          if (resolvedMethod != null) {
            if (resolvedMethod.getBody() != null) {
              for (PsiStatement statement : resolvedMethod.getBody().getStatements()) {
                if (!seenElements.contains(statement)) {
                  queue.offer(statement);
                }
              }
            }

            if (resolvedMethod.getReturnType() != null) {
              TypeName returnType = Field.resolveType(resolvedMethod.getReturnType());
              if (!returnType.equals(builderType)) {
                for (PsiReturnStatement returnStatement :
                    PsiUtil.findReturnStatements(resolvedMethod)) {
                  if (!seenElements.contains(returnStatement.getReturnValue())) {
                    queue.offer(returnStatement.getReturnValue());
                  }
                }
              } else {
                // If the method returns a builder, then inspect the return statements for any
                // setters
                for (PsiReturnStatement returnStatement :
                    PsiUtil.findReturnStatements(resolvedMethod)) {
                  if (!seenElements.contains(returnStatement.getReturnValue())) {
                    queue.offer(returnStatement.getReturnValue());
                  }
                }
              }
            }
          }
        }

        if (cur instanceof PsiReferenceExpressionImpl) {
          PsiElement resolvedElement = ((PsiReferenceExpressionImpl) cur).resolve();
          if (resolvedElement instanceof PsiLocalVariable) {
            PsiElement initializer = ((PsiLocalVariable) resolvedElement).getInitializer();
            if (!seenElements.contains(initializer)) {
              queue.offer(initializer);
            }

            Query<PsiReference> references =
                ReferencesSearch.search(
                    resolvedElement,
                    GlobalSearchScope.fileScope(resolvedElement.getContainingFile()),
                    false);

            for (PsiReference reference : references) {
              if (reference.getElement().getTextRange().getStartOffset()
                  < cur.getTextRange().getStartOffset()) {
                PsiElement referenceParent = reference.getElement().getParent();
                if (!seenElements.contains(referenceParent)) {
                  queue.offer(referenceParent);
                }
              }
            }
          }

          if (resolvedElement instanceof PsiField field
              && cur.getParent() instanceof PsiAssignmentExpression) {
            if (field.getContainingClass() == builderClass) {
              mandatoryFields.remove(field.getName());
            }
          }
        }

        if (cur instanceof PsiMethodImpl method) {
          if (method.getBody() != null) {
            for (PsiStatement statement : method.getBody().getStatements()) {
              if (!seenElements.contains(statement)) {
                queue.offer(statement);
              }
            }
          }
        }

        for (PsiElement child : cur.getChildren()) {
          if (!seenElements.contains(child)) {
            queue.offer(child);
          }
        }
      }
    }

    return mandatoryFields;
  }

  private List<String> getMandatoryFields(PsiClass aClass) {
    BuilderDescriptor descriptor = descriptorFor(aClass);
    NullableNotNullManager manager = NullableNotNullManager.getInstance(aClass.getProject());

    return descriptor.methods().keySet().stream()
        .filter(
            name -> {
              PsiMethod method = descriptor.methods().get(name);
              PsiParameter parameter = method.getParameterList().getParameter(0);

              if (parameter != null && manager.isNullable(parameter, true)
                  || manager.isNullable(descriptor.fields().get(name).source(), true)) {
                return false;
              }

              if (requireNotNullAnnotation) {
                Field field = descriptor.fields().get(name);
                List<PsiAnnotation> annotations =
                    new ArrayList<>(Arrays.asList(field.source().getAnnotations()));
                if (parameter != null) {
                  annotations.addAll(Arrays.asList(parameter.getAnnotations()));
                }
                if (annotations.stream()
                    .noneMatch(NullableNotNullManager::isNullabilityAnnotation)) {
                  return false;
                }
                return (parameter != null && manager.isNotNull(parameter, true)
                    || manager.isNotNull(field.source(), true));
              }
              return true;
            })
        .toList();
  }

  @Override
  public @NotNull OptPane getOptionsPane() {
    return OptPane.pane(
        checkbox(
            "requireNotNullAnnotation", "Only validate elements explicitly annotated as @NotNull"),
        checkbox("ignoreExternalBuilders", "Ignore Builders external to the current project"),
        JavaInspectionControls.button(JavaInspectionButtons.ButtonKind.NULLABILITY_ANNOTATIONS));
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new JavaElementVisitor() {
      @Override
      public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);
        PsiMethod resolvedMethod = expression.resolveMethod();
        if (resolvedMethod != null && Objects.equals(resolvedMethod.getName(), "build")) {
          PsiClass builderClass = getContainingBuilderClass(resolvedMethod);
          if (builderClass == null) {
            return;
          }
          PsiFile javaFile = builderClass.getContainingFile().getOriginalFile();
          List<String> fields = processMissingFields(expression, builderClass);

          if (ignoreExternalBuilders && javaFile.getVirtualFile().getPath().contains(".jar!/")) {
            return;
          }

          if (!fields.isEmpty()) {
            holder.registerProblem(
                expression,
                InspectionBundle.message(
                        "inspection.record.builder.missing.required.problem.descriptor")
                    + " "
                    + fields,
                myQuickFix);
          }
        }
      }
    };
  }

  private class ReplaceWithRequired implements LocalQuickFix {
    /**
     * Returns a partially localized string for the quick fix intention. Used by the test code for
     * this plugin.
     *
     * @return Quick fix short name.
     */
    @NotNull
    @Override
    public String getName() {
      return InspectionBundle.message("inspection.record.builder.missing.required.use.quickfix");
    }

    @NotNull
    public String getFamilyName() {
      return getName();
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiMethodCallExpression expression = (PsiMethodCallExpression) descriptor.getPsiElement();
      PsiMethod resolvedMethod = expression.resolveMethod();

      if (resolvedMethod == null) {
        return;
      }

      PsiClass builderClass = getContainingBuilderClass(resolvedMethod);
      if (builderClass == null) {
        return;
      }

      BuilderDescriptor builderDescriptor = descriptorFor(builderClass);

      List<String> missingFields =
          processMissingFields(expression, builderClass).stream()
              .map(
                  it ->
                      Optional.ofNullable(builderDescriptor.methods().get(it))
                          .map(PsiMethod::getName)
                          .orElse(it))
              .toList();

      if (!missingFields.isEmpty()) {
        String errorText = expression.getText();
        for (String name : missingFields) {
          errorText = errorText.replaceAll("\\." + name + "\\([^)]*\\)", "");
        }

        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiMethodCallExpression fixedMethodExpression =
            (PsiMethodCallExpression)
                factory.createExpressionFromText(
                    replaceLast(errorText, "." + String.join("().", missingFields) + "().build()"),
                    null);
        expression.replace(fixedMethodExpression);
      }
    }

    private String replaceLast(String string, String replacement) {
      int pos = string.lastIndexOf(".build()");
      if (pos > -1) {
        return string.substring(0, pos) + replacement + string.substring(pos + ".build()".length());
      } else {
        return string;
      }
    }
  }
}
