package de.hhu.stups.plues.prob;

import com.google.common.base.Objects;

public class Alternative {
    private final String day;
    private final String slot;

    public Alternative(final String day, final String slot) {
        this.day = day;
        this.slot = slot;
    }

    public String getSlot() {
        return slot;
    }

    public String getDay() {
        return day;
    }

    public String toString() {
        return "Alternative(" + this.day + ", " + this.slot + ")";
    }

    @Override
    public boolean equals(final Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        final Alternative that = (Alternative) o;
        return Objects.equal(day, that.day) &&
                Objects.equal(slot, that.slot);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(day, slot);
    }
}
