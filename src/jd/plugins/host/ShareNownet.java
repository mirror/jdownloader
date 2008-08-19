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

public class ShareNownet extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "share-now.net";

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?share-now\\.net/{1,}files/\\d+-(.*?)\\.html", Pattern.CASE_INSENSITIVE);

    private String captchaCode;
    private File captchaFile;
    private String downloadurl;

    public ShareNownet() {
        super();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://share-now.net/agb.php";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        Browser.clearCookies(HOST);
        br.setFollowRedirects(false);
        downloadurl = downloadLink.getDownloadURL();
        try {
            br.getPage(downloadurl);
            if (!br.containsHTML("Datei existiert nicht oder wurde gel&ouml;scht!")) {

                String linkinfo[][] = new Regex(br.getRequest().getHtmlCode(), Pattern.compile("<h3 align=\"center\"><strong>(.*?)</strong> \\(\\s*([0-9\\.]*)\\s([GKMB]*)\\s*\\) </h3>", Pattern.CASE_INSENSITIVE)).getMatches();
                if (linkinfo.length != 1) {
                    linkinfo = br.getRegex("<span class=\"style1\">(.*?)\\(([0-9\\.]*)\\s*([GKMB]*)\\) </span>").getMatches();
                }
                if (linkinfo[0][2].matches("MB")) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(linkinfo[0][1]) * 1024 * 1024));
                } else if (linkinfo[0][2].matches("KB")) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(linkinfo[0][1]) * 1024));
                }
                downloadLink.setName(linkinfo[0][0]);
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
        return HOST;
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

        Form form = br.getForm(1);

        /* gibts nen captcha? */
        if (br.containsHTML("Sicherheitscode eingeben")) {
            /* Captcha File holen */
            captchaFile = getLocalCaptchaFile(this);

            if (!br.downloadFile(captchaFile, "http://share-now.net/captcha.php?id=" + form.getVars().get("download")) || !captchaFile.exists()) {
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
            form.put("captcha", captchaCode);
        }
        /* DownloadLink holen/Captcha check */
        HTTPConnection con = br.openFormConnection(form);
        if (br.getRedirectLocation() != null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        /* Datei herunterladen */

        if (con.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        dl = new RAFDownload(this, downloadLink, con);
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
