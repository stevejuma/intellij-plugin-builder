RecordBuilder is an Intellij IDEA plugin for generating "Builder" pattern code for
Java records and POJO's.

The plugin compatibility is declared to be for any version of IDEA from 
`2023.3.` forward.

When written, the plugin was targeted at IDEA `2023.3.3` and thus assumes
you will use JDK 17 SDK to build and run it.

# Functionality

* The plugin will generate a builder for both your generic classes and 

# Usage guide

1. **Open the Generate Menu**:

* Navigate to `/ Code / Generate...` or use the shortcut `Alt + Insert`.


2. **Select Create Builder...**:

* From the popup list, choose `Create Builder...`.


3. **Customize Your Builder**:

* A dialog appears allowing field selection and Options Confirm to generate the builder
  pattern.

# Options 

| Config                                                     | Description                                                                                                                                                                            |
|------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Builder Notation                                           | The prefix to use for the builder setter methods. Either `get`, `with` or no prefix similar to record method names                                                                     |
| Generate Static Builder Method                             | Whether to generate a static builder method. If not set, the generated builder will have a public constructor                                                                          |
| Static Builder Name                                        | The name to use for the static builder method. Either `builder()`. `newBuilder()`, `new[ClassName]()` `a[ClassName]()`, `new[ClassName]Builder()`                                      |
| Static Builder Method Location                             | Where the static builder method should be located, either in the `parent class` or inside the `generated builder` itself                                                               |
| Generate builder copy constructor and `toBuilder()` method | Generate a copy method on the parent class that creates a builder from itself                                                                                                          |
| Copy Method Name                                           | The name of the copy method `copy()` `but()` or `toBuilder()`                                                                                                                          |
| Generate Immutable Collections                             | Force all collections `List, Set, Map` into immutable types using the `copyOf` method of the type                                                                                      |
| Null Handling Strategy                                     | How to deal with nulls, `Assume all fields required unless annotated with @Nullable or @Null` or `Assume all fields optional unless annotated with @NotNull/ @NonNull` or `Do Nothing` |
| Copy Validation Statements                                 | For records copy the `validate()` method in the builder into the canonical constructor                                                                                                 |
| Add JavaDoc                                                | Add JavaDoc to the generated builder methods                                                                                                                                           |
| Generate `.toString()` method                              | Generate a `toString()` method for the parent class. Can either generate a pretty to string or a generic one                                                                           |

