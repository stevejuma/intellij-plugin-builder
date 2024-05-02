package ma.ju.intellij.builder.ide;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum BuilderOption {
  NEW_BUILDER_METHOD("newBuilderMethod"),
  STATIC_BUILDER_DROPDOWN("staticBuilderDropdown", false),
  STATIC_BUILDER_NEW_BUILDER_NAME("staticBuilderNewBuilderName", false),
  STATIC_BUILDER_BUILDER_NAME("staticBuilderBuilderName", false),
  STATIC_BUILDER_NEW_CLASS_NAME("staticBuilderNewClassName", false),
  STATIC_BUILDER_NEW_A_CLASS_NAME("staticBuilderNewClassName", false),

  STATIC_BUILDER_NEW_CLASS_NAME_BUILDER("staticBuilderNewClassNameBuilder", false),

  BUILDER_METHOD_LOCATION_DROPDOWN("builderMethodDropdownLocation", false),
  BUILDER_METHOD_IN_PARENT_CLASS("builderMethodInParentClass", false),
  BUILDER_METHOD_IN_BUILDER("builderMethodInBuilder", false),

  TO_BUILDER_COPY_CONSTRUCTOR("toBuilderCopyConstructor"),
  TO_BUILDER_METHOD_NAME("toBuilderMethodNameDropdown", false),
  TO_BUILDER_METHOD_NAME_TO_BUILDER("toBuilderMethodNameToBuilder", false),
  TO_BUILDER_METHOD_NAME_BUT("toBuilderMethodNameBut", false),
  TO_BUILDER_METHOD_NAME_COPY("toBuilderMethodNameCopy", false),
  BUILDER_METHOD_PREFIX_DROPDOWN("builderMethodPrefixDropdown", false),
  BUILDER_METHOD_PREFIX_WITH("builderMethodPrefixWith", false),
  BUILDER_METHOD_PREFIX_SET("builderMethodPrefixSet", false),
  BUILDER_METHOD_PREFIX_NONE("builderMethodPrefixNone", false),
  NULL_HANDLING("nullHandlingDropdown", false),
  NULL_HANDLING_REQUIRED("nullHandlingRequired", false),
  NULL_HANDLING_OPTIONAL("nullHandlingOptional", false),
  NULL_HANDLING_DISABLED("nullHandlingDisabled", false),
  WITH_JAVADOC("withJavadoc"),
  TO_STRING_METHOD_DROPDOWN("toStringMethodDropDown", false),
  TO_STRING_METHOD_NONE("toStringMethodNone", false),
  TO_STRING_METHOD_PRETTY("toStringMethodPretty", false),
  TO_STRING_METHOD_GENERIC("toStringMethodGeneric", false),

  WITH_RECORD_VALIDATION_STATEMENTS("withRecordValidationStatements"),
  WITH_IMMUTABLE_COLLECTIONS("withImmutableCollections");

  private final String property;
  private final Boolean booleanProperty;

  BuilderOption(final String property) {
    this(property, true);
  }

  BuilderOption(final String property, final Boolean booleanProperty) {
    this.property = String.format("GenerateRecordBuilder.%s", property);
    this.booleanProperty = booleanProperty;
  }

  public String property() {
    return property;
  }

  public Boolean isBoolean() {
    return booleanProperty;
  }

  public static Optional<BuilderOption> findValue(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    return Arrays.stream(values()).filter(it -> Objects.equals(it.property(), value)).findFirst();
  }
}
