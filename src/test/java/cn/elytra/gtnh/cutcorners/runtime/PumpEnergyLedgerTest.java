package cn.elytra.gtnh.cutcorners.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PumpEnergyLedgerTest {
    @Test void sourceAndFlowingActionsRetainFractionalEnergy() {
        for (long cost : new long[]{1, 16, 64, 256, 1024, Long.MAX_VALUE}) {
            for (int speed : new int[]{1, 5, 10, 1000}) {
                PumpEnergyLedger ledger = new PumpEnergyLedger();
                java.math.BigInteger total = java.math.BigInteger.ZERO;
                for (int i = 0; i < speed; i++) {
                    long charge = ledger.charge(cost, speed);
                    assertTrue(charge >= 0 && charge <= PumpEnergyLedger.gate(cost, speed));
                    total = total.add(java.math.BigInteger.valueOf(charge));
                }
                assertEquals(java.math.BigInteger.valueOf(cost), total);
                assertEquals(0, ledger.remainder());
            }
        }
    }
    @Test void restoreAndDisabledPeriodsDoNotLoseCarry() {
        PumpEnergyLedger ledger = new PumpEnergyLedger();
        assertEquals(12, ledger.charge(64, 5));
        PumpEnergyLedger loaded = new PumpEnergyLedger();
        loaded.restore(ledger.remainder(), ledger.divisor());
        assertEquals(64, loaded.charge(64, 1));
        assertEquals(0, loaded.charge(0, 5));
        assertEquals(13, loaded.charge(64, 5));
        loaded.restore(-1, 5); assertEquals(12, loaded.charge(64, 5));
        loaded.restore(5, 5); assertEquals(12, loaded.charge(64, 5));
        loaded.restore(0, 0); assertEquals(12, loaded.charge(64, 5));
    }
    @Test void multiplierChangePreservesFractionalDebt() {
        PumpEnergyLedger ledger = new PumpEnergyLedger();
        assertEquals(12, ledger.charge(64, 5)); // 4/5 EU carried
        assertEquals(7, ledger.charge(64, 10)); // 8/10 + 64/10
        assertEquals(2, ledger.remainder());
        assertEquals(0, ledger.charge(1, 5)); // floor converts 2/10 to 1/5
        assertEquals(2, ledger.remainder());
    }
}
