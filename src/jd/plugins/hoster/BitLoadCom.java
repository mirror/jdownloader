//    jDownloader - Downloadmanager
//    Copyright (C) 2010  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bitload.com", "mystream.to" }, urls = { "http://(www\\.)?(bitload\\.com/(f|d)/\\d+/[a-z0-9]+|mystream\\.to/file-\\d+-[a-z0-9]+)", "http://blablarfdghrtthgrt56z3ef27893bv" }, flags = { 2, 0 })
public class BitLoadCom extends PluginForHost {

    private static String agent = RandomUserAgent.generate();

    public BitLoadCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.bitload.com/premium");
    }

    private static final String MAINPAGE = "http://www.bitload.com/";

    @Override
    public String getAGBLink() {
        return "http://www.bitload.com/imprint";
    }

    public void correctDownloadLink(DownloadLink link) {
        if (link.getDownloadURL().contains("mystream.to/")) {
            Regex mystreamIDs = new Regex(link.getDownloadURL(), "mystream\\.to/file-(\\d+)-([a-z0-9]+)");
            link.setUrlDownload("http://www.bitload.com/f/" + mystreamIDs.getMatch(0) + "/" + mystreamIDs.getMatch(1));
        } else {
            link.setUrlDownload(link.getDownloadURL().replace("/d/", "/f/"));
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", agent);
        br.setCookie("http://www.bitload.com", "locale", "de");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Datei nicht gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex nameAndSize = br.getRegex("Ihre Datei <strong>(.*?) \\((\\d+,\\d+ .*?)\\)</strong> wird angefordert");
        String filename = nameAndSize.getMatch(0);
        if (filename == null) filename = br.getRegex("Sie möchten <strong>(.*?)</strong> schauen <br/>").getMatch(0);
        if (filename == null) filename = br.getRegex("Sie haben folgende Datei angefordert.*?>(.*?)</").getMatch(0);
        String filesize = nameAndSize.getMatch(1);
        if (filesize == null) filesize = br.getRegex("x\">Divx</strong> \\((.*?)\\)<br/><br/>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        // Streamlinks show no filesize
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL() + "?c=free");
        if (br.containsHTML(">Datei nicht gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String dllink = getDllink();
        if (dllink == null) {
            logger.warning("The dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("Got html code instead of the file!");
            br.followConnection();
            if (br.containsHTML(">Datei nicht gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getDllink() {
        String dllink = br.getRegex("bis die Datei bereitgestellt wird!</div>[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(http://ms\\d+\\.mystream\\.to/file-\\d+/[A-Za-z0-9]+/.*?)\"").getMatch(0);
            if (dllink == null) {
                // For Streamlinks
                dllink = br.getRegex("var url = \\'(http://.*?)\\'").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\\'(http://ms\\d+\\.mystream\\.to/file-\\d+/[A-Za-z0-9]+/.*?)\\'").getMatch(0);
                }
            }
        }
        return dllink;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", agent);
        br.setCookie("http://www.bitload.com", "locale", "de");
        br.postPage(MAINPAGE + "login", "sUsername=" + Encoding.urlEncode(account.getUser()) + "&sPassword=" + Encoding.urlEncode(account.getPass()) + "&login_submit=");
        if (br.getCookie(MAINPAGE, "hash") == null || br.getCookie(MAINPAGE, "username") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage(MAINPAGE + "usercp?overview");
        String space = br.getRegex("<td>Belegter Speicherplatz:</td><td>(.*?)</td></tr>").getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim().replace(",", "."));
        account.setValid(true);
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("\">Premium gültig bis <strong>(.*?)</strong>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy | hh:mm", null));
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        br.getPage(link.getDownloadURL() + "?c=premium");
        if (br.containsHTML(">Datei nicht gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String dllink = br.getRegex("bis die Datei bereitgestellt wird\\!</div>[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(http://bl\\d+\\.bitload\\.com/file-\\d+/[A-Za-z0-9]+/.*?)\"").getMatch(0);
            if (dllink == null) dllink = getDllink();
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}