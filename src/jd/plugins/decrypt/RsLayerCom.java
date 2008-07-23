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
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.Form;
import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class RsLayerCom extends PluginForDecrypt {
    final static String host = "rs-layer.com";
    private String version = "0.3";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?rs-layer\\.com/.+\\.html", Pattern.CASE_INSENSITIVE);
    private static String strCaptchaPattern = "<img src=\"(captcha-[^\"]*\\.png)\" ";
    private static Pattern linkPattern = Pattern.compile("onclick=\"getFile\\('([^;]*)'\\)");

    public RsLayerCom() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "jD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return host + "-" + version;
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

                if (parameter.indexOf("/link-") != -1) {

                    String link = SimpleMatches.getBetween(reqinfo.getHtmlCode(), "<iframe src=\"", "\" ");
                    link = decryptEntities(link);

                    progress.setRange(1);
                    decryptedLinks.add(this.createDownloadlink(link));
                    progress.increase(1);
                    step.setParameter(decryptedLinks);

                } else if (parameter.indexOf("/directory-") != -1) {

                    Form[] forms = Form.getForms(reqinfo);

                    if (forms != null && forms.length != 0 && forms[0] != null) {
                        Form captchaForm = forms[0];

                        String captchaFileName = new Regex(reqinfo.getHtmlCode(), strCaptchaPattern).getFirstMatch(1);

                        if (captchaFileName == null) {
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return null;
                        }
                        String captchaUrl = "http://" + host + "/" + captchaFileName;
                        File captchaFile = getLocalCaptchaFile(this, ".png");
                        boolean fileDownloaded = JDUtilities.download(captchaFile, HTTP.getRequestWithoutHtmlCode(new URL(captchaUrl), reqinfo.getCookie(), null, true).getConnection());

                        if (!fileDownloaded) {
                            logger.info(JDLocale.L("plugins.decrypt.general.captchaDownloadError", "Captcha Download gescheitert"));
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return null;
                        }

                        String captchaCode = Plugin.getCaptchaCode(captchaFile, this);

                        if (null == captchaCode || captchaCode.length() == 0) {
                            logger.info(JDLocale.L("plugins.decrypt.rslayer.invalidCaptchaCode", "ungültiger Captcha Code"));
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return null;
                        }

                        captchaForm.put("captcha_input", captchaCode);

                        reqinfo = HTTP.readFromURL(captchaForm.getConnection());

                        if (reqinfo.containsHTML("Sicherheitscode<br />war nicht korrekt")) {
                            logger.info(JDLocale.L("plugins.decrypt.general.captchaCodeWrong", "Captcha Code falsch"));
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return null;
                        }

                        if (reqinfo.containsHTML("Gültigkeit für den<br> Sicherheitscode<br>ist abgelaufen")) {

                            logger.info(JDLocale.L("plugins.decrypt.rslayer.captchaExpired", "Sicherheitscode abgelaufen"));
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return null;

                        }

                    }

                    ArrayList<String> layerLinks = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), linkPattern, 1);
                    progress.setRange(layerLinks.size());

                    for (String fileId : layerLinks) {

                        String layerLink = "http://rs-layer.com/link-" + fileId + ".html";

                        RequestInfo request2 = HTTP.getRequest(new URL(layerLink));
                        String link = SimpleMatches.getBetween(request2.getHtmlCode(), "<iframe src=\"", "\" ");

                        decryptedLinks.add(this.createDownloadlink(link));
                        progress.increase(1);

                    }

                    step.setParameter(decryptedLinks);

                }

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

    // Zeichencode-Entities (&#124 etc.) in normale Zeichen umwandeln
    private String decryptEntities(String str) {

        ArrayList<ArrayList<String>> codes = SimpleMatches.getAllSimpleMatches(str, "&#°;");
        String decodedString = "";

        for (int i = 0; i < codes.size(); i++) {

            int code = Integer.parseInt(codes.get(i).get(0));
            char[] asciiChar = { (char) code };
            decodedString += String.copyValueOf(asciiChar);

        }

        return decodedString;

    }

}