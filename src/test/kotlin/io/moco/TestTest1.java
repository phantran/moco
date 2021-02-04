package io.moco;

import io.moco.engine.Worker;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TestTest1 {

    @Test
    public void test_hihi() {
        Worker t = new Worker();
        t.abc();
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
