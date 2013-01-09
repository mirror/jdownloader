package jd.plugins.hoster;

//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fsx.hu" }, urls = { "http://((www\\.)?(fsx|mfs\\.hu/download\\.php\\?s=\\d+\\&d=[^<>\"]+\\&h=[a-z0-9]+|s.*?\\.(fsx|mfs)\\.hu/.+/.+))" }, flags = { 2 })
public class FsxHu extends PluginForHost {

    public FsxHu(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://fsx.hu/index.php?o=dijcsomagok");
    }

    private static final String REGISTEREDONLY = JDL.L("plugins.hoster.fsxhu.errors.onlyregistered", "Only registered users can download this file");

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("fsx.hu/", "mfs.hu/"));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("kovetkezo idopontig ervenyes: (\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd HH:mm:ss", null));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.fsx.hu/index.php?m=home&o=szabalyzat";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        // Max 2 connections at all
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        requestFileInformation(downloadLink);
        handleFree0(downloadLink);
    }

    public void handleFree0(final DownloadLink downloadLink) throws Exception {
        String captcha = br.getRegex("/?(kep(\\d+)?\\.php)").getMatch(0);
        if (captcha == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        ArrayList<String> gifs = new ArrayList<String>();
        final String gif[] = br.getRegex("img/(.*?)\\.gif").getColumn(-1);
        if (gif != null && gif.length != 0) {
            for (final String template : gif) {
                if (!gifs.contains(template)) gifs.add(template);
            }
            for (final String template : gifs) {
                URLConnectionAdapter con = null;
                try {
                    con = br.cloneBrowser().openGetConnection(template);
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        br.getPage("/download.php?i=1");
        for (int i = 0; i <= 3; i++) {
            final String code = getCaptchaCode("/" + captcha, downloadLink);
            br.postPage("/download.php?i=1", "capcha=" + code);
            if (br.containsHTML("download\\.php")) break;
            if (br.containsHTML("/?(kep(\\d+)?\\.php)")) continue;
            break;
        }
        if (br.containsHTML("/?(kep(\\d+)?\\.php)") && !br.containsHTML("download\\.php")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (br.containsHTML("(>Az FSX szerverekről 24 óra alatt maximum|>Ingyenesen ekkor tölthetsz le legközelebb)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        for (int i = 0; i <= 50; i++) {
            String place = br.getRegex("<span style=\"color:#dd0000;font\\-weight:bold;\">(\\d+)</span>").getMatch(0);
            if (place != null) {
                downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.fsxhu.waiting", "Waiting for link, current place: " + place));
                sleep(16 * 1000l, downloadLink);
                br.getPage("/download.php");
            } else {
                break;
            }
        }
        String url = br.getRegex("<div class=\"gombbefoglalo\" style=\"text\\-align:center;float:left;\">[\t\n\r ]+<a [^>]+? href=\"(http://.*?)\"").getMatch(0);
        if (url == null) url = br.getRegex("\"(http://s\\d+\\.(fsx|mfs)\\.hu/[a-z0-9]+/\\d+/[^\"<>]+)\"").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = BrowserAdapter.openDownload(br, downloadLink, url, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            /**
             * In case user started a download via browser and tries to start another via JDownloader
             */
            if (dl.getConnection().getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 5 * 60 * 1000l);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(final Account account) throws Exception {
        setBrowserExclusive();
        br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
        br.getHeaders().put("Accept", "text/html, */*");
        br.getHeaders().put("Content-Type", "text/html");
        br.getHeaders().put("User-Agent", "FSX letöltésvezérlo v1.1.0.3");
        br.getHeaders().put("Accept-Language", null);
        br.getHeaders().put("Accept-Encoding", null);
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Cache-Control", null);
        br.getHeaders().put("Pragma", null);
        br.getPage("http://fsx.hu/testaccount.php?un=" + Encoding.urlEncode(account.getUser()) + "&pw=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("A megadott jelszo nem megfelelo") || !br.containsHTML("A megadott hozzaferes aktiv, mely a kovetkezo idopontig ervenyes:")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies("www.fsx.hu");
        br.clearCookies("www.mfs.hu");
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(>A kiválasztott fájl nem található|>vagy eltávolításra került)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        br.getPage("/download.php?i=1");
        final String filename = br.getRegex("<h1 style=\"padding\\-bottom:0;font\\-size:16px;\">(.+?)</h1>").getMatch(0);
        final String filesize = br.getRegex("Méret: (\\d+) Bájt ?<br").getMatch(0);
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim());
        if (filesize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}