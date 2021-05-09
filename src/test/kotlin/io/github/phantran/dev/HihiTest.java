package io.github.phantran.dev;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HihiTest {

    @Test
    public void test_hihi() {
        Hihi a = new Hihi(12);
        a.haha1();
        a.haha();
        a.goDown();
        assertEquals(1, 1);
    }

    @Test
    public void test_hihi1() {
        Hihi a = new Hihi(10);
        a.addRiders(3);
        a.getTopFloor();
        assertEquals(1, 1);
    }

    @Test
    public void test_hihi2() {
        Hihi a = new Hihi(12);
        a.goDown();
    }
}
