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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class Odsiebiecom extends PluginForHost {
    private static final String CODER = "JD-Team";

    private String captchaCode;
    private File captchaFile;
    private String downloadurl;

    public Odsiebiecom(PluginWrapper wrapper) {
        super(wrapper);
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
        try {
            br.getPage(downloadLink.getDownloadURL());
            if (br.getRedirectLocation() == null) {
                String filename = br.getRegex("Nazwa pliku: <strong>(.*?)</strong>").getMatch(0);
                String filesize;
                if ((filesize = br.getRegex("Rozmiar pliku: <strong>(.*?)MB</strong>").getMatch(0)) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize) * 1024 * 1024));
                } else if ((filesize = br.getRegex("Rozmiar pliku: <strong>(.*?)KB</strong>").getMatch(0)) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize) * 1024));
                }
                downloadLink.setName(filename);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
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
            return;
        }
        /*
         * Zuerst schaun ob wir nen Button haben oder direkt das File vorhanden
         * ist
         */
        String steplink = br.getRegex("<a href=\"/pobierz/(.*?)\"  style=\"font-size: 18px\">(.*?)</a>").getMatch(0);
        if (steplink == null) {
            /* Kein Button, also muss der Link irgendwo auf der Page sein */
            /* Film,Mp3 */
            downloadurl = br.getRegex("<PARAM NAME=\"FileName\" VALUE=\"(.*?)\"").getMatch(0);
            /* Flash */
            if (downloadurl == null) {
                downloadurl = br.getRegex("<PARAM NAME=\"movie\" VALUE=\"(.*?)\"").getMatch(0);
            }
            /* Bilder, Animationen */
            if (downloadurl == null) {
                downloadurl = br.getRegex("onLoad=\"scaleImg\\('thepic'\\)\" src=\"(.*?)\" \\/").getMatch(0);
            }
            /* kein Link gefunden */
            if (downloadurl == null) {
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                return;
            }
        } else {
            /* Button folgen, schaun ob Link oder Captcha als nächstes kommt */
            downloadurl = "http://odsiebie.com/pobierz/" + steplink;
            br.getPage(downloadurl);            
            downloadurl = "http://odsiebie.com/pobierz/" + steplink + ".html";
            br.getPage(downloadurl);
            if (br.getRedirectLocation() != null) {
                /* Weiterleitung auf andere Seite, evtl mit Captcha */
                downloadurl = br.getRedirectLocation();
                br.getPage(downloadurl);
            }
            if (br.getRegex(Pattern.compile("<img src=\"(.*?odsiebie.*?ca.*?php)\">", Pattern.CASE_INSENSITIVE)).matches()) {
                /* Captcha File holen */
                String captchaurl = br.getRegex(Pattern.compile("<img src=\"(.*?odsiebie.*?ca.*?php)\">", Pattern.CASE_INSENSITIVE)).getMatch(0);
                captchaFile = getLocalCaptchaFile(this);
                Browser cap_br = br.cloneBrowser();
                HTTPConnection captcha_con = cap_br.openGetConnection(captchaurl);
                if (captcha_con.getContentType().contains("text")) {
                    /* Fehler beim Captcha */
                    logger.severe("Captcha Download fehlgeschlagen!");
                    // step.setStatus(PluginStep.STATUS_ERROR);
                    linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                    return;
                }
                Browser.download(captchaFile, captcha_con);
                /* CaptchaCode holen */
                if ((captchaCode = Plugin.getCaptchaCode(captchaFile, this)) == null) {
                    linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                    return;
                }
                /* Überprüfen(Captcha,Password) */
                downloadurl = "http://odsiebie.com/pobierz/" + steplink + ".html?captcha=" + captchaCode;
                br.getPage(downloadurl);
                if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("html?err")) {
                    linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                    return;
                }
            }
            /* DownloadLink suchen */
            steplink = br.getRegex("<a href=\"/download/(.*?)\"").getMatch(0);
            if (steplink == null) {
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                return;
            }
            downloadurl = "http://odsiebie.com/download/" + steplink;
            br.getPage(downloadurl);
            if (br.getRedirectLocation() == null || br.getRedirectLocation().contains("upload")) {
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                return;
            }
            downloadurl = br.getRedirectLocation();
            if (downloadurl == null) {
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                return;
            }
        }
        /*
         * Leerzeichen müssen durch %20 ersetzt werden!!!!!!!!, sonst werden sie
         * von new URL() abgeschnitten
         */
        downloadurl = downloadurl.replaceAll(" ", "%20");
        /* Datei herunterladen */
        HTTPConnection urlConnection = br.openGetConnection(downloadurl);
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setChunkNum(1);
        dl.setResume(false);
        dl.startDownload();
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
