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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import jd.PluginWrapper;
import jd.captcha.specials.icaptcha.ICaptcha;
import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

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
            br.setDebug(true);
            /* Button folgen, schaun ob Link oder Captcha als nächstes kommt */
            downloadurl = "http://odsiebie.com/pobierz/" + steplink;
            br.getPage(downloadurl);
            if (br.getRedirectLocation() != null) {
                /* Weiterleitung auf andere Seite, evtl mit Captcha */
                downloadurl = br.getRedirectLocation();
                br.getPage(downloadurl);
            }
           
            if (br.containsHTML("http://odsiebie.com/icaptcha.swf")) {
                br.getPage("http://odsiebie.com/icaptcha.swf");
                String v =   br.getPage("http://odsiebie.com/weryfikacja/icaptcha.php").substring(2);
                
             
                BufferedImage image = ICaptcha.paintImage(v, 250, 100);
                /* Captcha File holen */
         
              if(image==null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
              File file = getLocalCaptchaFile(this);
              file.mkdirs();
              ImageIO.write(image, "png", file);
              String code = getCaptchaCode(file, this, downloadLink);
              
              br.getPage(downloadurl.replace(".html","")+"?code="+code);
                
             
                if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("html?err")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
           
            if(br.getRedirectLocation() != null){
                br.setFollowRedirects(true);
                br.getPage(br.getRedirectLocation());
                br.setFollowRedirects(false);
            }
            
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
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, downloadurl, false, 1);
        if (dl.getConnection().getContentType().contains("text")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FATAL, "Server Error");
        }
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
