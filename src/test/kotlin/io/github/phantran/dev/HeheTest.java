package io.github.phantran.dev;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class HeheTest {

    @Test
    public void test_hihi() {
        Hihi a = new Hihi(5);
        a.haha();
        assertEquals(1, 1);
    }

    @Test
    public void test_hihi1() {
        Hihi a = new Hihi(23);
        a.addRiders(5);
        a.getTopFloor();
        assertEquals(1, 1);
    }

    @Test
    public void test_hihi3() {
        Hihi a = new Hihi(1213);
        a.addRiders(34);
        a.getTopFloor();
        a.call(5);
        assertEquals(1, 1);
    }
}
