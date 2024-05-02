package ma.ju.intellij.builder.psi;

import java.util.List;

public record DeleteCustomRecord(List<String> data) {
  public DeleteCustomRecord {
    java.util.Objects.requireNonNull(data, "property :data is required");
  }

  @java.lang.Override
  public java.lang.String toString() {
    return getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this));
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private List<String> data = List.of();

    private Builder() {}

    private Builder(DeleteCustomRecord record) {
      this.data = record.data;
    }

    /**
     * Sets the {@code data} and returns a reference to this Builder enabling method chaining.
     *
     * @param data the {@code data} to set
     * @return a reference to this Builder
     * @see DeleteCustomRecord#data
     */
    public Builder setData(List<String> data) {
      this.data = data == null ? List.of() : data;
      if (data.isEmpty()) {
        throw new IllegalStateException("property :data cannot be empty");
      }
      return this;
    }

    /**
     * Returns a {@code DeleteCustomRecord} built from the parameters previously set.
     *
     * @return a {@code DeleteCustomRecord} built with parameters of this {@code DeleteCustomRecord.Builder}
     * @see DeleteCustomRecord
     */
    public DeleteCustomRecord build() {
      java.lang.StringBuilder missing = new java.lang.StringBuilder();
      if (this.data == null) {
        missing.append(" data");
      }
      if (!missing.isEmpty()) {
        throw new java.lang.IllegalStateException("Missing required properties:" + missing);
      }
      this.validate();
      return new DeleteCustomRecord(this.data);
    }

    void validate() {}
  }
}
