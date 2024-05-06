package ma.ju.intellij.builder.ide;

import static org.assertj.core.api.Assertions.assertThat;

import com.intellij.openapi.command.WriteCommandAction;
import ma.ju.intellij.builder.psi.BuilderGenerator;
import ma.ju.intellij.builder.psi.BuilderSettings;
import ma.ju.intellij.builder.BuilderTestCase;

import java.util.EnumSet;

public class RecordBuilderActionTest extends BuilderTestCase {
  private static final EnumSet<BuilderOption> options =
      EnumSet.of(
          BuilderOption.NEW_BUILDER_METHOD,
          BuilderOption.STATIC_BUILDER_DROPDOWN,
          BuilderOption.STATIC_BUILDER_BUILDER_NAME,
          BuilderOption.TO_BUILDER_COPY_CONSTRUCTOR,
          BuilderOption.BUILDER_METHOD_PREFIX_DROPDOWN,
          BuilderOption.BUILDER_METHOD_PREFIX_SET,
          BuilderOption.NULL_HANDLING,
          BuilderOption.WITH_RECORD_VALIDATION_STATEMENTS,
          BuilderOption.TO_STRING_METHOD_DROPDOWN,
          BuilderOption.TO_STRING_METHOD_GENERIC,
          BuilderOption.NULL_HANDLING_REQUIRED);

  public void testVarArgRecord() {
    verifyContents("VarArgRecord", new BuilderSettings(options));
  }

  public void testPoJo() {
    verifyContents("PoJo", new BuilderSettings(options));
  }

  public void testCustomMethod() {
    EnumSet<BuilderOption> opts = EnumSet.copyOf(options);
    opts.add(BuilderOption.WITH_JAVADOC);
    verifyContents("CustomRecord", new BuilderSettings(opts));
  }

  public void testNestedRecord() {
    var inputJava = getTestPsiJavaFile("NestedRecord.java");
    var outputText = getTestPsiJavaFile("NestedRecord.after.java");

    var recordClass = inputJava.getClasses()[0].getInnerClasses()[0];
    WriteCommandAction.runWriteCommandAction(
        inputJava.getProject(), () -> BuilderGenerator.generate(recordClass));
    assertThat(inputJava.getText()).isEqualToIgnoringWhitespace(outputText.getText());
  }

  public void testInterface() {
    EnumSet<BuilderOption> opts = EnumSet.copyOf(options);
    opts.add(BuilderOption.WITH_JAVADOC);
    verifyContents("InterfaceRecord", new BuilderSettings(opts));
  }

  public void testRegenerateRecord() {
    EnumSet<BuilderOption> opts = EnumSet.copyOf(options);
    opts.add(BuilderOption.WITH_JAVADOC);
    verifyContents("RegenerateRecord", new BuilderSettings(opts));
  }
}
