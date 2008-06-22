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

package jd.plugins.decrypt;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import jd.parser.HTMLParser;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class LinksaveIn extends PluginForDecrypt {

    static private final String HOST = "Linksave.in";

    private String VERSION = "0.0.1";

    private String CODER = "jD-Team";

    // http://xlice.net/getdlc/a50d323947054cc204362a47ddd5bc49/
    static private final Pattern patternSupported = getSupportPattern("http://linksave.in/[+]/[+].[+]|http://linksave.in/[+]");
    
    static private final Pattern patternCaptcha = Pattern.compile("img id=\"captcha\" src=\"\\./captcha/captcha\\.php(.+?)\"");

    public LinksaveIn() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
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
    public PluginStep doStep(PluginStep step, String parameter) {
        // surpress jd warning
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        step.setParameter(decryptedLinks);
        String url = parameter;
        String[] sp = parameter.split("[/|\\\\]");
        if (sp.length < 3) return step;
        String id = sp[3];
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            File container = null;
//            boolean folder = false;
            if (parameter.endsWith(".rsdf")) {
                container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".rsdf");
            } else if (parameter.endsWith(".ccf")) {
                container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".ccf");

            } else if (parameter.endsWith(".dlc")) {
                container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");

            } else {
//                folder = true;
                RequestInfo ri;
                try {
                    ri = HTTP.getRequest(new URL(parameter));
                    
                    Matcher matcher = patternCaptcha.matcher(ri.getHtmlCode());
                    String cookie = ri.getCookie();
                    while (matcher.find()) {
                        logger.info("Captcha protected");
                        String captchaAddress = "http://www.linksave.in/captcha/captcha.php" + matcher.group(1);

                        File captchaFile = this.getLocalCaptchaFile(this);
                        if (!JDUtilities.download(captchaFile, captchaAddress) || !captchaFile.exists()) {
                            logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                            step.setParameter(null);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                        HashMap<String, String> reqpro = new HashMap<String, String>();
                        reqpro.put("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
                        reqpro.put("Accept-Encoding", "gzip,deflate");
                        reqpro.put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
                        
                        String captchaCode = Plugin.getCaptchaCode(captchaFile, this);
                        String postdata = HTMLParser.getFormInputHidden(SimpleMatches.getBetween(ri.getHtmlCode(), "form name=\"captchaform\" method=\"post\"", "/form")) + "&code=" + captchaCode + "&x=0&y=0";
                        ri = HTTP.postRequest(new URL(parameter), cookie, parameter, reqpro, postdata, false);
                        
                        if(ri.getHtmlCode().contains("Der eingegebene Captcha-code ist falsch"))
                            ri = HTTP.getRequest(new URL(parameter));
                        else
                            ri = HTTP.getRequest(new URL(parameter), ri.getCookie(), parameter, false);
                        matcher = patternCaptcha.matcher(ri.getHtmlCode());
                    }

                    if (ri.containsHTML(".dlc")) {
                        url = parameter + "/" + id + ".dlc";
                        container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
                    } else if (ri.containsHTML(".rsdf")) {
                        url = parameter + "/" + id + ".rsdf";
                        container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".rsdf");

                    } else if (ri.containsHTML(".ccf")) {
                        url = parameter + "/" + id + ".ccf";
                        container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".ccf");

                    } else {
                        return step;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    return step;
                }

            }

            HTTPConnection con;
            try {
                con = new HTTPConnection(new URL(url).openConnection());
                con.setRequestProperty("Referer", "http://linksave.in/" + id);
            } catch (Exception e) {

                e.printStackTrace();
                return step;
            }

            if (JDUtilities.download(container, con)) {

                //JDUtilities.getController().loadContainerFile(container);

            }
        }

        return step;

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

}
