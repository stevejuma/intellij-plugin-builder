package ma.ju.intellij.builder.ide;

import com.intellij.ui.SimpleListCellRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public interface SelectorOption {
  BuilderOption option();

  String caption();

  String tooltip();

  record CheckBox(BuilderOption option, String caption, String tooltip) implements SelectorOption {}

  record DropDown(BuilderOption option, String caption, String tooltip, List<Value> values)
      implements SelectorOption {
    public record Value(BuilderOption option, String caption) {}

    public static class Renderer extends SimpleListCellRenderer<Value> {
      @Override
      public void customize(
          @NotNull JList<? extends Value> list,
          SelectorOption.DropDown.Value value,
          int index,
          boolean selected,
          boolean hasFocus) {
        setText(value.caption());
      }
    }
  }
}
