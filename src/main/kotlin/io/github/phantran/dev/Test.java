package io.github.phantran.dev;

public class Test {

    public void abc(int a, int b) {
        int k = 100;
        int l = 900;
        int s = k + l;
        s = k + 5;
        s = s + 5;
        int f = -b - a + k-- - (+l);
    }

    public Integer foo(Integer counter) {
        Integer c = 0;
        try {
            while (counter != null && counter <= 41) {
                c++;
                counter++;
            }
            return c;
        } catch (Exception ignored) {
            return null;
        } finally {
            c = 5;
            int d = 7;
        }
    }

    public Long foo1(Double counter) {
        double doubleZ = Feature.doubleZ + 5;
        Double c = 0.0;
        if (c == null) {
            c = 1.0;
        }
        Double k = 23.2;
        Float t = 1f;
        Float tryre = 45f;
        Byte m = 2;
        Short n = 3;
        if (n != 0) {
            n--;
        }
        Long z = 12L;
        if (z == 0) {
            z = z / 2;
        }
        Long w = 13L;
        if (w != null) {
            w++;
        }
        c = c + k;
        t = t - tryre;
        m = m++;
        n = n--;
        z = z / 2;
        z = z * 3;
        --z;
        z++;
        z = z % w;
        int a = 5;
        int abc = 6;
        a = a | abc;
        a = a & abc;
        return z;
    }
}
