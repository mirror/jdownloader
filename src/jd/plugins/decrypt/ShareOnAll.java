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
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class ShareOnAll extends PluginForDecrypt {
    final static String host = "shareonall.com";
    private String version = "1.0.0.0";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?shareonall\\.com/(.*?)\\.htm", Pattern.CASE_INSENSITIVE);

    public ShareOnAll() {
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
        return "ShareOnAll.com-1.0.0.";
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
        String cryptedLink = (String) parameter;
        logger.info(cryptedLink);
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                String id = new Regex(cryptedLink, patternSupported).getFirstMatch();
                URL url = new URL("http://www.shareonall.com/showlinks.php?f=" + id + ".htm");
                RequestInfo reqInfo = HTTP.getRequest(url);
                boolean do_continue = false;
                Form form;
                for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                    if (reqInfo.containsHTML("<img src='code")) {
                        form = reqInfo.getForms()[0];
                        String captchaAddress = new Regex(reqInfo.getHtmlCode(), Pattern.compile("src='code/(.*?)'", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                        captchaAddress = "http://www.shareonall.com/code/" + captchaAddress;
                        HTTPConnection captcha_con = new HTTPConnection(new URL(captchaAddress).openConnection());
                        captcha_con.setRequestProperty("Cookie", reqInfo.getCookie());
                        File captchaFile = this.getLocalCaptchaFile(this);
                        if (!JDUtilities.download(captchaFile, captchaAddress) || !captchaFile.exists()) {
                            /* Fehler beim Captcha */
                            logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                            step.setParameter(null);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                        String captchaCode = Plugin.getCaptchaCode(captchaFile, this);
                        if (captchaCode == null) {
                            /* abbruch geklickt */
                            step.setParameter(decryptedLinks);
                            return null;
                        }
                        captchaCode = captchaCode.toUpperCase();
                        form.put("c", captchaCode);
                        reqInfo = form.getRequestInfo();
                    } else {
                        do_continue = true;
                        break;
                    }
                }
                if (do_continue == true) {
                    // Links herausfiltern
                    String links[][] = new Regex(reqInfo.getHtmlCode(), Pattern.compile("<a href=\'(.*?)\' target='_blank'>", Pattern.CASE_INSENSITIVE)).getMatches();
                    progress.setRange(links.length);
                    for (int i = 0; i < links.length; i++) {
                        decryptedLinks.add(this.createDownloadlink(links[i][0]));
                        progress.increase(1);
                    }
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