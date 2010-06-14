package jd.http.ext.interfaces;

import java.security.Policy;

import jd.http.Request;

public interface BrowserEnviroment {

    public boolean doLoadContent(Request request);

    public void prepareContents(Request request);

    public boolean isImageLoadingEnabled();

    public boolean isAutoProcessSubFrames();

    public String getAppCodeName();

    public String getAppMinorVersion();

    public String getAppName();

    public String getAppVersion();

    public String getPlatform();

    public String getProduct();

    public Policy getSecurityPolicy();

    public String getVendor();

    public boolean isExternalCSSEnabled();

    public boolean isScriptingEnabled();

    public boolean isInternalCSSEnabled();

}
