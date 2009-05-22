package jd;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;

public class Tester {

    private static final int MASS = 50000;

    /**
     * @param args
     */
    public static void main(String[] args) {

        test(new Vector<Object>());

        test(new ArrayList<Object>());
        test(new LinkedList<Object>());
    }

    private static void test(AbstractList<Object> col) {
        long t = System.currentTimeMillis();
        long free = Runtime.getRuntime().freeMemory();
        Object res;
        System.out.println(col.getClass() + "\r\n\r\n");
        for (int i = 0; i < MASS; i++) {
            col.add(new Long(i));
        }
        System.out.println(MASS + " adds: " + (System.currentTimeMillis() - t));
        t = System.currentTimeMillis();
        for (int i = 0; i < MASS; i++) {
            res = col.get((int) (Math.random() * MASS));
        }
        System.out.println(MASS + " random gets: " + (System.currentTimeMillis() - t));
        t = System.currentTimeMillis();
        for (int i = 0; i < MASS; i++) {
            col.remove((int) (Math.random() * col.size()));
        }
        System.out.println(MASS + " random removes: " + (System.currentTimeMillis() - t));
        // t = System.currentTimeMillis();

        // System.out.println(col.getClass() + ": " +
        // (System.currentTimeMillis() - t) + " ms " + (free -
        // Runtime.getRuntime().freeMemory()) + " bytes");
    }
}
