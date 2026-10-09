package net.onelitefeather.cygnus.footprint;

import java.util.random.RandomGenerator;

/**
 * A random source that always rolls the same value, so chance-based code can be tested.
 */
final class FixedRandom implements RandomGenerator {

    double value;

    FixedRandom(double value) {
        this.value = value;
    }

    @Override
    public long nextLong() {
        return 0L;
    }

    @Override
    public double nextDouble() {
        return this.value;
    }

    @Override
    public float nextFloat() {
        return (float) this.value;
    }

    @Override
    public int nextInt(int origin, int bound) {
        return origin;
    }
}
