package net.onelitefeather.cygnus.stamina;

import java.util.random.RandomGenerator;

/**
 * Hands out fixed doubles in order and starts over once they run out, so a test decides every roll.
 */
final class FixedRandom implements RandomGenerator {

    private final double[] doubles;
    private int next;

    FixedRandom(double... doubles) {
        this.doubles = doubles;
    }

    @Override
    public double nextDouble() {
        double value = this.doubles[this.next];
        this.next = (this.next + 1) % this.doubles.length;
        return value;
    }

    @Override
    public long nextLong() {
        throw new UnsupportedOperationException();
    }
}
