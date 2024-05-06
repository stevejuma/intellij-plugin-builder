package ma.ju.intellij.builder.psi;

import org.jetbrains.annotations.Nullable;

public class PoJo {
    private final @Nullable String str;
    private final boolean bool;

    private PoJo(Builder builder) {
        this.str = builder.str;
        this.bool = builder.bool;
    }

    @Nullable
    public String getStr() {
        return this.str;
    }

    public boolean isBool() {
        return this.bool;
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
        private String str;

        private boolean bool;

        private Builder() {
        }

        private Builder(PoJo record) {
            this.str = record.str;
            this.bool = record.bool;
        }

        public Builder setStr(@Nullable String str) {
            this.str = str;
            return this;
        }

        public Builder setBool(boolean bool) {
            this.bool = bool;
            return this;
        }

        public PoJo build() {
            return new PoJo(this);
        }
    }
}