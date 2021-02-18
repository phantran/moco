package io.moco;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TestTest1 {

    @Test
    public void test_hihi() {
        DummyForTesting t = new DummyForTesting();
        t.dummy();
        int a = 1;
        int b = 2;
        assertEquals(b, a + 1);
    }

    @Test
    public void test_hihi1() {
        int a = 1;
        int b = 2;
        assertEquals(b, a + 1);
    }

    @Test
    public void test_hihi3() {
        int a = 1;
        int b = 2;
        assertEquals(b, a + 1);
    }
}
