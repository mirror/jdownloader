package jd;

import java.io.File;

import jd.utils.JDUtilities;

public class Tester {

    public static void main(String[] args) throws Exception {
        System.out.println(JDUtilities.getResourceFile("jd").getParent());

        for (File f : File.listRoots())
            System.out.println(f.getAbsolutePath());
    }

}