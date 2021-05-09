package io.github.phantran.dev;

import java.util.ArrayList;
import java.util.List;

public class Feature {

    public static String s = null;
    public Integer publicValue;
    public Integer value;
    public int hehe = 5;
    private List<String> logger;
    public static int staticInt = 5;
    public int valueInt;
    public static double doubleZ = 5.9;

    public String getStatic() {
        return Feature.s;
    }

    public int getVal() {
        return value;
    }

    public Feature(int intValue, int value2) {
        this.value = intValue + value2;
    }

    public static void foo2(double d) {
        Feature a = new Feature(5,5);
        Feature.s = "asd";
        int sds = a.valueInt;
        Feature.s = "abc";
        String temp = Feature.s;
        int temp1 = Feature.staticInt;
        double x = d;
        int y = 123123232;
        double z = x + y;
    }

    public String doStuffFeature(String s, Integer i, Boolean b, Double d, Float f, Short r) {
        if (value != null && value > 0) {
            return "dasdings" + i + b + d + f + r;
        } else if (publicValue != null && publicValue < 625) {
            return "blasdub";
        } else {
            return null;
        }
    }

    public Integer foo(Integer counter) {
        Integer c = 0;
        while (counter != null && counter <= 41) {
            c++;
            counter++;
        }

        int j = 0;
        int z = 10;
        do {
            j++;
            z++;
        } while(j < 1);
        return c;
    }

    public void doNothing() {
        // this is empty!
    }

    public void caller(int i, float x, boolean b) {
        if (publicValue != null && i < 42 && i < publicValue) {
            call2(i);
        }

        if (x > 22 && b) {
            List<String> list = new ArrayList<>();
            for (int z = 0; z < x; z++) {
                list.add("n" + z);
            }
        }
    }


    private void call2(double y) {
        List<Boolean> bools = new ArrayList<>();
        for (int j = (int) y / 2; j < 80; j++) {
            bools.add(y == j);
            if (bools.size() > 40) {
                break;
            }
        }
        if (s != null) {
            bools.add(false);
        }
    }

    public int simpleIf(int x) {
        if (x < 2) {
            return 0;
        }
        return 5;
    }

    public void seeWhatHappens(int x) {
        if (logger == null) {
            logger = new ArrayList<>();
        }

        if (x > -1023) {
            logger.add("Not quite there yet");
        }

        logger.add("You did it");
    }
}
