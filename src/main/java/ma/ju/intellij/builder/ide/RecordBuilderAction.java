package ma.ju.intellij.builder.ide;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class RecordBuilderAction extends BaseCodeInsightAction {
  private final RecordBuilderHandler handler = new RecordBuilderHandler();

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
