//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.utils;

import java.util.ArrayList;

import jd.http.Browser;
import jd.nutils.io.JDIO;
import jd.parser.Regex;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Scriptable;

public class JavaScript {

    public static String evalPage(Browser br) {
        String clazz = JDIO.getLocalFile(JDUtilities.getResourceFile("jd/dummy.js"));
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

                } catch (EcmaError e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace(System.err);
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
