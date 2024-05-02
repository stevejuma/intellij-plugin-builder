package ma.ju.intellij.builder.psi;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.psi.PsiClass;
import ma.ju.intellij.builder.ide.BuilderOption;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class BuilderSettings {
  private final EnumSet<BuilderOption> options;
  private static final ThreadLocal<BuilderSettings> settingsThreadLocal = new ThreadLocal<>();

  public BuilderSettings(EnumSet<BuilderOption> options) {
    this.options = options;
  }


  public String methodPrefix() {
    if (options.contains(BuilderOption.BUILDER_METHOD_PREFIX_SET)) {
      return "set";
    } else if (options.contains(BuilderOption.BUILDER_METHOD_PREFIX_WITH)) {
      return "with";
    } else if (options.contains(BuilderOption.BUILDER_METHOD_PREFIX_NONE)) {
      return "";
    }
    return "set";
  }

  public List<String> methodPrefixes() {
    return List.of("set", "with", "");
  }

  public boolean generateBuilderMethod() {
    return options.contains(BuilderOption.NEW_BUILDER_METHOD);
  }

  public boolean recordValidationStatements() {
    return options.contains(BuilderOption.WITH_RECORD_VALIDATION_STATEMENTS);
  }

  public String builderMethodName(PsiClass psiClass) {
    if (options.contains(BuilderOption.STATIC_BUILDER_BUILDER_NAME)) {
      return "builder";
    } else if (options.contains(BuilderOption.STATIC_BUILDER_NEW_BUILDER_NAME)) {
      return "newBuilder";
    } else if (options.contains(BuilderOption.STATIC_BUILDER_NEW_CLASS_NAME)) {
      return "new" + psiClass.getName();
    } else if (options.contains(BuilderOption.STATIC_BUILDER_NEW_A_CLASS_NAME)) {
      return "a" + psiClass.getName();
    } else if (options.contains(BuilderOption.STATIC_BUILDER_NEW_CLASS_NAME_BUILDER)) {
      return "new" + psiClass.getName() + "Builder";
    }
    return "builder";
  }

  public Set<String> builderMethodNames(PsiClass psiClass) {
    return new HashSet<>(
        List.of(
            "builder",
            "newBuilder",
            "new" + psiClass.getName(),
            "a" + psiClass.getName(),
            "new" + psiClass.getName() + "Builder"));
  }

  public boolean builderMethodInBuilder() {
    return options.contains(BuilderOption.BUILDER_METHOD_IN_BUILDER);
  }

  public String copyMethodName() {
    if (options.contains(BuilderOption.TO_BUILDER_METHOD_NAME_TO_BUILDER)) {
      return "toBuilder";
    } else if (options.contains(BuilderOption.TO_BUILDER_METHOD_NAME_COPY)) {
      return "copy";
    } else if (options.contains(BuilderOption.TO_BUILDER_METHOD_NAME_BUT)) {
      return "but";
    }
    return "toBuilder";
  }

  public Set<String> copyMethodNames() {
    return Set.of("toBuilder", "copy", "but");
  }

  public boolean immutableCollections() {
    return options.contains(BuilderOption.WITH_IMMUTABLE_COLLECTIONS);
  }

  public boolean generateJavaDoc() {
    return options.contains(BuilderOption.WITH_JAVADOC);
  }

  public boolean generateCopyMethod() {
    return options.contains(BuilderOption.TO_BUILDER_COPY_CONSTRUCTOR);
  }

  public boolean validateNulls() {
    return !options.contains(BuilderOption.NULL_HANDLING_DISABLED);
  }

  public boolean generateToStringMethod() {
    return (options.contains(BuilderOption.TO_STRING_METHOD_PRETTY)
            || options.contains(BuilderOption.TO_STRING_METHOD_GENERIC))
        || !options.contains(BuilderOption.TO_STRING_METHOD_NONE);
  }

  public boolean generatePrettyToString() {
    return generateToStringMethod() && options.contains(BuilderOption.TO_STRING_METHOD_PRETTY);
  }

  public boolean nullHandlingRequired() {
    return validateNulls() && !options.contains(BuilderOption.NULL_HANDLING_OPTIONAL);
  }

  private static BuilderSettings getInstance() {
    EnumSet<BuilderOption> options = EnumSet.noneOf(BuilderOption.class);
    final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
    for (final BuilderOption option : BuilderOption.values()) {
      if (option.isBoolean()) {
        final boolean currentSetting = propertiesComponent.getBoolean(option.property(), false);
        if (currentSetting) {
          options.add(option);
        }
      } else {
        BuilderOption.findValue(propertiesComponent.getValue(option.property()))
            .ifPresent(options::add);
      }
    }
    return new BuilderSettings(options);
  }

  public static BuilderSettings settings() {
    BuilderSettings value = settingsThreadLocal.get();
    if (value == null) {
      value = getInstance();
      settingsThreadLocal.set(value);
    }
    return value;
  }

  public static BuilderSettings init() {
    return init(getInstance());
  }

  public static BuilderSettings init(BuilderSettings value) {
    settingsThreadLocal.set(value);
    return value;
  }
}
