package jd.nutils.debug;

import java.lang.reflect.Method;
import java.util.ArrayList;

import jd.http.Browser;

public abstract class UnitTest {

    private static ArrayList<Class> tests;
    private StringBuffer log;

    private static void init() {
        tests = new ArrayList<Class>();
        tests.add(Browser.Test.class);

    }

    public static UnitTest newInstance() {
        return null;
    }

    public static void main(String args[]) throws Exception {
        UnitTest.run(".*");

    }

    private static void run(String pattern) {
        init();
        UnitTest testInstance;
        for (Class test : tests) {
            String name = test.getName();
            if (!name.matches(pattern)) continue;
            try {
                System.out.println("-----------Run Test: " + name + "----------");
                Method f = test.getMethod("newInstance", new Class[] {});
                testInstance = (UnitTest) f.invoke(null, new Object[] {});
                if (testInstance == null) {
                    System.out.println("FAILED: forgot to override public static UnitTest newInstance");
                } else {

                    try {
                        testInstance.run();
                        System.out.println("Successfull");
                        //System.out.println(testInstance.getLog());
                    } catch (Exception e) {
                        System.out.println("FAILED");
                       // System.err.println(testInstance.getLog());
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void log(String msg) {
        if (this.log == null) log = new StringBuffer();
        // System.err.println(ee.getStackTrace()[1].getClassName() + "." +
        // ee.getStackTrace()[1].getMethodName() + "[" +
        // ee.getStackTrace()[1].getLineNumber() + "] " + msg);
     System.out.println(new Exception().getStackTrace()[1].toString() + " : " + msg);
        log.append(new Exception().getStackTrace()[1].toString() + " : " + msg + "\r\n");
    }

    public String getLog() {
        // TODO Auto-generated method stub
        return log.toString();
    }

    public abstract void run() throws Exception;
}
