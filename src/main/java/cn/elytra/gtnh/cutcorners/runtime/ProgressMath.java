package cn.elytra.gtnh.cutcorners.runtime;

/** No game dependencies: the same math is exercised by unit and runtime tests. */
public final class ProgressMath {
    private ProgressMath() {}

    public static int increment(int progress, int maximum, int multiplier) {
        // Negative progress is GT's power-stutter recovery, not productive work.
        if (progress < 0 || maximum <= progress || multiplier <= 1) return 1;
        return (int) Math.min((long) multiplier, (long) maximum - progress);
    }

    /** Native work has already subtracted one step. Saturate without long overflow. */
    public static long afterExtraWork(long remaining, long step, int multiplier) {
        if (remaining <= 0 || step <= 0 || multiplier <= 1) return remaining;
        long extra = multiplier - 1L;
        if (step > remaining / extra) return 0;
        return remaining - step * extra;
    }

    public static int duration(int original, int multiplier) {
        if (original <= 0 || multiplier <= 1) return original;
        return (int) Math.max(1L, ((long) original + multiplier - 1) / multiplier);
    }

    /** Preserve the miner's extraction-at-duration-1 boundary when its clock changes. */
    public static int miningProgress(int progress, int previous, int next) {
        if (progress < 0 || previous <= 0 || next <= 0 || previous == next) return progress;
        if (next == 1) return 0;
        // Extraction already happened at this boundary; let the next native
        // counter increment finish the cycle without extracting a second block.
        if (progress >= previous - 1) return next - 1;
        // Incomplete work must still reach the extraction boundary once.
        return (int) Math.min(next - 2L, (long) progress * next / previous);
    }
}
