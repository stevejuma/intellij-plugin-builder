package ma.ju.intellij.builder.psi;

import java.util.List;

public record CustomRecord(List<String> data) {
  public CustomRecord {
    java.util.Objects.requireNonNull(data, "property :data is required");
    if (data.isEmpty()) {
      throw new IllegalStateException("property :data cannot be empty");
    }
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

  public static class Builder {

    private List<String> data = List.of();

    private Builder() {}

    private Builder(CustomRecord record) {
      this.data = record.data;
    }

    /**
     * Sets the {@code data} and returns a reference to this Builder enabling method chaining.
     *
     * @param data the {@code data} to set
     * @return a reference to this Builder
     * @see CustomRecord#data
     */
    public Builder setData(List<String> data) {
      this.data = data == null ? List.of() : data;
      if (data.isEmpty()) {
        throw new IllegalStateException("property :data cannot be empty");
      }
      return this;
    }

    /**
     * Returns a {@code CustomRecord} built from the parameters previously set.
     *
     * @return a {@code CustomRecord} built with parameters of this {@code CustomRecord.Builder}
     * @see CustomRecord
     */
    public CustomRecord build() {
      if (this.data == null) {
        throw new java.lang.IllegalStateException("Missing required property: data");
      }
      this.validate();
      return new CustomRecord(this.data);
    }

    private void validate() {
      if (data.isEmpty()) {
        throw new IllegalStateException("property :data cannot be empty");
      }
    }
  }
}
