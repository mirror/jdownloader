package jd;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import jd.http.ext.security.JSPermissionRestricter;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.IO;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.tools.shell.Global;

public class EnvTest {
    public static String readFromFile(String url) {

        System.out.println("Read File: " + url);
        try {
            return IO.readURLToString(new URL(url));
        } catch (Throwable e) {
            throw new WTFException(e);
        }
    }

    public static void main(String[] args) {

        // String cb = rcBr.getRegex("(cb\\=function\\(a\\)\\{\\;.*?(var H\\=function\\(\\)\\{\\}\\;").getMatch(0);
        JSPermissionRestricter.init();
        Context cx = Context.enter();
        Global scope = new Global(cx);
        cx.setOptimizationLevel(-1);
        cx.setLanguageVersion(Context.VERSION_1_5);
        try {
            jd.http.ext.security.JSPermissionRestricter.evaluateTrustedString(cx, scope, IO.readFileToString(new File("\\ressourcen\\libs\\js\\env.rhino.js")), "js", 1, null);

            jd.http.ext.security.JSPermissionRestricter.evaluateTrustedString(cx, scope, "Envjs.scriptTypes = {\"text/javascript\"   :true,   \"text/envjs\"        :true};", "js", 1, null);

            // jd.http.ext.security.JSPermissionRestricter.evaluateTrustedString(cx, scope,
            // "Envjs.log = function(message){java.lang.System.out.println(message);};", "js", 1, null);
            // jd.http.ext.security.JSPermissionRestricter.evaluateTrustedString(cx, scope, "Packages.jd.EnvTest.readFromFile(null);", "js",
            // 1, null);

            // jd.http.ext.security.JSPermissionRestricter.evaluateTrustedString(cx, scope,
            // "Envjs.eval = function(context, source, name){java.lang.System.out.println(context+\"_\"+source+\"-\"+name);};", "js", 1,
            // null);
            // jd.http.ext.security.JSPermissionRestricter.evaluateTrustedString(cx, scope, "console.log('Blablabla');", "js", 1, null);
            String html = IO.readFileToString(new File("rapidgator.html"));
            // html = HTMLEntities.htmlentities(html);
            //
            // html = HTMLEntities.htmlAmpersand(html);
            // html = HTMLEntities.htmlAngleBrackets(html);
            // html = HTMLEntities.htmlDoubleQuotes(html);
            // html = HTMLEntities.htmlQuotes(html);
            // html = HTMLEntities.htmlSingleQuotes(html);
            long start = System.currentTimeMillis();
            // String js = "document.write(\"" + html.replace("\"", "\\\"") + "\"); document.open();";

            String js = "document.async=true; document.baseURI = 'http://jdownloader.org/';HTMLParser.parseDocument(\"" + html.replace("\"", "\\\"") + "\", document);Envjs.wait();";

            System.out.println(js);
            jd.http.ext.security.JSPermissionRestricter.evaluateTrustedString(cx, scope, js, "js", 1, null);
            // cx.evaluateString(scope, js, "js", 1, null);
            // cx.evaluateString(scope, "document.write('bla');", "js", 1, null);
            // cx.evaluateString(scope, "document.documentElement.innerHTML=\"" + html.replace("\"", "\\\"") + "\"", "js", 1, null);

            Object result = cx.evaluateString(scope, "var f=function(){return document.innerHTML;}; f();", "js", 1, null);
            System.out.println(result);

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

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
