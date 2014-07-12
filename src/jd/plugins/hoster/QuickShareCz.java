//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.Locale;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "quickshare.cz" }, urls = { "http://[\\w\\.]*?quickshare\\.cz/stahnout-soubor/\\d+:[^\\s]+" }, flags = { 2 })
public class QuickShareCz extends PluginForHost {

    public QuickShareCz(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(5000l);
        this.enablePremium("http://www.quickshare.cz/premium");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("/uzivatele/profil");
        String trafficleft = br.getRegex("Kredit:\\s*<a href=\"/platby/cenik\">(.*?)</a>").getMatch(0);
        if (trafficleft != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(trafficleft.replace(",", ".")));
        }
        final String expire = br.getRegex("<th>Datum registrace</th>\\s*<td>(\\d{2}\\.\\d{2}\\.\\d{4})</td>").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MM.dd.yyyy", Locale.ENGLISH));
            ai.setStatus("Premium User");
            account.setProperty("free", false);
        } else {
            ai.setStatus("Free User");
            account.setProperty("free", true);
            // not support as of yet...
            account.setEnabled(false);
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.quickshare.cz/podminky-pouziti";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String wait = br.getRegex("<strong id=\"freeDown-timeout\">(\\d+)</strong>").getMatch(0);
        if (wait != null) {
            // doesn't seem to be required... though will help with this IP is already downloading.
            sleep(Long.parseLong(wait) * 1001, downloadLink);
        }
        final String ddlink = br.getRegex("(https?:[^\"]+quickshare\\.cz[^\"]+free\\.php\\?fid=\\d+)").getMatch(0);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, ddlink.replaceAll("\\\\/", "/"), true, 1);
        if (dl.getConnection().getContentType().contains("text")) {
            br.followConnection();
            if (br.containsHTML("403 Forbidden<br><br>Z Vasi IP adresy jiz probiha stahovani. Jako free uzivatel muzete stahovat pouze jeden soubor\\.")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "This IP Address is already downloading", 2 * 60 * 1000);
            }
        }
        dl.startDownload();
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        br.getPage(parameter.getDownloadURL());
        final String ddlink = br.getRegex("(https?:[^\"]+quickshare\\.cz[^\"]+premium\\.php\\?[^\"]+)").getMatch(0);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, HTMLEntities.unhtmlentities(ddlink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void login(Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage("http://www.quickshare.cz/");
        br.postPage("http://www.quickshare.cz/?do=prihlaseni-submit", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=on&send=P%C5%99ihl%C3%A1sit");
        if (br.getRedirectLocation() == null || !br.getRedirectLocation().contains("/?_fid=")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Takov. soubor neexistuje")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>(.*?) \\| QuickShare\\.cz</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<div class=\"detail\">\\s*<h1>(.*?)</h1>").getMatch(0);
        }
        String filesize = br.getRegex("<em><i class=\"fa fa-video-camera\"></i>([\\d\\,]+ [A-Z]+)</em>").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        downloadLink.setName(filename.trim());
        if (filesize != null) {
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", ".")));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}