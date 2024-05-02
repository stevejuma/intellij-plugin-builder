package ma.ju.intellij.builder.ide;

import com.intellij.codeInsight.generation.PsiElementClassMember;
import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.ide.util.MemberChooser;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.NonFocusableCheckBox;
import ma.ju.intellij.builder.psi.Field;
import ma.ju.intellij.builder.psi.BuilderGenerator;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

public class RecordMemberChooser {
  private static final SelectorOption.DropDown.Renderer RENDERER =
      new SelectorOption.DropDown.Renderer();
  private static final List<SelectorOption> OPTIONS = createGeneratorOptions();

  private static List<SelectorOption> createGeneratorOptions() {
    final List<SelectorOption> options = new ArrayList<>();
    options.add(
        new SelectorOption.DropDown(
            BuilderOption.BUILDER_METHOD_PREFIX_DROPDOWN,
            "The notation to use for generating the builder (with.../set...)",
            "Generate builder methods that start with 'set/with', for example: "
                + "builder.setName(String name) / builder.withName(String name) / builder.name(String name)",
            List.of(
                new SelectorOption.DropDown.Value(
                    BuilderOption.BUILDER_METHOD_PREFIX_SET, "Use 'set...' notation"),
                new SelectorOption.DropDown.Value(
                    BuilderOption.BUILDER_METHOD_PREFIX_WITH, "Use 'with...' notation"),
                new SelectorOption.DropDown.Value(
                    BuilderOption.BUILDER_METHOD_PREFIX_NONE, "Use field name notation"))));
    options.add(
        new SelectorOption.CheckBox(
            BuilderOption.NEW_BUILDER_METHOD, "Generate static builder method", ""));
    options.add(
        new SelectorOption.DropDown(
            BuilderOption.STATIC_BUILDER_DROPDOWN,
            "Static builder naming",
            "Select what the static builder method should look like.",
            List.of(
                new SelectorOption.DropDown.Value(
                    BuilderOption.STATIC_BUILDER_BUILDER_NAME, "builder()"),
                new SelectorOption.DropDown.Value(
                    BuilderOption.STATIC_BUILDER_NEW_BUILDER_NAME, "newBuilder()"),
                new SelectorOption.DropDown.Value(
                    BuilderOption.STATIC_BUILDER_NEW_CLASS_NAME, "new[ClassName]()"),
                new SelectorOption.DropDown.Value(
                    BuilderOption.STATIC_BUILDER_NEW_A_CLASS_NAME, "a[ClassName]()"),
                new SelectorOption.DropDown.Value(
                    BuilderOption.STATIC_BUILDER_NEW_CLASS_NAME_BUILDER,
                    "new[ClassName]Builder()"))));

    options.add(
        new SelectorOption.DropDown(
            BuilderOption.BUILDER_METHOD_LOCATION_DROPDOWN,
            "Builder method location",
            "Select where the builder method should be located.",
            List.of(
                new SelectorOption.DropDown.Value(
                    BuilderOption.BUILDER_METHOD_IN_PARENT_CLASS, "Inside parent class"),
                new SelectorOption.DropDown.Value(
                    BuilderOption.BUILDER_METHOD_IN_BUILDER, "Inside generated Builder class"))));
    options.add(
        new SelectorOption.CheckBox(
            BuilderOption.TO_BUILDER_COPY_CONSTRUCTOR,
            "Generate builder copy constructor and toBuilder() method",
            "Generate builder copy constructor and copy method"));
    options.add(
        new SelectorOption.DropDown(
            BuilderOption.TO_BUILDER_METHOD_NAME,
            "ToBuilder/Copy method name",
            "Select the name of the method to use for converting the type into a builder",
            List.of(
                new SelectorOption.DropDown.Value(
                    BuilderOption.TO_BUILDER_METHOD_NAME_TO_BUILDER, "toBuilder()"),
                new SelectorOption.DropDown.Value(
                    BuilderOption.TO_BUILDER_METHOD_NAME_COPY, "copy()"),
                new SelectorOption.DropDown.Value(
                    BuilderOption.TO_BUILDER_METHOD_NAME_BUT, "but()"))));
    options.add(
        new SelectorOption.CheckBox(
            BuilderOption.WITH_IMMUTABLE_COLLECTIONS,
            "Generate Immutable Collections",
            "Generate immutable collections using `.copyOf(...)`"));
    options.add(
        new SelectorOption.DropDown(
            BuilderOption.NULL_HANDLING,
            "Null Handling Strategy",
            "Select the name of the method to use for converting the type into a builder",
            List.of(
                new SelectorOption.DropDown.Value(
                    BuilderOption.NULL_HANDLING_REQUIRED,
                    "All Fields required unless annotated with @Nullable/@Null"),
                new SelectorOption.DropDown.Value(
                    BuilderOption.NULL_HANDLING_OPTIONAL,
                    "All Fields optional unless annotated with @NonNull/@NotNull"),
                new SelectorOption.DropDown.Value(
                    BuilderOption.NULL_HANDLING_DISABLED, "Nulls not validated"))));
    options.add(
        new SelectorOption.CheckBox(
            BuilderOption.WITH_RECORD_VALIDATION_STATEMENTS,
            "Copy Validation Statements",
            "Copy statements in the builder's validate method into the records canonical constructor"));

    options.add(
        new SelectorOption.CheckBox(
            BuilderOption.WITH_JAVADOC,
            "Add Javadoc",
            "Add Javadoc to generated builder class and methods"));

    options.add(
        new SelectorOption.DropDown(
            BuilderOption.TO_STRING_METHOD_DROPDOWN,
            "Generate .toString() method",
            "Generate a .toString() method on the parent class",
            List.of(
                new SelectorOption.DropDown.Value(
                    BuilderOption.TO_STRING_METHOD_NONE, "Do Not Generate .toString() method"),
                new SelectorOption.DropDown.Value(
                    BuilderOption.TO_STRING_METHOD_PRETTY,
                    "Generate Pretty .toString() method for Non Records"),
                new SelectorOption.DropDown.Value(
                    BuilderOption.TO_STRING_METHOD_GENERIC,
                    "Generate a Generic .toString() method (Record)"))));

    return options;
  }

  private static JComponent[] buildOptions() {
    final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
    final int optionCount = OPTIONS.size();
    final JComponent[] checkBoxesArray = new JComponent[optionCount];
    for (int i = 0; i < optionCount; i++) {
      checkBoxesArray[i] = buildOption(propertiesComponent, OPTIONS.get(i));
    }
    return checkBoxesArray;
  }

  private static JComponent buildOption(
      final PropertiesComponent propertiesComponent, final SelectorOption selectorOption) {
    if (selectorOption instanceof SelectorOption.CheckBox checkBox) {
      return buildCheckbox(propertiesComponent, checkBox);
    } else if (selectorOption instanceof SelectorOption.DropDown dropDown) {
      return buildDropdown(propertiesComponent, dropDown);
    }
    throw new IllegalArgumentException();
  }

  private static JComponent buildCheckbox(
      PropertiesComponent propertiesComponent, SelectorOption.CheckBox selectorOption) {
    final JCheckBox optionCheckBox = new NonFocusableCheckBox(selectorOption.caption());
    optionCheckBox.setToolTipText(selectorOption.tooltip());

    final String optionProperty = selectorOption.option().property();
    optionCheckBox.setSelected(propertiesComponent.isTrueValue(optionProperty));
    optionCheckBox.addItemListener(
        event ->
            propertiesComponent.setValue(
                optionProperty, Boolean.toString(optionCheckBox.isSelected())));
    return optionCheckBox;
  }

  private static JComponent buildDropdown(
      PropertiesComponent propertiesComponent, SelectorOption.DropDown selectorOption) {
    final ComboBox<SelectorOption.DropDown.Value> comboBox = new ComboBox<>();
    comboBox.setEditable(false);
    comboBox.setRenderer(RENDERER);
    selectorOption.values().forEach(comboBox::addItem);

    comboBox.setSelectedItem(setSelectedComboBoxItem(propertiesComponent, selectorOption));
    comboBox.addItemListener(
        event -> setPropertiesComponentValue(propertiesComponent, selectorOption, event));

    LabeledComponent<ComboBox<SelectorOption.DropDown.Value>> labeledComponent =
        LabeledComponent.create(comboBox, selectorOption.caption());
    labeledComponent.setToolTipText(selectorOption.tooltip());

    return labeledComponent;
  }

  private static SelectorOption.DropDown.Value setSelectedComboBoxItem(
      PropertiesComponent propertiesComponent, SelectorOption.DropDown selectorOption) {
    String selectedValue = propertiesComponent.getValue(selectorOption.option().property());
    return selectorOption.values().stream()
        .filter(it -> Objects.equals(it.option().property(), selectedValue))
        .findFirst()
        .orElse(selectorOption.values().get(0));
  }

  private static void setPropertiesComponentValue(
      PropertiesComponent propertiesComponent,
      SelectorOption.DropDown selectorOption,
      ItemEvent itemEvent) {
    SelectorOption.DropDown.Value value = (SelectorOption.DropDown.Value) itemEvent.getItem();
    propertiesComponent.setValue(selectorOption.option().property(), value.option().property());
  }

  public static List<Field> chooseFieldNames(PsiClass recordClass) {
    return chooseFieldNames("Select Fields to Be Available in Builder", recordClass);
  }

  /** Displays the confirmation dialog where users can choose what fields to generate. */
  public static List<Field> chooseFieldNames(String title, PsiClass... recordClasses) {
    List<PsiFieldMember> members = new ArrayList<>();
    if (recordClasses.length == 0) {
      return emptyList();
    }
    PsiClass recordClass = recordClasses[0];

    for (PsiClass clazz : recordClasses) {
      members.addAll(RecordMemberChooser.mapAllFieldMembers(clazz));
    }

    final JComponent[] optionCheckBoxes = buildOptions();
    MemberChooser<PsiFieldMember> chooser =
        new MemberChooser<>(
            members.toArray(PsiFieldMember[]::new),
            false, // allowEmptySelection
            true, // allowMultiSelection
            recordClass.getProject(),
            null,
            optionCheckBoxes);
    chooser.setCopyJavadocVisible(false);
    chooser.selectElements(
        members.stream()
            .filter(RecordMemberChooser::isDefaultSelection)
            .toArray(PsiFieldMember[]::new));
    chooser.setTitle(title);

    if (chooser.showAndGet()) {
      // return the chosen fields as a list of field names
      List<PsiFieldMember> selectedMembers = requireNonNull(chooser.getSelectedElements());
      return selectedMembers.stream()
          .map(PsiElementClassMember::getElement)
          .map(Field::of)
          .toList();
    }
    return emptyList();
  }

  public static List<Field> mapRecordComponentNames(PsiClass recordClass) {
    return BuilderGenerator.getComponents(recordClass).stream().toList();
  }

  /**
   * Return all fields of the given class as "members". Chooser has its own "Member" abstraction
   * wrapped around the PSI types.
   */
  public static List<PsiFieldMember> mapAllFieldMembers(PsiClass psiClass) {
    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());

    PsiClass type = elementFactory.createClass(requireNonNull(psiClass.getName()));
    type.setName(psiClass.getName());

    Set<PsiFieldMember> members =
        new LinkedHashSet<>(stream(psiClass.getAllFields()).map(PsiFieldMember::new).toList());
    for (Field component : mapRecordComponentNames(psiClass)) {
      if (component.source() instanceof PsiMethod method && method.getReturnType() != null) {
        PsiField field =
            (PsiField)
                type.addBefore(
                    elementFactory.createField(method.getName(), method.getReturnType()),
                    type.getLastChild());
        members.add(new PsiFieldMember(field));
      }
    }
    return List.copyOf(members);
  }

  /** Defines fields should be selected by default. */
  public static boolean isDefaultSelection(PsiFieldMember field) {
    return !field.getElement().getName().equals("serialVersionUID");
  }
}
