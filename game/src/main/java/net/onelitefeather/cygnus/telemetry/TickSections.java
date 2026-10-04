package net.onelitefeather.cygnus.telemetry;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongSupplier;

/**
 * Measures how much of a server tick named pieces of work took, so a slow tick can say who was in it.
 * <p>
 * A piece of work is registered once, at construction time, through {@link #wrap(String, Runnable)}.
 * Every run adds its duration to a counter; {@link SlowTickTracer} reads and clears the counters when
 * the tick ends. The hot path is two {@code nanoTime} reads and one {@link LongAdder} add - no
 * allocation, no lock - and when tracing is not wanted {@link #NONE} hands the action back untouched,
 * so the path costs nothing at all.
 * </p>
 * <p>
 * The durations are <em>totals per tick</em>, not positions: a service that ran twice in one tick is
 * reported once, with both runs added up. The scheduler runs on the tick thread, so the counters are
 * written by one thread and read by the same one; the adders are there for the odd call from
 * elsewhere, not for contention.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class TickSections {

    /**
     * Sections that measure nothing: {@link #wrap(String, Runnable)} returns the action itself.
     */
    public static final TickSections NONE = new TickSections(null);

    private final LongSupplier nanoTime;
    private volatile Section[] sections = new Section[0];

    private TickSections(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    /**
     * Creates sections that measure.
     *
     * @param nanoTime the clock durations are taken from, {@code System::nanoTime} in production
     * @return the sections
     */
    public static TickSections measuring(LongSupplier nanoTime) {
        return new TickSections(nanoTime);
    }

    /**
     * Returns an action that runs the given one and counts the time it took under the given name.
     * Wrapping twice under the same name adds both to the same section.
     *
     * @param name   the section name reported with a slow tick
     * @param action the action to measure
     * @return the measuring action, or the action itself when this does not measure
     */
    public synchronized Runnable wrap(String name, Runnable action) {
        if (this.nanoTime == null) {
            return action;
        }
        Section section = sectionNamed(name);
        LongSupplier clock = this.nanoTime;
        return () -> {
            long start = clock.getAsLong();
            try {
                action.run();
            } finally {
                section.nanos.add(clock.getAsLong() - start);
            }
        };
    }

    /**
     * Returns how many sections are registered.
     *
     * @return the section count
     */
    int size() {
        return this.sections.length;
    }

    /**
     * Returns the name of a section.
     *
     * @param index the section index, below {@link #size()}
     * @return the name
     */
    String name(int index) {
        return this.sections[index].name;
    }

    /**
     * Returns the time a section ran since the last call and clears it.
     *
     * @param index the section index, below {@link #size()}
     * @return the nanoseconds
     */
    long takeNanos(int index) {
        return this.sections[index].nanos.sumThenReset();
    }

    private Section sectionNamed(String name) {
        for (Section existing : this.sections) {
            if (existing.name.equals(name)) {
                return existing;
            }
        }
        Section[] grown = java.util.Arrays.copyOf(this.sections, this.sections.length + 1);
        Section created = new Section(name);
        grown[grown.length - 1] = created;
        // Published whole, so the tick thread iterating by index never sees a half-grown array.
        this.sections = grown;
        return created;
    }

    private static final class Section {

        private final String name;
        private final LongAdder nanos = new LongAdder();

        private Section(String name) {
            this.name = name;
        }
    }
}
