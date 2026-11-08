package cn.elytra.gtnh.cutcorners.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgressMathTest {
    @Test void cyclesRoundUpAndNeverRepeatScale() {
        for (int maximum : new int[]{1,2,9,10,11,19,20,99,100,198,300,1200,2400,512000}) {
            for (int speed : new int[]{1,2,3,10,1000}) {
                for (int batch=0; batch<3; batch++) {
                    int progress=0,ticks=0;
                    while(progress<maximum) { progress+=ProgressMath.increment(progress,maximum,speed); ticks++; }
                    assertEquals(ProgressMath.duration(maximum,speed),ticks);
                    assertEquals(maximum,progress);
                }
            }
        }
    }
    @Test void powerStutterRemainsARealTickCooldown() {
        int progress=-100;
        for(int tick=0;tick<100;tick++) progress+=ProgressMath.increment(progress,200,10);
        assertEquals(0,progress);
        assertEquals(10,ProgressMath.increment(progress,200,10));
    }
    @Test void computationSaturatesWithoutOverflowAndPreservesDisabledWork() {
        assertEquals(900,ProgressMath.afterExtraWork(990,10,10));
        assertEquals(0,ProgressMath.afterExtraWork(5,10,10));
        assertEquals(0,ProgressMath.afterExtraWork(Long.MAX_VALUE,Long.MAX_VALUE,1000));
        assertEquals(100,ProgressMath.afterExtraWork(100,10,1));
        assertEquals(100,ProgressMath.afterExtraWork(100,0,10));
    }
    @Test void disabledAndIdleKeepTheOriginalStep() {
        assertEquals(1,ProgressMath.increment(10,100,1));
        assertEquals(1,ProgressMath.increment(0,0,10));
        assertEquals(1,ProgressMath.increment(-1,0,1000));
        assertEquals(0,ProgressMath.duration(0,10));
    }
    @Test void nearIntegerLimitCannotWrapAndShortRecipesTakeOneTick() {
        assertEquals(4,ProgressMath.increment(Integer.MAX_VALUE-4,Integer.MAX_VALUE,1000));
        assertEquals(214748365,ProgressMath.duration(Integer.MAX_VALUE,10));
        assertEquals(1,ProgressMath.duration(1,1000));
    }
    @Test void miningClockChangesKeepCompletedAndPendingExtractionSeparate() {
        assertEquals(16, ProgressMath.miningProgress(80, 160, 32));
        assertEquals(30, ProgressMath.miningProgress(158, 160, 32));
        assertEquals(31, ProgressMath.miningProgress(159, 160, 32));
        assertEquals(159, ProgressMath.miningProgress(31, 32, 160));
        assertEquals(-100, ProgressMath.miningProgress(-100, 160, 32));
        assertEquals(0, ProgressMath.miningProgress(158, 160, 1));
        assertEquals(536_870_911, ProgressMath.miningProgress(1, 4, Integer.MAX_VALUE));
    }
}
