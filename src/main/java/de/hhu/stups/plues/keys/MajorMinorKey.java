package de.hhu.stups.plues.keys;

import java.util.Objects;

public final class MajorMinorKey {
  private final String major;
  private final String minor;

  public MajorMinorKey(final String major, final String minor) {
    this.major = major;
    this.minor = minor;
  }

  @Override
  public final boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    MajorMinorKey that = (MajorMinorKey) obj;
    return Objects.equals(major, that.major)
        && Objects.equals(minor, that.minor);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(major, minor);
  }

  public final String getMajor() {
    return this.major;
  }

  public final String getMinor() {
    return this.minor;
  }

  public final Boolean hasMinor() {
    return this.minor != null;
  }
}
