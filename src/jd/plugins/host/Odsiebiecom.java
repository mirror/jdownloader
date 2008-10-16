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
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class Odsiebiecom extends PluginForHost {
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
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            br.getPage(downloadLink.getDownloadURL());
            if (br.getRedirectLocation() == null) {
                String filename = br.getRegex(Pattern.compile("<dt>Nazwa pliku:</dt>.*?<dd>(.*?)</dd>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
                String filesize;
                if ((filesize = br.getRegex(Pattern.compile("<dt>Rozmiar pliku:</dt>.*?<dd>(.*?)MB</dd>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0)) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize) * 1024 * 1024));
                } else if ((filesize = br.getRegex(Pattern.compile("<dt>Rozmiar pliku:</dt>.*?<dd>(.*?)KB</dd>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0)) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize) * 1024));
                }
                downloadLink.setName(filename);
                return true;
            }
        } catch (Exception e) {
        }
        downloadLink.setAvailable(false);
        return false;
    }

    @Override
    public String getVersion() {

        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        /*
         * Zuerst schaun ob wir nen Button haben oder direkt das File vorhanden
         * ist
         */
        captchaCode = null;
        String steplink = br.getRegex("<a class=\".*?\" href=\"/pobierz/(.*?)\">Pobierz plik</a>").getMatch(0);
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
            if (downloadurl == null) { throw new PluginException(LinkStatus.ERROR_FATAL); }
        } else {
            /* Button folgen, schaun ob Link oder Captcha als nächstes kommt */
            downloadurl = "http://odsiebie.com/pobierz/" + steplink;
            br.getPage(downloadurl);
            if (br.getRedirectLocation() != null) {
                /* Weiterleitung auf andere Seite, evtl mit Captcha */
                downloadurl = br.getRedirectLocation();
                br.getPage(downloadurl);
            }
            Form capform = br.getFormbyName("wer");
            if (capform != null) {
                /* Captcha File holen */

                String[] captchaurls = capform.getRegex("<img(.*?src=\".*?\".*?)/>").getColumn(0);
                String captchaurl = null;

                for (String url : captchaurls) {
                    if (!url.contains("style")) {
                        captchaurl = new Regex(url, "src.*?=.*?\"(.*?)\"").getMatch(0);
                        break;
                    }
                    if (url.contains("style")) {
                        if (!new Regex(url, "display:none[ ]*?\"").matches()) {
                            captchaurl = new Regex(url, "src.*?=.*?\"(.*?)\"").getMatch(0);
                            break;
                        } else {
                            String[] captchacodes = capform.getRegex("<font(.*?style=\".*?\".*?.*?>.*?<)").getColumn(0);
                            for (String tmp : captchacodes) {
                                if (!new Regex(url, "display:none[ ]*?\"").matches()) {
                                    captchaCode = new Regex(tmp, ">(.*?)<").getMatch(0).trim();
                                    break;
                                }
                            }
                        }
                    }
                }

                if (captchaurl == null && captchaCode == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                if (captchaCode == null) {
                    captchaFile = getLocalCaptchaFile(this);
                    Browser cap_br = br.cloneBrowser();
                    HTTPConnection captcha_con = cap_br.openGetConnection(captchaurl);
                    if (captcha_con.getContentType().contains("text")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
                    Browser.download(captchaFile, captcha_con);
                    /* CaptchaCode holen */
                    captchaCode = Plugin.getCaptchaCode(captchaFile, this, downloadLink);
                }

                capform.setVariable(0, captchaCode);
                /* Überprüfen(Captcha,Password) */
                downloadurl = "http://odsiebie.com/pobierz/" + steplink + "?captcha=" + captchaCode;
                br.getPage(downloadurl);
                if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("html?err")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
            }
            /* DownloadLink suchen */
            steplink = br.getRegex("<a href=\"/download/(.*?)\"").getMatch(0);
            if (steplink == null) { throw new PluginException(LinkStatus.ERROR_RETRY); }
            downloadurl = "http://odsiebie.com/download/" + steplink;
            br.getPage(downloadurl);
            if (br.getRedirectLocation() == null || br.getRedirectLocation().contains("upload")) { throw new PluginException(LinkStatus.ERROR_RETRY); }
            downloadurl = br.getRedirectLocation();
            if (downloadurl == null) { throw new PluginException(LinkStatus.ERROR_RETRY); }
        }
        /*
         * Leerzeichen müssen durch %20 ersetzt werden!!!!!!!!, sonst werden sie
         * von new URL() abgeschnitten
         */
        downloadurl = downloadurl.replaceAll(" ", "%20");
        /* Datei herunterladen */
        dl = new RAFDownload(this, downloadLink, br.createGetRequest(downloadurl));
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
