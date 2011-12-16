package jd;

import java.io.File;
import java.io.IOException;

public class Tester2 {

    enum TEST {
        A,
        B,
        C
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // Browser br = new Browser();
        // br.setLogger(Log.L);
        // br.setVerbose(true);
        // br.setDebug(true);
        // HTTPProxy proxy = new HTTPProxy(HTTPProxy.TYPE.HTTP,
        // "80.246.253.230", 80);
        // // proxy = new HTTPProxy(HTTPProxy.TYPE.SOCKS5, "127.0.0.1", 8080);
        // // proxy.setPass("ZXm9fp2c");
        // // proxy.setUser("underworld");
        // // br.setProxy(proxy);
        // br.getHeaders().put("Accept-Language", "en-US,en;q=0.8");
        // br.getHeaders().put("User-Agent",
        // "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.2 (KHTML, like Gecko) Chrome/15.0.874.121 Safari/535.2");
        // br.getPage("http://doperoms.com/");
        // br.getPage("http://doperoms.com/");
        byte[] a = "é".getBytes("UTF-8");
        byte[] c = "é".getBytes("UTF-16");
        byte[] d = "é".getBytes("UTF-16LE");
        byte[] dd = "é".getBytes("UTF-16BE");
        byte[] f = "é".getBytes("UTF-32");
        byte[] e = "é".getBytes("UTF-32LE");
        byte[] ee = "é".getBytes("UTF-32BE");

        byte[] ee2 = "é".getBytes("x-UTF-32BE-BOM");
        byte[] ee22 = "é".getBytes("x-UTF-32LE-BOM");
        byte[] ee223 = "é".getBytes("x-UTF-16LE-BOM");
        String jj = new String(new byte[] { 0, -17, -65, -67 }, "UTF-8");
        System.out.println(jj);

        // System.out.println(getFreeSpace(new File("/home/daniel/test")));
        // Pattern m = Pattern.compile("(\\d+)", Pattern.CASE_INSENSITIVE);
        // for (int i = 1; i < 9000000; i++) {
        // // br.getRegex(m).getMatch(0);
        // }

        // TestApiInterface inst = (TestApiInterface)
        // Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
        // new Class[] { TestApiInterface.class }, new InvocationHandler() {
        //
        // public Object invoke(Object proxy, Method method, Object[] args)
        // throws Throwable {
        //
        // return JSonStorage.restoreFromString(new
        // Browser().getPage("http://localhost:3128/org.appwork.remoteapi.test.TestApiInterface/"
        // + method.getName() + "?" + args[0] + "&" + args[1]), new
        // TypeRef(method.getGenericReturnType()) {
        // }, null);
        // }
        // });
        // System.out.println(inst.sum(3, (byte) 3));
        // Browser br = new Browser();
        // br.setLogger(Log.L);
        // br.setVerbose(true);
        // br.setDebug(true);
        // System.out.println(br.postPage("http://api.easy-share.com/apikeys",
        // "login=coalado&password=efe98d6a"));

    }

    private static long getFreeSpace(File file) {
        File p = file;
        File lastp = file;
        long ret = 0l;
        while ((ret = lastp.getFreeSpace()) == 0l && (p = p.getParentFile()) != null && p != lastp) {
            lastp = p;
        }
        return ret;
    }
}
