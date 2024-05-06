package ma.ju.intellij.builder.psi;

import java.util.List;

public record RegenerateRecord(String strField) {

  public RegenerateRecord {
    java.util.Objects.requireNonNull(strField, "property :strField is required");
  }

  @Override
  public String toString() {
    return getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this));
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String strField;
    private List<String> stringList = List.of();

    private Builder() {}

    private Builder(RegenerateRecord record) {
      this.strField = record.strField;
    }

    /**
     * Sets the {@code stringList} and returns a reference to this Builder enabling method chaining.
     *
     * @param stringList the {@code stringList} to set
     * @return a reference to this Builder
     * @see RegenerateRecord#stringList
     */
    public Builder setStringList(List<String> stringList) {
      this.stringList = (stringList == null) ? List.of() : stringList;
      return this;
    }

    /**
     * Sets the {@code strField} and returns a reference to this Builder enabling method chaining.
     *
     * @param strField the {@code strField} to set
     * @return a reference to this Builder
     * @see RegenerateRecord#strField
     */
    public Builder setStrField(String strField) {
      this.strField = java.util.Objects.requireNonNull(strField, "Null strField");
      return this;
    }

    /**
     * Returns a {@code RegenerateRecord} built from the parameters previously set.
     *
     * @return a {@code RegenerateRecord} built with parameters of this {@codeRegenerateRecord.Builder}
     * @see RegenerateRecord
     */
    public RegenerateRecord build() {
      if (this.strField == null) {
        throw new java.lang.IllegalStateException("Missing required property: strField");
      }
      return new RegenerateRecord(this.strField);
    }
  }
}
