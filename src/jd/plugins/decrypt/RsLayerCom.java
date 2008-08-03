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
import java.util.regex.Pattern;

import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class RsLayerCom extends PluginForDecrypt {
    final static String host = "rs-layer.com";
    private static Pattern linkPattern = Pattern.compile("onclick=\"getFile\\('([^;]*)'\\)", Pattern.CASE_INSENSITIVE);
    private static String strCaptchaPattern = "<img src=\"(captcha-[^\"]*\\.png)\" ";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?rs-layer\\.com/(.+)\\.html", Pattern.CASE_INSENSITIVE);

    public RsLayerCom() {
        super();
    }

    private boolean add_container(RequestInfo reqinfo, String cryptedLink, String ContainerFormat) throws IOException {
        String link_id = new Regex(cryptedLink, patternSupported).getFirstMatch();
        String container_link = "http://rs-layer.com/" + link_id + ContainerFormat;
        if (reqinfo.containsHTML(container_link)) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ContainerFormat);
            URL container_url = new URL(container_link);
            HTTPConnection container_con = new HTTPConnection(container_url.openConnection());
            container_con.setRequestProperty("Referer", cryptedLink);
            container_con.setRequestProperty("Cookie", reqinfo.getCookie());
            if (JDUtilities.download(container, container_con)) {
                JDUtilities.getController().loadContainerFile(container);
                return true;
            }
        }
        return false;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            RequestInfo reqinfo = HTTP.getRequest(new URL(parameter));
            if (parameter.indexOf("/link-") != -1) {
                String link = new Regex(reqinfo.getHtmlCode(), "<iframe src=\"(.*?)\" ", Pattern.CASE_INSENSITIVE).getFirstMatch();
                link = JDUtilities.htmlDecode(link);
                if (link == null) {
                    return null;
                } else {
                    decryptedLinks.add(createDownloadlink(link));
                }
                ;
            } else if (parameter.indexOf("/directory-") != -1) {
                Form[] forms = Form.getForms(reqinfo);
                if (forms != null && forms.length != 0 && forms[0] != null) {
                    Form captchaForm = forms[0];
                    String captchaFileName = new Regex(reqinfo.getHtmlCode(), strCaptchaPattern).getFirstMatch(1);
                    if (captchaFileName == null) { return null; }
                    String captchaUrl = "http://" + host + "/" + captchaFileName;
                    File captchaFile = Plugin.getLocalCaptchaFile(this, ".png");
                    boolean fileDownloaded = JDUtilities.download(captchaFile, HTTP.getRequestWithoutHtmlCode(new URL(captchaUrl), reqinfo.getCookie(), null, true).getConnection());
                    if (!fileDownloaded) {
                        logger.info(JDLocale.L("plugins.decrypt.general.captchaDownloadError", "Captcha Download gescheitert"));
                        return null;
                    }
                    String captchaCode = Plugin.getCaptchaCode(captchaFile, this);
                    if (null == captchaCode || captchaCode.length() == 0) {
                        logger.info(JDLocale.L("plugins.decrypt.rslayer.invalidCaptchaCode", "ungültiger Captcha Code"));
                        return null;
                    }
                    captchaForm.put("captcha_input", captchaCode);
                    reqinfo = HTTP.readFromURL(captchaForm.getConnection());
                    if (reqinfo.containsHTML("Sicherheitscode<br />war nicht korrekt")) {
                        logger.info(JDLocale.L("plugins.decrypt.general.captchaCodeWrong", "Captcha Code falsch"));
                        return null;
                    }
                    if (reqinfo.containsHTML("Gültigkeit für den<br> Sicherheitscode<br>ist abgelaufen")) {
                        logger.info(JDLocale.L("plugins.decrypt.rslayer.captchaExpired", "Sicherheitscode abgelaufen"));
                        return null;
                    }
                }
                String layerLinks[][] = new Regex(reqinfo.getHtmlCode(), linkPattern).getMatches();
                if (layerLinks.length == 0) {
                    if (!add_container(reqinfo, parameter, ".dlc")) {
                        if (!add_container(reqinfo, parameter, ".rsdf")) {
                            add_container(reqinfo, parameter, ".ccf");
                        }
                    }
                } else {
                    progress.setRange(layerLinks.length);
                    for (String[] element : layerLinks) {
                        String layerLink = "http://rs-layer.com/link-" + element[0] + ".html";
                        RequestInfo request2 = HTTP.getRequest(new URL(layerLink));
                        String link = new Regex(request2.getHtmlCode(), "<iframe src=\"(.*?)\" ", Pattern.CASE_INSENSITIVE).getFirstMatch();
                        if (link != null) decryptedLinks.add(createDownloadlink(link));
                        progress.increase(1);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
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
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

}