package io.github.phantran.dev;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SimpleExampleTest {

    @Test
    public void test_hihi() {
        SimpleExample a = new SimpleExample(1);
        a.doStuff(1, 2.0);
        String abc = a.toString();
        assertEquals(1, 1);
    }

    @Test
    public void test_hihi1() {
        Hihi a = new Hihi(20);
        a.addRiders(3);
        a.getTopFloor();
        assertEquals(1, 1);
    }

    @Test
    public void test_hihi2() {
        SimpleExample a = new SimpleExample(20);
        a.doNothing();
        assertEquals(1, 1);
    }
}
