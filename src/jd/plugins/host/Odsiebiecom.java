//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
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
            /* Button folgen, schaun ob Link oder Captcha als nächstes kommt */
            downloadurl = "http://odsiebie.com/pobierz/" + steplink;
            br.getPage(downloadurl);
            if (br.getRedirectLocation() != null) {
                /* Weiterleitung auf andere Seite, evtl mit Captcha */
                downloadurl = br.getRedirectLocation();
                br.getPage(downloadurl);
            }
            Form capform = br.getFormbyProperty("name","wer");
            if (capform != null) {
                /* Captcha File holen */
                BufferedImage image = getCSSCaptchaImage(capform.getHtmlCode());

                captchaFile = getLocalCaptchaFile(this);
                captchaFile.mkdirs();
                ImageIO.write(image, "png", captchaFile);

                /* CaptchaCode holen */
                captchaCode = Plugin.getCaptchaCode(this, "CCSCaptchaOdsiebie", captchaFile, false, downloadLink);

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
        br.setFollowRedirects(true);
        br.setDebug(true);
        dl = br.openDownload(downloadLink, downloadurl, false, 1);
        if (dl.getConnection().getContentType().contains("text")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FATAL, "Server Error");
        }
        dl.startDownload();
    }

    private BufferedImage getCSSCaptchaImage(String htmlCode) {
        String[] letters = htmlCode.split("<div.*?class=");

        int[][] grid = null;
        Color[] cols = new Color[] { Color.BLUE.darker(), Color.GREEN.darker(), Color.MAGENTA.darker(), Color.DARK_GRAY, Color.RED.darker(), Color.RED, Color.RED };
        String comp = null;
        for (int i = 0; i < letters.length; i++) {
            String[] lines = new Regex(letters[i], "<div.*?</div").getColumn(-1);
            for (int y = 0; y < lines.length; y++) {
                String[] pixel = new Regex(lines[y], "<span.*?</span").getColumn(-1);
                if (grid == null) grid = new int[(letters.length - 1) * (pixel.length + 10)][lines.length];
                int offset = (i - 1) * (pixel.length + 5);
                for (int x = 0; x < pixel.length; x++) {
                    if (comp == null) comp = pixel[x];
                    grid[x + offset][y] = pixel[x].length() > 30 ? cols[i].getRGB() : 0xffffff;
                }

            }

        }
        int width = grid.length;
        int height = grid[0].length;
        int faktor = 4;
        BufferedImage image = new BufferedImage(width * faktor, height * faktor, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(8));
        g2.setBackground(Color.WHITE);
        g2.clearRect(0, 0, width, height);
        g2.setColor(Color.BLACK);

        for (int y = 0; y < height * faktor; y += faktor) {
            for (int x = 0; x < width * faktor; x += faktor) {
                int col = grid[x / faktor][y / faktor];
                if (col == 0) col = 0xffffff;
                g2.setColor(new Color(col));
                g2.fillRect(x, y, faktor, faktor);
            }
        }
        return image;
    }

    @Override
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
