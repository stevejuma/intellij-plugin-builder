package ma.ju.intellij.builder.ide;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class DeleteBuilderAction extends BaseCodeInsightAction {
  private final DeleteBuilderHandler handler = new DeleteBuilderHandler();

  @Override
  protected @NotNull CodeInsightActionHandler getHandler() {
    return handler;
  }

  @Override
  protected boolean isValidForFile(
      @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    return handler.isValidFor(editor, file);
  }
}
