package io.github.phantran.dev;

import java.math.BigInteger;

public class SimpleExample {

    private Integer index;

    static class Abc {
        public static int temp;

        static void setTemp(int x) {
            temp = x;
        }
    }


    public void xyz() {
        int a = 5;
        assert a == 5;
    }


    public SimpleExample(Integer index) {
        this.index = index;
    }

    public void doStuff(int i, double x) {
         double t = i + x;
    }

    public void doFoo(Integer x, String s, double d, boolean b) {
        if (x != null) {
            double dd = x + d + 5;
        }
    }

    public int simple(int x) {
        if (x < 120) {
            return 1;
        }
        return x;
    }

    public void doNothing() {
        // this is empty!
        int result = 6 | 5;
        int v = result & 10;
        int z = 5;
        for (int i = 5; i < 100; i++)  {
            z++;
        }

        do {
            z--;
        } while (z < 5);
    }

    public void checkCode() {
        Abc.setTemp(10000);
    }


    public void checkFinally() {
        int a = 5;
        try {
            a = 5;
        } catch (Exception e) {
            a = 10;
        } finally {
            a = 0;
        }
    }

    public void abc(int x) {
        if (x == 0) {
            throw new IllegalArgumentException("Infinity is not supported, use ExtendedRational instead");
        }
    }


    @Override
    public String toString() {
        return "SimpleExasdfasdfmple";
    }
}
