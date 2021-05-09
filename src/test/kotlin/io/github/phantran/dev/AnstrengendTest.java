package io.github.phantran.dev;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AnstrengendTest {

    @Test
    public void whenThis_thenThat() {
        assertTrue(true);
    }

    private void assertTrue(boolean b) {
    }

    @Test
    public void whenSomething_thenSomething() {
        assertTrue(true);
    }

    @Test
    public void whenSomethingElse_thenSomethingElse() {
        assertTrue(false);
    }

    @Test
    public void whenSomethingElse_thenSomeasdthingElse() {
        io.github.phantran.dev.Test temp = new io.github.phantran.dev.Test();
        long res = temp.foo1(5.0);
        assertEquals(4L, res);
        assertTrue(true);
    }
}