package ma.ju.intellij.builder;

import static org.assertj.core.api.Assertions.assertThat;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiPlainTextFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import java.util.List;
import java.util.function.Function;
import ma.ju.intellij.builder.ide.RecordMemberChooser;
import ma.ju.intellij.builder.psi.BuilderGenerator;
import ma.ju.intellij.builder.psi.BuilderSettings;
import ma.ju.intellij.builder.psi.Field;

public abstract class BuilderTestCase extends LightJavaCodeInsightFixtureTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.setTestDataPath("src/test/resources");
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public String getPackagePath() {
    return "records";
  }

  public PsiJavaFile getTestPsiJavaFile(String filename) {
    var file = myFixture.configureByFile(getPackagePath() + "/" + filename);

    assertThat(file).isNotNull();
    assertThat(file).isInstanceOf(PsiJavaFile.class);

    return (PsiJavaFile) file;
  }

  public PsiPlainTextFile getTestPsiTextFile(String filename) {
    var file = myFixture.configureByFile(getPackagePath() + "/" + filename);

    assertThat(file).isNotNull();
    assertThat(file).isInstanceOf(PsiPlainTextFile.class);

    return (PsiPlainTextFile) file;
  }

  public void verifyDeletedContents(String input, BuilderSettings settings) {
    verifyDeletedContents(input, settings, RecordMemberChooser::mapRecordComponentNames);
  }

  public void verifyDeletedContents(
      String input, BuilderSettings settings, Function<PsiClass, List<Field>> callback) {
    var inputJava = getTestPsiJavaFile(input + ".java");
    var outputText = getTestPsiJavaFile(input + ".after.java");
    var recordClass = inputJava.getClasses()[0];
    WriteCommandAction.runWriteCommandAction(
        inputJava.getProject(),
        () -> BuilderGenerator.delete(recordClass, callback.apply(recordClass), settings));
    assertThat(inputJava.getText()).isEqualToIgnoringWhitespace(outputText.getText());
  }

  public void verifyContents(String input, BuilderSettings settings) {
    var inputJava = getTestPsiJavaFile(input + ".java");
    var outputText = getTestPsiJavaFile(input + ".after.java");
    var recordClass = inputJava.getClasses()[0];
    WriteCommandAction.runWriteCommandAction(
        inputJava.getProject(), () -> BuilderGenerator.generate(recordClass, settings));
    assertThat(inputJava.getText()).isEqualToIgnoringWhitespace(outputText.getText());
  }
}
