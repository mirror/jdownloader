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

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "odsiebie.com" }, urls = { "http://[\\w\\.]*?odsiebie\\.com/(pokaz|pobierz)/\\d+---[\\w]+" }, flags = { 2 })
public class Odsiebiecom extends PluginForHost {

    public Odsiebiecom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return "http://odsiebie.com/tresc/faq.html";
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage("http://odsiebie.com/logowanie.html");
        br.postPage("http://odsiebie.com/logowanie.html?login", "luser=" + Encoding.urlEncode(account.getUser()) + "&lpass=" + Encoding.urlEncode(account.getPass()) + "&sub=Zaloguj+mnie");
        if (br.getCookie("http://odsiebie.com/", "gb_col") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (br.getCookie("http://odsiebie.com/", "gg_info") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Free Membership");
        account.setValid(true);
        return ai;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex("Nazwa\\s+pliku:</dt>\\s+<dd[^>]*?>(.*?)dd>").getMatch(0);
        String filesize = br.getRegex("<dt>Rozmiar pliku:</dt>.*?<dd>(.*?)</dd>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filename = filename.replaceAll("<!--.*?-->", " ");
        filename = new Regex(filename, "[\\s*?]*(.*?)</").getMatch(0);
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "")));
        downloadLink.setName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return getMaxSimultanDownloadNum();
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        /* Nochmals das File überprüfen */
        String finalfn = downloadLink.getName();
        requestFileInformation(downloadLink);
        if (account != null) {
            login(account);
            br.getPage(downloadLink.getDownloadURL());
        }
        String downloadurl;
        /*
         * Zuerst schaun ob wir nen Button haben oder direkt das File vorhanden
         * ist
         */
        String steplink = br.getRegex("class=\"pob..\"\\s+href=\"/pobierz/(.*?)\">").getMatch(0);
        if (steplink == null) {
            /* Kein Button, also muss der Link irgendwo auf der Page sein */
            /* Film,Mp3 */
            downloadurl = br.getRegex("flashvars=\"url=(.*?)&v_autostart").getMatch(0);
            /* Flash */
            if (downloadurl == null) {
                downloadurl = br.getRegex("<PARAM NAME=\"movie\" VALUE=\"(.*?)\"").getMatch(0);
            }
            /* Bilder, Animationen */
            if (downloadurl == null) {
                downloadurl = br.getRegex("onLoad=\"scaleImg\\('thepic'\\)\" src=\"(.*?)\" \\/").getMatch(0);
            }
            /* kein Link gefunden */
            if (downloadurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        } else {
            /* Button folgen, schaun ob Link oder Captcha als nächstes kommt */
            downloadurl = "http://odsiebie.com/pobierz/" + steplink;
            br.getPage(downloadurl);
            downloadurl = br.getRegex("href=\"/download/(.*?)\"").getMatch(0);
            Form capform = br.getFormbyProperty("name", "wer1");
            if (capform == null) {
                String form2search = br.getRegex("from picture\\)</small>.*?<form name=\"(.*?)\" method").getMatch(0);
                if (form2search != null) {
                    capform = br.getFormbyProperty("name", form2search);
                }
            }
            if (downloadurl == null && capform != null) {
                int i = 0;
                Browser brc = br.cloneBrowser();
                while (capform != null) {
                    String pagepiece = br.getRegex("<img src.*?\"(.*?)</form>").getMatch(0);
                    if (pagepiece == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
                    String[] captchalinks = HTMLParser.getHttpLinks(pagepiece, "");
                    if (captchalinks == null || captchalinks.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
                    String adr = null;
                    File file = null;
                    for (String link : captchalinks) {
                        URLConnectionAdapter con = brc.openGetConnection(link);
                        if ((con.getContentType().contains("image"))) {
                            adr = link;
                            file = this.getLocalCaptchaFile();
                            Browser.download(file, con);
                            break;
                        }
                        con.disconnect();
                        continue;
                    }
                    // String adr =
                    // br.getRegex("<img src=\"http://odsiebie.com/v_auth.php\" style=\"display: none;\"><img src=\"(.*?)\"  style=\"display:").getMatch(0);
                    // adr = "http://odsiebie.com/v.php";
                    if (adr == null || file == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
                    // URLConnectionAdapter con = brc.openGetConnection(adr);
                    // File file = this.getLocalCaptchaFile();
                    // Browser.download(file, con);
                    String code = getCaptchaCode(file, downloadLink);
                    capform.getInputFieldByName("captcha").setValue(code);
                    br.submitForm(capform);
                    capform = br.getFormbyProperty("name", "wer1");
                    i++;
                    if (i > 3) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
                }
                if (capform != null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            }
            br.setFollowRedirects(false);
            /* DownloadLink suchen */
            steplink = br.getRegex("href=\"/download/(.*?)\"").getMatch(0);
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
        downloadLink.setDownloadSize(0);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadurl, false, 1);
        if (dl.getConnection().getContentType().contains("text")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.odsiebiecom.errors.servererror", "Server error"));
        }
        downloadLink.setFinalFileName(finalfn);
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        this.handlePremium(downloadLink, null);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return getMaxSimultanDownloadNum();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
