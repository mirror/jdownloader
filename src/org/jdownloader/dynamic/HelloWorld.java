package org.jdownloader.dynamic;

import java.util.Arrays;

public class HelloWorld {

    public static void runStatic() {
        System.out.println("First Static!");
    }

    public static void runMain(String[] args) {
        System.out.println("First Main! " + Arrays.toString(args));
    }

}
