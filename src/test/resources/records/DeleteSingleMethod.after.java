package ma.ju.intellij.builder.psi;

import java.util.List;

public record DeleteSingleMethod(Boolean active, List<String> data) {

  public DeleteSingleMethod {
    java.util.Objects.requireNonNull(data, "property :data is required");
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

    private List<String> data = List.of();

    private Builder() {
    }

    private Builder(DeleteSingleMethod record) {
      this.data = record.data;
    }

    /**
     * Sets the {@code data} and returns a reference to this Builder enabling method chaining.
     *
     * @param data the {@code data} to set
     * @return a reference to this Builder
     * @see DeleteSingleMethod#data
     */
    public Builder setData(List<String> data) {
      this.data = (data == null) ? List.of() : data;
      return this;
    }

    /**
     * Returns a {@code DeleteSingleMethod} built from the parameters previously set.
     *
     * @return a {@code DeleteSingleMethod} built with parameters of this {@code DeleteSingleMethod.Builder}
     * @see DeleteSingleMethod
     */
    public DeleteSingleMethod build() {
      if (this.data == null) {
        throw new java.lang.IllegalStateException("Missing required property: data");
      }
      return new DeleteSingleMethod(null, this.data);
    }
  }
}