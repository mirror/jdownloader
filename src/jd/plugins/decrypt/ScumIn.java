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


package jd.plugins.decrypt;  import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class ScumIn extends PluginForDecrypt {

    static private String host = "scum.in";

    private String version = "1.0.0.0";
    
    private Pattern patternSupported = getSupportPattern("http://[*]scum.in/index.php\\?id=[+]");

    public ScumIn() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "Scum.in-1.0.0.";
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                RequestInfo reqinfo = HTTP.getRequest(new URL(parameter));
                String cookie = reqinfo.getCookie();
                String id = parameter.split("=")[1];

                String urlString = URLDecoder.decode("http://scum.in/share/includes/captcha.php?t=", "UTF-8");
                URL url = new URL(urlString);
                HTTPConnection con = new HTTPConnection(url.openConnection());
                con.setReadTimeout(HTTP.getReadTimeoutFromConfiguration());
                con.setConnectTimeout(HTTP.getConnectTimeoutFromConfiguration());
                con.setInstanceFollowRedirects(false);
                con.setRequestProperty("Referer", parameter);
                con.setRequestProperty("Cookie", cookie);
                con.setRequestProperty("Accept", "image/png,*/*;q=0.5");
                con.setRequestProperty("Accept-Encoding", "gzip,deflate");
                con.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
                con.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
                con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
                
                File captchaFile = this.getLocalCaptchaFile(this);
                if (!JDUtilities.download(captchaFile, con) || !captchaFile.exists()) {
                    step.setParameter(null);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                String captchaCode = Plugin.getCaptchaCode(captchaFile, this);
                
                reqinfo = HTTP.postRequest(new URL("http://scum.in/plugins/home/links.old.callback.php"), cookie, parameter, null, "id=" + id + "&captcha=" + captchaCode, false);
                
                ArrayList<ArrayList<String>> links = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), "href=\"°\"");

                progress.setRange(links.size());
                
                for(int i=0; i<links.size(); i++) {
                    progress.increase(1);
                    decryptedLinks.add(this.createDownloadlink(links.get(i).get(0)));
                }

                step.setParameter(decryptedLinks);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}