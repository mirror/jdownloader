package jd.http.ext;

import java.security.Policy;

import jd.http.Request;
import jd.http.ext.interfaces.BrowserEnviroment;

import org.appwork.utils.Regex;
import org.lobobrowser.html.domimpl.HTMLScriptElementImpl;

public class BasicBrowserEnviroment implements BrowserEnviroment {

    private String[] blackList = null;
    private String[] whiteList = null;
    private AdBlockerInterface adblocker;

    public BasicBrowserEnviroment(String[] blackList, String[] whitelist) {
        this.blackList = blackList;
        this.whiteList = whitelist;
        this.setAdblocker(AdBlocker.getInstance());

    }

    private void setAdblocker(AdBlockerInterface instance) {
        adblocker = instance;
    }

    public AdBlockerInterface getAdblocker() {
        return adblocker;
    }

    public boolean doLoadContent(Request request) {

        boolean ret = false;
        if (whiteList != null) {
            for (String b : whiteList) {
                String m = new Regex(request.getUrl().toString(), b).getMatch(-1);
                if (m != null && m.equals(request.getUrl().toString())) {
                    ret = true;
                    System.out.println("WHITE: " + b);
                    break;
                }
            }
        }
        if (!ret) {
            if (adblocker.doBlockRequest(request)) return false;
            ret = true;
            if (blackList != null) {

                for (String b : blackList) {
                    String m = new Regex(request.getUrl().toString(), b).getMatch(-1);
                    if (m != null && m.equals(request.getUrl().toString())) {
                        ret = false;
                        System.out.println("BLACK: " + b);
                        break;
                    }
                }

            }
        }
        System.out.println(request.getUrl() + "  choosen : " + ret);
        return ret;
    }

    public void prepareContents(Request request) {
        try {
            if (request.getHtmlCode() != null) {
                request.setHtmlCode(request.getHtmlCode().replaceAll("(filter: progid:DXImageTransform\\.Microsoft\\..*?;)", "/* CSSParserFilter $1*/"));
            }
        } catch (Exception e) {

        }
    }

    public boolean isImageLoadingEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    public String getAppCodeName() {
        // TODO Auto-generated method stub
        return "Mozilla";

    }

    public boolean isAutoProcessSubFrames() {
        // TODO Auto-generated method stub
        return true;
    }

    public String getAppMinorVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getAppName() {
        // TODO Auto-generated method stub
        return "Netscape";
    }

    public String getAppVersion() {
        // TODO Auto-generated method stub
        return "5.0 (Windows; de)";

    }

    public String getPlatform() {
        // TODO Auto-generated method stub
        return "Win32";
    }

    public String getProduct() {
        // TODO Auto-generated method stub
        return "Gecko";
    }

    public Policy getSecurityPolicy() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getVendor() {
        return null;
    }

    public boolean isExternalCSSEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isInternalCSSEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isScriptingEnabled() {
        // TODO Auto-generated method stub
        return true;
    }

    public String doScriptFilter(HTMLScriptElementImpl htmlScriptElementImpl, String text) {
        if (getAdblocker() == null) return text;
        return getAdblocker().prepareScript(text, htmlScriptElementImpl.getSrc());
    }

}
