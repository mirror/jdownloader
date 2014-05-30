package jd;

import java.net.URL;

import jd.http.Browser;
import jd.http.ext.security.JSPermissionRestricter;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.IO;
import org.jdownloader.scripting.envjs.EnvJS;

public class EnvTest {
    public static Object connection(Object a, Object b, Object c) {

        return null;
    }

    public static String readFromFile(String url) {

        System.out.println("Read File: " + url);
        try {
            return IO.readURLToString(new URL(url));
        } catch (Throwable e) {
            throw new WTFException(e);
        }
    }

    public static void main(String[] args) {

        // Init Javascript Sandbox
        JSPermissionRestricter.init();
        EnvJS env = new EnvJS() {
            public String xhrRequest(String url, String method, String data, String requestHeaders) throws java.io.IOException {
                if (url.endsWith("fb.html")) {
                    return "";
                }
                return super.xhrRequest(url, method, data, requestHeaders);
            };
        };

        try {

            // load env.js library
            env.init();

            Browser br = new Browser();
            // load page
            String link = "";
            // String html = br.getPage(link);

            // evaluate page
            long start = System.currentTimeMillis();
            env.eval("document.location = '" + link + "';");
            ;
            System.out.println(env.getDocument());
            // jd.http.ext.security.JSPermissionRestricter.evaluateTrustedString(cx, scope, "document.location = '" + link + "';", "js", 1,
            // null);
            //

            // // actually this js is NOT trusted.
            // jd.http.ext.security.JSPermissionRestricter.evaluateTrustedString(cx, scope, js, "js", 1, null);
            //
            // Object result = cx.evaluateString(scope, "var f=function(){return document.innerHTML;}; f();", "js", 1, null);
            // System.out.println("Result:\r\n" + result);

            System.out.println("Duration: " + (System.currentTimeMillis() - start));

            // script = org.mozilla.javascript.tools.shell.Main.loadScriptFromSource(cx, "console.log(Recaptcha.th3.exec())", "<stdin>",
            // 1, null);
            // result = org.mozilla.javascript.tools.shell.Main.evaluateScript(script, cx, scope);
            // script = org.mozilla.javascript.tools.shell.Main.loadScriptFromSource(cx, "", "<stdin>", 1, null);
            // result = org.mozilla.javascript.tools.shell.Main.evaluateScript(script, cx, scope);
            // rcBr.getPage("http://www.google.com/recaptcha/api/reload?c=" + this.challenge + "&k=" + site +
            // "&reason=i&type=image&lange=de&th=,8bAyAuXRtl-Wibb4_zT-yMiUdPAAAAAjoAAAAAnYANOWaB2DMBDRXVkfCch7hT43TFM3Xr26pNeEzrB6XjYschGfGhUqLRI5RpsAsFOY6RPG0AEdhmw0WDLzUquOfmkuBSJYD5PvVARPMbDhK1FcztXrpMy0hcjPRGdh1376dJUJ0P_FonFDsd5cfXjJXE7RORlO2mUvesDihcNITxYmqbR2r607dOeZRryJ7oj_-pPcLDqV4OBc3ey2Nnbz5tOYpC-ekM8VUTS8ea5vwzw_uxKytqYrFE6gzPfK6LsEwlqoNcBhvrDDDIAB2EC75toZ4zMJ");
            //
            // script = org.mozilla.javascript.tools.shell.Main.loadScriptFromSource(cx, rcBr.toString(), "<stdin>", 1, null);
            // result = org.mozilla.javascript.tools.shell.Main.evaluateScript(script, cx, scope);

            // script = org.mozilla.javascript.tools.shell.Main.loadScriptFromSource(cx, "console.log(Recaptcha.th3.exec());",
            // "<stdin>", 1, null);
            // result = org.mozilla.javascript.tools.shell.Main.evaluateScript(script, cx, scope);

        } catch (Throwable e) {
            e.printStackTrace();
        }

    }
}
