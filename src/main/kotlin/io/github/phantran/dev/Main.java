package io.github.phantran.dev;


public class Main {
    public static void main(String[] args) {
    }

    public void haha() {
        int z = 0;
        int c = 5;
        if (true) {
            int b = 2;
            b = b + z ;
            z = b;
        }

        Feature a = new Feature(5,4);
        Feature.s = "asd";
        String asc = a.getStatic();
        int asd = a.hehe + (++Feature.staticInt);
        int sds = a.value;
        String xyz = Feature.s + "a";

        int f = (z + sds) + asd;
        c = c - 21;
        double gc = 0.45326e+04;
    }
}
