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

package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.parser.Form.InputField;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.JavaScript;

public class FastLoadNet extends PluginForHost {

    private static final String CODER = "eXecuTe";

    private static final String HARDWARE_DEFECT = "Hardware-Defekt!";

    private static final String NOT_FOUND = "Datei existiert nicht";

    public FastLoadNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fast-load.net/infos.php";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        String downloadurl = downloadLink.getDownloadURL() + "&lg=de";
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getPage(downloadurl);

        if (br.containsHTML(NOT_FOUND)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setName(downloadLink.getDownloadURL().substring(downloadurl.indexOf("pid=") + 4));
            return false;
        }

        if (br.containsHTML(HARDWARE_DEFECT)) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            downloadLink.setName(downloadLink.getDownloadURL().substring(downloadurl.indexOf("pid=") + 4));
            return false;
        }
        String txt = (br + "").replaceAll("<.*?>", "|");
        txt = txt.replaceAll("\r", "|");
        txt = txt.replaceAll("\n", "|");
        txt = txt.replaceAll("\\|\\s+\\|", "|");
        txt = txt.replaceAll("[|]+", "|");
        String fileName = Encoding.htmlDecode(new Regex(txt, "Datei\\|(.+?)\\|").getMatch(0));
        String fileSize = Encoding.htmlDecode(new Regex(txt, "Gr&ouml;sse\\|(.+?)\\|").getMatch(0));
        // downloadinfos gefunden? -> download verfügbar
        if (fileName != null && fileSize != null) {
            downloadLink.setName(fileName.trim());
            downloadLink.setDownloadSize(Regex.getSize(fileSize));
            return true;
        }
        downloadLink.setName(downloadurl.substring(downloadurl.indexOf("pid=") + 4));
        return false;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {        LinkStatus linkStatus = downloadLink.getLinkStatus();
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        String downloadurl = downloadLink.getDownloadURL() + "&lg=de";
        HTTPConnection urlConnection;

        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(getHost() + " " + JDLocale.L("plugins.host.server.unavailable", "Serverfehler"));
            return;
        }
      String pre=  br.toString();
        String page = JavaScript.evalPage(br);
        br.getRequest().setHtmlCode(page+pre);
        Form captcha_form = getDownloadForm();

        if (captcha_form != null) {
            boolean valid = false;
            for (int retry = 1; retry <= 5; retry++) {
                captcha_form = getDownloadForm();
                if (captcha_form != null) {
                    File file = this.getLocalCaptchaFile(this);
                    String url = "includes/captcha.php";
                    String[] phps = br.getRegex("\"(.{5,40}\\.php)").getColumn(0);
                    String captcha = null;
                    int best = Integer.MAX_VALUE;
                    if (phps != null) {
                        for (int i = 0; i < phps.length; i++) {
                            int lev = JDUtilities.getLevenshteinDistance(url, phps[i]);
                            if (lev < best) {
                                best = lev;
                                captcha = phps[i];
                            }
                        }
                    }
                    phps = br.getRegex("'(.{5,40}\\.php)").getColumn(0);
                    if (phps != null) {
                        for (int i = 0; i < phps.length; i++) {
                            int lev = JDUtilities.getLevenshteinDistance(url, phps[i]);
                            if (lev < best) {
                                best = lev;
                                captcha = phps[i];
                            }

                        }
                    }
                

                    Browser.download(file, br.cloneBrowser().openGetConnection("/includes/captcha123.php"));
                    String code = Plugin.getCaptchaCode(file, this, downloadLink);

                    ArrayList<InputField> fields = captcha_form.getInputFieldsByType("text");
                    InputField txt = null;
                    for (InputField f : fields) {
                        if (f.getStringProperty("style", "").contains("none")) continue;
                        txt = f;
                        break;
                    }
                    if(txt!=null)                    txt.setValue(code);

                    br.openFormConnection(captcha_form);
                    if (br.getHttpConnection().isContentDisposition()) {
                        valid = true;
                        break;
                    } else {
                        br.getPage(downloadurl);
                    }
                } else {
                    valid = true;
                    break;
                }
            }

            if (valid == false) {
                linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                return;
            }
            urlConnection = br.getHttpConnection();
        } else {
            String dl_url = br.getRegex(Pattern.compile("type=\"button\" onclick=\"location='(.*?)'\" value=\"download", Pattern.CASE_INSENSITIVE)).getMatch(0);
            urlConnection = br.openGetConnection(dl_url);
        }

        long length = urlConnection.getContentLength();

        if (urlConnection.getContentType() != null) {

            if (urlConnection.getContentType().contains("text/html")) {

                if (length == 184) {
                    logger.info("System overload: Retry in 20 seconds");
                    linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                    linkStatus.setValue(20 * 60 * 1000l);
                    return;
                } else if (length == 169) {
                    logger.severe("File not found: File is deleted from Server");
                    linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                    return;
                } else if (length == 529) {
                    logger.severe("File not found: Unkown 404 Error");
                    linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                    return;
                } else {
                    logger.severe("Unknown error page - [Length: " + length + "]");
                    linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
                    return;
                }
            }
            // Download starten
            dl = new RAFDownload(this, downloadLink, urlConnection);
            dl.setResume(false);
            dl.setChunkNum(1);
            dl.startDownload();
            return;

        } else {
            logger.severe("Couldn't get HTTP connection");
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }
    }

    private Form getDownloadForm() {
        /* richtige form suchen, da fakeforms verwendet werden */
        Form[] forms = br.getForms();
        if (forms != null) {
            for (int i = 0; i < forms.length; i++) {
                if (forms[i].getVars().size() >= 2) { return forms[i]; }
            }
        }
        return null;
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}