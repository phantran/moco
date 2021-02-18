package io.moco;

import io.moco.engine.DummyForTesting;
import io.moco.engine.tracker.BlockTracker;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestTest2 {

    @Test
    public void test_hihi() {
        BlockTracker t = new BlockTracker();
        DummyForTesting t1 = new DummyForTesting();
        t1.dummy();
        int a = 1;
        int b = 2;
        assertEquals(b, a + 1);
    }

    @Test
    public void test_hihi1() {
        DummyForTesting t1 = new DummyForTesting();
        int a = 1;
        int b = 2;
        assertEquals(b, a + 1);
    }
}
