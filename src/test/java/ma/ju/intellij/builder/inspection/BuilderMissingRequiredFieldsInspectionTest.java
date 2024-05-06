package ma.ju.intellij.builder.inspection;

import static org.assertj.core.api.Assertions.assertThat;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.testFramework.TestDataPath;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import java.util.List;
import org.jetbrains.annotations.NotNull;

@TestDataPath("$CONTENT_ROOT/testData")
public class BuilderMissingRequiredFieldsInspectionTest
    extends LightJavaCodeInsightFixtureTestCase {
  private static final String HIGHLIGHT_NAME =
      InspectionBundle.message("inspection.record.builder.missing.required.display.name");
  private static final String QUICK_FIX_NAME =
      InspectionBundle.message("inspection.record.builder.missing.required.use.quickfix");

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new BuilderMissingRequiredFieldsInspection());
    // optimization: add a fake java.lang.String class to avoid loading all JDK classes for test
    myFixture.addClass("package java.lang; public final class String {}");
  }

  /**
   * Defines the path to files used for running tests.
   *
   * @return The path from this module's root directory ($MODULE_WORKING_DIR$) to the directory
   *     containing files for these tests.
   */
  @Override
  protected String getTestDataPath() {
    return "src/test/testData";
  }

  public void testMissingNull() {
    doTest("Inspection", "Inspection.after.java");
  }

  public void testNullAssignment() {
    doTest("InspectionNull", "Inspection.after.java");
  }

  public void testReferencedBuilderSetterDetected() {
    doTest("InspectionReference");
  }

  public void testDefaultsDetected() {
    doTest("InspectionDefaults");
  }

  public void testAssignmentsDetected() {
    doTest("InspectionAssignments");
  }

  public void testToBuilder() {
    doTest("InspectionToBuilder");
  }

  protected void doTest(@NotNull String testName) {
    doTest(testName, "");
  }

  protected void doTest(@NotNull String testName, String expected) {
    // Initialize the test based on the testData file
    myFixture.configureByFile(testName + ".java");
    // Initialize the inspection and get a list of highlighted
    List<HighlightInfo> highlightInfos =
        myFixture.doHighlighting().stream()
            .filter(
                it -> it.getDescription() != null && it.getDescription().startsWith(HIGHLIGHT_NAME))
            .toList();
    // Get the quick fix action for comparing references inspection and apply it to the file

    if (expected != null && !expected.isBlank()) {
      assertThat(highlightInfos).isNotEmpty();
      myFixture.getAllQuickFixes(testName + ".java").stream()
          .filter(it -> it.getFamilyName().equals(QUICK_FIX_NAME))
          .findFirst()
          .ifPresent(it -> myFixture.launchAction(it));

      // Verify the results
      myFixture.checkResultByFile(expected);
    } else {
      assertThat(highlightInfos).isEmpty();
    }
  }
}
