package model.frankfurter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RatesTest {

    @Test
    void shouldStoreEurAndCnyValues() {
        Rates rates = new Rates(1.08, 7.85);

        assertEquals(1.08, rates.eur(), 0.0001);
        assertEquals(7.85, rates.cny(), 0.0001);
    }

    @Test
    void recordsWithSameValuesShouldBeEqual() {
        Rates r1 = new Rates(1.08, 7.85);
        Rates r2 = new Rates(1.08, 7.85);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void recordsWithDifferentValuesShouldNotBeEqual() {
        Rates r1 = new Rates(1.08, 7.85);
        Rates r2 = new Rates(1.09, 7.85);

        assertNotEquals(r1, r2);
    }
}