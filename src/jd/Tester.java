package jd;

import jd.nutils.Executer;

public class Tester {

    public static void main(String ss[]) throws Exception {
        Executer exec = new Executer("firefox");
        exec.addParameters(new String[] { "heise.de" });
        exec.start();
    }

}