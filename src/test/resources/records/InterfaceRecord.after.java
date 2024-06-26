package ma.ju.intellij.builder.psi;

interface InterfaceRecord {
  String name();

  List<String> friends();

  public record InterfaceRecordRecord(String name, List<String> friends) implements InterfaceRecord {
    public InterfaceRecordRecord {
      java.util.Objects.requireNonNull(name, "property :name is required");
      java.util.Objects.requireNonNull(friends, "property :friends is required");
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
      private String name;

      private List<String> friends = List.of();

      private Builder() {
      }

      private Builder(InterfaceRecordRecord record) {
        this.name = record.name;
        this.friends = record.friends;
      }

      /**
       * Sets the {@code name} and returns a reference to this Builder enabling method chaining.
       *
       * @param name the {@code name} to set
       * @return a reference to this Builder
       * @see InterfaceRecordRecord#name
       */
      public Builder setName(String name) {
        this.name = Objects.requireNonNull(name, "Null name");
        return this;
      }

      /**
       * Sets the {@code friends} and returns a reference to this Builder enabling method chaining.
       *
       * @param friends the {@code friends} to set
       * @return a reference to this Builder
       * @see InterfaceRecordRecord#friends
       */
      public Builder setFriends(List<String> friends) {
        this.friends = (friends == null) ? List.of() : friends;
        return this;
      }

      /**
       * Returns a {@code InterfaceRecordRecord} built from the parameters previously set.
       *
       * @return a {@code InterfaceRecordRecord} built with parameters of this {@code InterfaceRecordRecord.Builder}
       * @see InterfaceRecordRecord
       */
      public InterfaceRecord build() {
        if (this.name == null || this.friends == null) {
          StringBuilder missing = new StringBuilder();
          if (this.name == null) {
            missing.append(" name");
          }
          if (this.friends == null) {
            missing.append(" friends");
          }
          throw new IllegalStateException("Missing required properties:" + missing);
        }
        return new InterfaceRecordRecord(this.name, this.friends);
      }
    }
  }
}