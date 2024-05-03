package ma.ju.intellij.builder.psi;

import java.util.List;
import java.util.Objects;

public record RegenerateRecord(String strField) {

  public RegenerateRecord {
    stringList =
        List.copyOf(Objects.requireNonNull(stringList, "property :stringList is required"));
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
    private List<String> stringList = List.of();

    private Builder() {}

    private Builder(RegenerateRecord record) {
      this.stringList = record.stringList;
    }

    /**
     * Sets the {@code stringList} and returns a reference to this Builder enabling method chaining.
     *
     * @param stringList the {@code stringList} to set
     * @see RegenerateRecord#stringList
     * @return a reference to this Builder
     */
    public Builder setStringList(List<String> stringList) {
      this.stringList = (stringList == null) ? List.of() : stringList;
      return this;
    }

    public Builder setStrField(String strField) {
      this.strField = strField;
      return this;
    }

    /**
     * Returns a {@code RegenerateRecord} built from the parameters previously set.
     *
     * @see RegenerateRecord
     * @return a {@code RegenerateRecord} built with parameters of this {@code
     *     RegenerateRecord.Builder}
     */
    public RegenerateRecord build() {
      StringBuilder missing = new StringBuilder();
      if (this.stringList == null) {
        missing.append(" stringList");
      }
      if (!missing.isEmpty()) {
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new RegenerateRecord(this.stringList);
    }
  }
}
