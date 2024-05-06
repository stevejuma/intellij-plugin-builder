package ma.ju.intellij.inspections;

import java.util.Objects;

public class Inspection {
  public final NameAndAge person;

  public Inspection() {
    this.person = NameAndAge.builder().setAge(22).build();
  }

  public record NameAndAge(String name, int age) {
    public NameAndAge {
      Objects.requireNonNull(name, "property :name is required");
    }

    @Override
    public String toString() {
      return getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this));
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private String name;

      private int age;

      private Builder() {}

      private Builder(NameAndAge record) {
        this.name = record.name;
        this.age = record.age;
      }

      /**
       * Sets the {@code name} and returns a reference to this Builder enabling method chaining.
       *
       * @param name the {@code name} to set
       * @see NameAndAge#name
       * @return a reference to this Builder
       */
      public Builder setName(String name) {
        this.name = name;
        return this;
      }

      /**
       * Sets the {@code age} and returns a reference to this Builder enabling method chaining.
       *
       * @param age the {@code age} to set
       * @see NameAndAge#age
       * @return a reference to this Builder
       */
      public Builder setAge(int age) {
        this.age = age;
        if (this.name == null) {
          this.name = "Aged: " + age;
        }
        return this;
      }

      /**
       * Returns a {@code NameAndAge} built from the parameters previously set.
       *
       * @see NameAndAge
       * @return a {@code NameAndAge} built with parameters of this {@code NameAndAge.Builder}
       */
      public NameAndAge build() {
        StringBuilder missing = new StringBuilder();
        if (this.name == null) {
          missing.append(" name");
        }
        if (!missing.isEmpty()) {
          throw new IllegalStateException("Missing required properties:" + missing);
        }
        return new NameAndAge(this.name, this.age);
      }
    }
  }
}
