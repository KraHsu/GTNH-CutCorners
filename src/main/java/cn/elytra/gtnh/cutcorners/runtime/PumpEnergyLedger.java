package cn.elytra.gtnh.cutcorners.runtime;

/** Per-pump fractional EU, charged only by the native successful-fluid path. */
public final class PumpEnergyLedger {
    private int remainder;
    private int divisor = 1;

    public long charge(long original, int speed) {
        if (original <= 0 || speed <= 1) return original;
        speed = Math.min(1000, speed);
        if (speed != divisor) {
            remainder = (int) ((long) remainder * speed / divisor);
            divisor = speed;
        }
        long fractional = original % speed + remainder;
        remainder = (int) (fractional % speed);
        return original / speed + fractional / speed;
    }

    public int remainder() { return remainder; }
    public int divisor() { return divisor; }
    public void restore(int savedRemainder, int savedDivisor) {
        boolean valid = savedDivisor >= 1 && savedDivisor <= 1000
            && savedRemainder >= 0 && savedRemainder < savedDivisor;
        remainder = valid ? savedRemainder : 0;
        divisor = valid ? savedDivisor : 1;
    }

    /** Conservative preflight: never start a native fluid action on zero EU. */
    public static long gate(long original, int speed) {
        if (original <= 0 || speed <= 1) return original;
        return original / speed + (original % speed == 0 ? 0 : 1);
    }
}
