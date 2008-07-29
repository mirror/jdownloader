package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class Rapidlayerin extends PluginForDecrypt {

    static private final String HOST = "rapidlayer.in";

    private String VERSION = "1.0.0";

    private String CODER = "JD-Team";
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?rapidlayer\\.in/go/[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    public Rapidlayerin() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginID() {
        return HOST + "-" + VERSION;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = (String) parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            // //if (step.getStep() == PluginStep.STEP_DECRYPT) {

            String link = null;
            URL url = new URL(cryptedLink);
            RequestInfo requestInfo = HTTP.getRequest(url);

            /* DownloadLink entschlüsseln */
            String fun_id = new Regex(requestInfo.getHtmlCode(), Pattern.compile("function (.*?)\\(", Pattern.CASE_INSENSITIVE)).getFirstMatch();
            String all = "function " + new Regex(requestInfo.getHtmlCode(), Pattern.compile("function (.*?)a=", Pattern.CASE_INSENSITIVE)).getFirstMatch();
            String dec = new Regex(requestInfo.getHtmlCode(), Pattern.compile("a=(.*?);document.write", Pattern.CASE_INSENSITIVE)).getFirstMatch();

            Context cx = Context.enter();
            Scriptable scope = cx.initStandardObjects();
            String fun = "function f(){ " + all + "\nreturn " + fun_id + "(" + dec + ")} f()";
            Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
            if ((link = JDUtilities.htmlDecode(Context.toString(result))) != null) decryptedLinks.add(this.createDownloadlink(link));
            Context.exit();
            // step.setParameter(decryptedLinks);

        } catch (MalformedURLException e) {
            
            e.printStackTrace();
        } catch (IOException e) {
            
            e.printStackTrace();
        }
        return decryptedLinks;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

}
