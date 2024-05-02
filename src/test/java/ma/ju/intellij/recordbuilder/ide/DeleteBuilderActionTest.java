package ma.ju.intellij.recordbuilder.ide;

import java.util.EnumSet;
import java.util.List;

import ma.ju.intellij.builder.ide.BuilderOption;
import ma.ju.intellij.builder.psi.BuilderSettings;
import ma.ju.intellij.recordbuilder.BuilderTestCase;
import ma.ju.intellij.builder.psi.Field;

public class DeleteBuilderActionTest extends BuilderTestCase {
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
          BuilderOption.NULL_HANDLING_REQUIRED);

  public void testDeleteCustomMethod() {
    EnumSet<BuilderOption> opts = EnumSet.copyOf(options);
    opts.add(BuilderOption.WITH_JAVADOC);
    verifyDeletedContents("DeleteCustomRecord.java", new BuilderSettings(opts));
  }

  public void testDeleteSingleMethod() {
    EnumSet<BuilderOption> opts = EnumSet.copyOf(options);
    opts.add(BuilderOption.WITH_JAVADOC);
    verifyDeletedContents(
        "DeleteSingleMethod.java",
        new BuilderSettings(opts),
        psiClass -> List.of(Field.of(psiClass.getRecordComponents()[0])));
  }
}
