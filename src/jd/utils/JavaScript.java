package jd.utils;

import java.util.ArrayList;

import jd.http.Browser;
import jd.parser.Regex;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class JavaScript {

    public static String evalPage(Browser br) {
        String clazz = JDUtilities.getLocalFile(JDUtilities.getResourceFile("jd/dummy.js"));
        clazz = clazz.replaceAll("%%%HOST%%%", br.getHost());
        clazz = clazz.replaceAll("%%%URL%%%", br.getHttpConnection().getURL() + "");
        clazz = clazz.replaceAll("%%%URLPATH%%%", br.getHttpConnection().getURL().getPath() + "");
        clazz = clazz.replaceAll("%%%PORT%%%", br.getHttpConnection().getURL().getPort() + "");
        clazz = clazz.replaceAll("%%%QUERY%%%", br.getHttpConnection().getURL().getQuery() + "");
        clazz = clazz.replaceAll("%%%PROTOCOL%%%", br.getHttpConnection().getURL().getProtocol() + "");
        clazz = clazz.replaceAll("%%%REFERRER%%%", br.getHttpConnection().getRequestProperty("Referrer") + "");

        String page = br.toString();

        Context cx = Context.enter();

        Scriptable scope = cx.initStandardObjects();
        cx.evaluateString(scope, clazz, "<cmd>", 1, null);
        page = eval(page, cx, scope, br);

        Context.exit();
        return page;
    }

    private static String eval(String page, Context cx, Scriptable scope, Browser br) {
        String[] codes = new Regex(page, "<script.*?>(.*?)</script>").getColumn(-1);

        for (String code : codes) {
            String lib = new Regex(code, "<script.*?src[ ]*=[ ]*[\"\\'](.+?)[\"\\'].*?/>").getMatch(0);
            if (lib == null) {
                lib = new Regex(code, "<script.*?src[ ]*=[ ]*[\"\\'](.+?)[\"\\'].*?>.*?</script").getMatch(0);
            }
            String rep = "";
            try {
                if (lib != null && !isForbidden(lib)) {
                    br = br.cloneBrowser();
                    lib = br.getPage(lib);
                    if (br.getHttpConnection().isOK()) {
                        Object result = cx.evaluateString(scope, lib + "\r\nfunction f(){\r\nreturn document.getMyOutput();} f()", "<cmd>", 1, null);
                        String res = Context.toString(result);
                        res = eval(res, cx, scope, br);
                        rep += res;
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block

                System.out.println(e.getMessage() + "\r\n" + lib);
            }
            String content = new Regex(code, ">(.+?)</script").getMatch(0);
            if (content != null) {
                try {
                    String fun = content + "\r\nfunction f(){\r\nreturn document.getMyOutput();} f()";
                    Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
                    String res = Context.toString(result);
                    res = eval(res, cx, scope, br);
                    rep += res;

                } catch (Exception e) {
                    // TODO Auto-generated catch block

                    System.out.println(e.getMessage() + "\r\n" + content);
                }
            }
            page = page.replace(code, rep);
        }
        return page;
    }

    private static boolean isForbidden(String lib) {
        ArrayList<String> pattern = new ArrayList<String>();
        pattern.add("http://.*?mirando.de/.*");
        pattern.add("http://ads\\..*");
        pattern.add(".*adition\\.com.*");
        pattern.add(".*google.*");
        for (String p : pattern) {
            if (Regex.matches(lib, p)) {
                System.out.println("Filtered JS library: " + lib);
                return true;
            }
        }
        System.out.println("Loaded JS library: " + lib);
        return false;
    }

}
