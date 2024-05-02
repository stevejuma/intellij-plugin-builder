package ma.ju.intellij.builder.psi;

import java.util.List;

public record CustomRecord(List<String> data) {
  public static class Builder {
    private List<String> data;

    public Builder setData(List<String> data) {
      this.data = data == null ? List.of() : data;
      if (data.isEmpty()) {
        throw new IllegalStateException("property :data cannot be empty");
      }
      return this;
    }

    private void validate() {
      if (data.isEmpty()) {
        throw new IllegalStateException("property :data cannot be empty");
      }
    }
  }
}
