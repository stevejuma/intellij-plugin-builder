package ma.ju.intellij.builder.psi;

import java.util.List;
import java.util.Objects;

public record DeleteSingleMethod(Boolean active, List<String> data) {
  public DeleteSingleMethod {
    Objects.requireNonNull(active, "property :active is required");
    data = List.copyOf(Objects.requireNonNull(data, "property :data is required"));
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
    private Boolean active;

    private List<String> data = List.of();

    private Builder() {
    }

    private Builder(DeleteSingleMethod record) {
      this.active = record.active;
      this.data = record.data;
    }

    /**
     * Sets the {@code active} and returns a reference to this Builder enabling method chaining.
     * @param active the {@code active} to set
     * @see DeleteSingleMethod#active
     * @return a reference to this Builder
     */
    public Builder setActive(Boolean active) {
      this.active = active;
      return this;
    }

    /**
     * Sets the {@code data} and returns a reference to this Builder enabling method chaining.
     * @param data the {@code data} to set
     * @see DeleteSingleMethod#data
     * @return a reference to this Builder
     */
    public Builder setData(List<String> data) {
      this.data = (data==null) ? List.of():data;
      return this;
    }

    /**
     * Returns a {@code DeleteSingleMethod} built from the parameters previously set.
     * @see DeleteSingleMethod
     * @return a {@code DeleteSingleMethod} built with parameters of this {@code DeleteSingleMethod.Builder}
     */
    public DeleteSingleMethod build() {
      StringBuilder missing = new StringBuilder();
      if (this.active==null) {
        missing.append(" active");
      }
      if (this.data==null) {
        missing.append(" data");
      }
      if (!missing.isEmpty()) {
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new DeleteSingleMethod(this.active, this.data);
    }
  }
}
