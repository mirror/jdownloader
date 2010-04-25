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

import java.io.IOException;
import java.net.MalformedURLException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharebase.to" }, urls = { "http://[\\w\\.]*?sharebase\\.(de|to)/(files/|1,)[\\w]+\\.html" }, flags = { 2 })
public class ShareBaseTo extends PluginForHost {

    public ShareBaseTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://sharebase.to/premium/");
    }

    @Override
    public String getAGBLink() {
        return "http://sharebase.to/terms/";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws MalformedURLException {
        /* damit neue links mit .de als .to in die liste kommen */
        link.setUrlDownload(link.getDownloadURL().replaceAll("sharebase\\.de", "sharebase\\.to"));
        String id = new Regex(link.getDownloadURL(), "/files/([\\w]+\\.html)").getMatch(0);
        if (id != null) link.setUrlDownload("http://sharebase.to/1," + id);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.getPage("http://sharebase.to/apito/jd.php?f=" + downloadLink.getDownloadURL());
        String[] info = Regex.getLines(br.toString());
        if (info.length < 3 || info[0].matches("NONE")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setFinalFileName(info[0]);
        downloadLink.setDownloadSize(Regex.getSize(info[1]));
        if (info[2].matches("OFFLINE")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (!info[2].matches("ONLINE")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        return AvailableStatus.TRUE;
    }

    public void login(Account account) throws IOException, PluginException {
        setBrowserExclusive();
        br.getPage("http://sharebase.to/apito/jd.php?u=" + Encoding.urlEncode(account.getUser()) + "&p=" + Encoding.urlEncode(account.getPass()));
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
            br.getPage("http://sharebase.to/files/" + id + "," + Encoding.urlEncode(account.getUser() + "," + Encoding.urlEncode(account.getPass())));
        } else {
            id = new Regex(downloadLink.getDownloadURL(), "/1,([\\w]+\\.html)").getMatch(0);
            if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage("http://sharebase.to/1," + id + "," + Encoding.urlEncode(account.getUser() + "," + Encoding.urlEncode(account.getPass())));
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
        /* für links welche noch mit .de in der liste stehen */
        String url = downloadLink.getDownloadURL();
        br.getPage(url);
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        br.postPage(br.getURL(), "lang=de&submit.x=8&submit.y=9");
        br.postPage(br.getURL(), "free=Free");
        Form form = br.getFormBySubmitvalue("Please+Activate+Javascript");
        String id = form.getVarsMap().get("asi");
        form.put(id, Encoding.urlEncode("Download Now !"));
        br.submitForm(form);

        if (br.containsHTML("Von deinem Computer ist noch ein Download aktiv.")) {
            logger.severe("ShareBaseTo Error: Too many downloads");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 1000l);
        } else if (br.containsHTML("werden derzeit Wartungsarbeiten vorgenommen")) {
            logger.severe("ShareBaseTo Error: Maintenance");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.sharebaseto.errors.maintenance", "Maintenance works in progress"), 30 * 60 * 1000l);
        } else if (br.containsHTML("Sorry, es laden derzeit")) {
            logger.severe("ShareBaseTo Error: Too many Users");
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.sharebaseto.errors.toomanyusers", "Too many users"), 5 * 60 * 1000l);
        }
        String[] wait = br.getRegex("Du musst noch <strong>(\\d*?)min (\\d*?)sec</strong> warten").getRow(0);
        if (wait != null) {
            long waitTime = (Integer.parseInt(wait[0]) * 60 + Integer.parseInt(wait[1])) * 1000l;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitTime);
        }
        br.setFollowRedirects(true);
        if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), false, 1);
        if (dl.getConnection() == null || dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Gegen Netz-Zensur")) throw new PluginException(LinkStatus.ERROR_FATAL, "Service/Link not available (at the moment)");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
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
