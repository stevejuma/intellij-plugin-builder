package ma.ju.intellij.builder.ide;

import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.Arrays;
import java.util.List;
import ma.ju.intellij.builder.psi.BuilderGenerator;
import ma.ju.intellij.builder.psi.BuilderSettings;
import ma.ju.intellij.builder.psi.Field;
import org.jetbrains.annotations.NotNull;

public class DeleteBuilderHandler implements LanguageCodeInsightActionHandler {
  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  public boolean isValidFor(Editor editor, PsiFile psiFile) {
    if (!(psiFile instanceof PsiJavaFile)) {
      return false;
    }
    if (editor.getProject() == null) {
      return false;
    }
    PsiClass psiClass = getClass(psiFile, editor);
    if (psiClass == null) {
      return false;
    }
    for (PsiClass innerClass : psiClass.getInnerClasses()) {
      if ("Builder".equals(innerClass.getName())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
    final PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
    final Document currentDocument = psiDocumentManager.getDocument(psiFile);
    PsiClass psiClass = getClass(psiFile, editor);
    if (psiClass == null || currentDocument == null) {
      return;
    }
    psiDocumentManager.commitDocument(currentDocument);
    if (!EditorModificationUtil.checkModificationAllowed(editor)) {
      return;
    }
    if (!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)) {
      return;
    }
    PsiClass builderClass =
        Arrays.stream(psiClass.getInnerClasses())
            .filter(it -> "Builder".equals(it.getName()))
            .findFirst()
            .orElse(null);
    List<Field> fields =
        RecordMemberChooser.chooseFieldNames(
            "Select Fields to Be Deleted From The Builder", psiClass, builderClass);
    if (fields == null || fields.isEmpty()) {
      return;
    }
    WriteCommandAction.runWriteCommandAction(
        psiClass.getProject(),
        () -> BuilderGenerator.delete(psiClass, fields, BuilderSettings.init()));
  }

  public static PsiClass getClass(PsiFile file, Editor editor) {
    final int offset = editor.getCaretModel().getOffset();
    final PsiElement element = file.findElementAt(offset);
    if (element == null) {
      return null;
    }
    return PsiTreeUtil.getParentOfType(element, PsiClass.class, false);
  }
}
