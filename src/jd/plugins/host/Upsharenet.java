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
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class Upsharenet extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "upshare.net";

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?upshare\\.(net|eu)/download\\.php\\?id=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    private static final String PLUGIN_NAME = HOST;
    private String captchaCode;
    private File captchaFile;
    private String downloadurl;
    private String passCode = null;

    public Upsharenet() {
        super();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://www.upshare.net/faq.php?setlang=en";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {

        /* .eu zu .net weiterleitung */
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("upshare\\.(net|eu)", "upshare\\.net"));
        Browser.clearCookies(HOST);

        downloadurl = downloadLink.getDownloadURL();
        try {
            br.getPage(downloadurl);

            if (!br.containsHTML("Your requested file is not found")) {
                String linkinfo[][] = new Regex(br, Pattern.compile("<b>File size:</b></td>[\\r\\n\\s]*<td align=left>([0-9\\.]*) ([GKMB]*)</td>", Pattern.CASE_INSENSITIVE)).getMatches();

                if (linkinfo[0][1].matches("MB")) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(linkinfo[0][0]) * 1024 * 1024));
                } else if (linkinfo[0][1].matches("KB")) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(linkinfo[0][0]) * 1024));
                }
                downloadLink.setName(new URL(downloadurl).getQuery().substring(3));
                return true;
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        Form form = br.getForms()[1];
        /* Captcha File holen */
        captchaFile = getLocalCaptchaFile(this);

        if (!br.downloadFile(captchaFile, "http://www.upshare.net/captcha.php") || !captchaFile.exists()) {
            /* Fehler beim Captcha */
            logger.severe("Captcha Download fehlgeschlagen!");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        /* CaptchaCode holen */
        if ((captchaCode = Plugin.getCaptchaCode(captchaFile, this)) == null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        form.put("captchacode", captchaCode);
        /* Passwort holen holen */
        if (form.getVars().containsKey("downloadpw")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                if ((passCode = JDUtilities.getGUI().showUserInputDialog("Code?")) == null) {
                    passCode = "";
                }
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            form.put("downloadpw", passCode);
        }
        /* Pass/Captcha check */
        br.submitForm(form);

        if (br.containsHTML("<span>Password Error</span>")) {
            /* PassCode war falsch, also Löschen */
            downloadLink.setProperty("pass", null);
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        if (br.containsHTML("<span>Captcha number error or expired</span>")) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        if (br.containsHTML("<span>You have got max allowed download sessions from the same IP!</span>")) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.setValue(60 * 60 * 1000);
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            return;
        }
        /* PassCode war richtig, also Speichern */
        downloadLink.setProperty("pass", passCode);
        /* DownloadLink holen */
        String link = new Regex(br, Pattern.compile("document.location=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (link == null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            return;
        }
        br.openGetConnection(link);
        if (br.getRedirectLocation() != null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            return;
        }
        /* Datei herunterladen */
        HTTPConnection urlConnection = br.getHttpConnection();
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
