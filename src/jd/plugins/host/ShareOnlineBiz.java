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
import java.net.URL;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class ShareOnlineBiz extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "share-online.biz";

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?share\\-online\\.biz/download.php\\?id\\=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    // private static final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getFirstMatch().*= "2.0.0.0";

    // private static final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();

    private static final String PLUGIN_NAME = HOST;
    private String captchaCode;
    private File captchaFile;
    private String passCode;
    private RequestInfo requestInfo;
    private String url;

    public ShareOnlineBiz() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://share-online.biz/rules.php";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        url = downloadLink.getDownloadURL();
        for (int i = 1; i < 3; i++) {
            try {
                Thread.sleep(1000);/*
                 * Sicherheitspause, sonst gibts 403
                 * Response
                 */
                requestInfo = HTTP.getRequest(new URL(url));
                if (requestInfo != null && requestInfo.getLocation() == null) {
                    String filename = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<span class=\"locatedActive\">Download (.*?)</span>", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                    String sizev[][] = new Regex(requestInfo.getHtmlCode(), Pattern.compile("</font> \\((.*?) (.*?)\\) angefordert", Pattern.CASE_INSENSITIVE)).getMatches();

                    double size = Double.parseDouble(sizev[0][0].trim());
                    String type = sizev[0][1].trim().toLowerCase();
                    int filesize = 0;
                    if (type.equals("mb")) {
                        filesize = (int) (1024 * 1024 * size);
                    } else if (type.equals("kb")) {
                        filesize = (int) (1024 * size);
                    } else {
                        filesize = (int) size;
                    }
                    downloadLink.setDownloadSize(filesize);
                    downloadLink.setName(filename);
                    return true;
                }
            } catch (Exception e) {

                e.printStackTrace();
            }
        }
        downloadLink.setAvailable(false);
        return false;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    /*public int getMaxSimultanDownloadNum() {
        return 1;
    }

    @Override
   */ public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        url = downloadLink.getDownloadURL();
        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        /* Captcha File holen */
        captchaFile = getLocalCaptchaFile(this);
        HTTPConnection captcha_con = new HTTPConnection(new URL("http://www.share-online.biz/captcha.php").openConnection());
        captcha_con.setRequestProperty("Referer", url);
        captcha_con.setRequestProperty("Cookie", requestInfo.getCookie());
        if (!captcha_con.getContentType().contains("text") && !Browser.download(captchaFile, captcha_con) || !captchaFile.exists()) {
            /* Fehler beim Captcha */
            logger.severe("Captcha Download fehlgeschlagen!");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);// step.setParameter("Captcha
            // ImageIO
            // Error");
            return;
        }

        /* CaptchaCode holen */
        if ((captchaCode = Plugin.getCaptchaCode(captchaFile, this)) == null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        /* Passwort holen holen */
        if (requestInfo.getHtmlCode().contains("name=downloadpw")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                if ((passCode = JDUtilities.getGUI().showUserInputDialog("Code?")) == null) {
                    passCode = "";
                }
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
        }

        /* Überprüfen(Captcha,Password) */
        requestInfo = HTTP.postRequest((new URL(url)), requestInfo.getCookie(), url, null, "captchacode=" + captchaCode + "&downloadpw=" + passCode, true);
        if (requestInfo.getHtmlCode().contains("<span>Die Nummer ist leider nicht richtig oder ausgelaufen!</span>") || requestInfo.getHtmlCode().contains("Tippfehler")) {
            if (requestInfo.getHtmlCode().contains("Tippfehler")) {
                /* PassCode war falsch, also Löschen */
                downloadLink.setProperty("pass", null);
            }
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        /* Downloadlimit erreicht */
        if (requestInfo.getHtmlCode().contains("<span>Entschuldigung")) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            sleep(60 * 60 * 1000, downloadLink);
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            return;
        }
        /* PassCode war richtig, also Speichern */
        downloadLink.setProperty("pass", passCode);
        /* DownloadLink holen, thx @dwd */
        String all = requestInfo.getRegexp("eval\\(unescape\\(.*?\"\\)\\)\\);").getFirstMatch();
        String dec = requestInfo.getRegexp("loadfilelink\\.decode\\(\".*?\"\\);").getFirstMatch();
        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects();
        String fun = "function f(){ " + all + "\nreturn " + dec + "} f()";
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
        url = Context.toString(result);
        Context.exit();

        /* 15 seks warten */
        sleep(15000, downloadLink);

        /* Datei herunterladen */
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(url), requestInfo.getCookie(), downloadLink.getDownloadURL(), false);
        HTTPConnection urlConnection = requestInfo.getConnection();
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setChunkNum(1);
        dl.setResume(false);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }

}
