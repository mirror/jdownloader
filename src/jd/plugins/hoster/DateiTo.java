//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import java.io.IOException;
import java.net.MalformedURLException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datei.to", "sharebase.to" }, urls = { "http://(www\\.)?(sharebase\\.(de|to)/(files/|1,)|datei\\.to/datei/)[\\w]+\\.html", "blablablaInvalid_regex" }, flags = { 2, 2 })
public class DateiTo extends PluginForHost {

    public DateiTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://datei.to/premium");
    }

    private static final String APIPAGE          = "http://api.datei.to/";
    private static final String FILEIDREGEX      = "datei\\.to/datei/(.*?)\\.html";
    private static final String DOWNLOADPOSTPAGE = "http://datei.to/ajax/download.php";

    @Override
    public String getAGBLink() {
        return "http://datei.to/agb";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws MalformedURLException {
        String id = new Regex(link.getDownloadURL(), "(/files/|/1,)([\\w]+\\.html)").getMatch(1);
        if (id != null) link.setUrlDownload("http://datei.to/datei/" + id);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.postPage(APIPAGE, "key=YYMHGBR9SFQA0ZWA&info=COMPLETE&datei=" + new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0));
        if (!br.containsHTML("online") || br.containsHTML("offline")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex info = br.getRegex(";(.*?);(\\d+);");
        downloadLink.setFinalFileName(info.getMatch(0));
        downloadLink.setDownloadSize(SizeFormatter.getSize(info.getMatch(1)));
        return AvailableStatus.TRUE;
    }

    public void login(Account account) throws IOException, PluginException {
        setBrowserExclusive();
        br.getPage("http://datei.to/apito/jd.php?u=" + Encoding.urlEncode(account.getUser()) + "&p=" + Encoding.urlEncode(account.getPass()));
        String[] info = Regex.getLines(br.toString());
        if (info.length != 3 || info[0].matches("FALSE") || !info[0].matches("PREMIUM")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String[] info = Regex.getLines(br.toString());
        ai.setValidUntil(Long.parseLong(info[1]) * 1000l);
        ai.setPremiumPoints(Long.parseLong(info[2]));
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        String id = new Regex(downloadLink.getDownloadURL(), "/files/([\\w]+\\.html)").getMatch(0);
        if (id != null) {
            br.getPage("http://datei.to/files/" + id + "," + Encoding.urlEncode(account.getUser() + "," + Encoding.urlEncode(account.getPass())));
        } else {
            id = new Regex(downloadLink.getDownloadURL(), "/1,([\\w]+\\.html)").getMatch(0);
            if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage("http://datei.to/1," + id + "," + Encoding.urlEncode(account.getUser() + "," + Encoding.urlEncode(account.getPass())));
        }
        String dlUrl = br.getRedirectLocation();
        if (dlUrl == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlUrl, true, 0);
        br.setFollowRedirects(true);
        if (dl.getConnection() == null || dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.severe("PremiumError: " + br.toString());
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage(DOWNLOADPOSTPAGE, "P=I&ID=" + new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0));
        if (br.containsHTML("(Aktuell lädst du bereits eine Datei herunter|Als Free-User kannst du nur 1 Datei gleichzeitig downloaden.)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.dateito.toomanysimultandownloads", "Too many simultan downloads"), 10 * 60 * 1000l);
        String reconnectWaittime = br.getRegex("Du musst <span style=\"font-weight:bold; color:#DD0000;\">(\\d+) Minuten</span>").getMatch(0);
        if (reconnectWaittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWaittime) * 60 * 1001l);
        String postId = br.getRegex("data: \"P=III\\&ID=(.*?)\"").getMatch(0);
        if (postId == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        int wait = 30;
        String waittimeRegexed = br.getRegex("id=\"CDD\">(\\d+)</span> Sekunden").getMatch(0);
        if (waittimeRegexed != null) wait = Integer.parseInt(waittimeRegexed);
        sleep(wait * 1001l, downloadLink);
        // Can be skipped
        // br.postPage(DOWNLOADPOSTPAGE, "P=III&ID=" + postId);
        br.postPage(DOWNLOADPOSTPAGE, "P=IV&ID=" + postId);
        if (br.containsHTML("(Bitte versuche es in ein paar Minuten erneut|>Dies kann verschiedene Ursachen haben)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        String dllink = br.toString();
        if (!dllink.startsWith("http") || dllink.length() > 500) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.trim();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection() == null || dl.getConnection().getContentType().contains("html")) {
            logger.warning("The dllink doesn't seem to be a file...");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
