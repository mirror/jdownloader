//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.nutils.debug;

import java.lang.reflect.Method;
import java.util.ArrayList;

import jd.http.Browser;

public abstract class UnitTest {

    private static ArrayList<Class<?>> tests;
    private StringBuffer log;

    private static void init() {
        tests = new ArrayList<Class<?>>();
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
        for (Class<?> test : tests) {
            String name = test.getName();
            if (!name.matches(pattern)) continue;
            try {
                System.out.println("-----------Run Test: " + name + "----------");
                Method f = test.getMethod("newInstance", new Class<?>[] {});
                testInstance = (UnitTest) f.invoke(null, new Object[] {});
                if (testInstance == null) {
                    System.out.println("FAILED: forgot to override public static UnitTest newInstance");
                } else {

                    try {
                        testInstance.run();
                        System.out.println("Successfull");
                        // System.out.println(testInstance.getLog());
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
        return log.toString();
    }

    public abstract void run() throws Exception;

}
