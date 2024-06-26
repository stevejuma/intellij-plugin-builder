package ma.ju.intellij.builder.psi;

public class NestedRecord {

  public static void method() {}

  public record Sample(String field1, String field2) {
    public Sample {
      java.util.Objects.requireNonNull(field1, "property :field1 is required");
      java.util.Objects.requireNonNull(field2, "property :field2 is required");
    }

    @java.lang.Override
    public java.lang.String toString() {
      return getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this));
    }

    public static final class Builder {
      private String field1;

      private String field2;

      public Builder() {}

      public Builder setField1(String field1) {
        this.field1 = Objects.requireNonNull(field1, "Null field1");
        return this;
      }

      public Builder setField2(String field2) {
        this.field2 = Objects.requireNonNull(field2, "Null field2");
        return this;
      }

      public Sample build() {
        if (this.field1 == null || this.field2 == null) {
          StringBuilder missing = new StringBuilder();
          if (this.field1 == null) {
            missing.append(" field1");
          }
          if (this.field2 == null) {
            missing.append(" field2");
          }
          throw new IllegalStateException("Missing required properties:" + missing);
        }
        return new Sample(this.field1, this.field2);
      }
    }
  }
}
