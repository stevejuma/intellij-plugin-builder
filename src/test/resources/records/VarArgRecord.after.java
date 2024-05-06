package ma.ju.intellij.builder.psi;

public record VarArgRecord(String... args) {
  public VarArgRecord {
    java.util.Objects.requireNonNull(args, "property :args is required");
  }

  @java.lang.Override
  public java.lang.String toString() {
    return getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this));
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String[] args;

    private Builder() {}

    private Builder(VarArgRecord record) {
      this.args = record.args;
    }

    public Builder setArgs(String[] args) {
      this.args = Objects.requireNonNull(args, "Null args");
      return this;
    }

    public VarArgRecord build() {
        if (this.args == null) {
            throw new IllegalStateException("Missing required property: args");
        }
        return new VarArgRecord(this.args);
    }
  }
}
