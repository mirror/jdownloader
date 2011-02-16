//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import java.util.ArrayList;

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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "euroshare.eu" }, urls = { "http://(www\\.)?euroshare\\.(eu|sk)/file/\\d+/.+" }, flags = { 2 })
public class EuroShareEu extends PluginForHost {

    public EuroShareEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://euroshare.eu/premium-accounts");
    }

    @Override
    public String getAGBLink() {
        return "http://euroshare.eu/terms";
    }

    private static final String TOOMANYSIMULTANDOWNLOADS = "<p>Naraz je z jednej IP adresy možné sťahovať iba jeden súbor";

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("euroshare.sk", "euroshare.eu"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!downloadLink.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return downloadLink.getAvailableStatus();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(TOOMANYSIMULTANDOWNLOADS)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
        br.setFollowRedirects(false);
        String dllink = br.getRegex("iba jeden súbor\\.<a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://euroshare\\.eu/download/\\d+/.*?/\\d+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(TOOMANYSIMULTANDOWNLOADS)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.postPage("http://euroshare.eu/login", "login=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
        // There are no cookies so we can only check via text on the website
        if (!br.containsHTML(">Ste úspešne prihlásený<") || br.containsHTML(">Nesprávne prihlasovacie meno alebo heslo")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("http://euroshare.eu/my-profile");
        ai.setUnlimitedTraffic();
        String expire = br.getRegex(">Premium účet do</td>[\t\n\r ]+<td width=\"70%\"><input type=\"text\" name=\"premium\" value=\"(.*?)\"").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd HH:mm:ss", null));
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
        String dllink = br.getRegex("\\'(http://euroshare\\.eu/download/\\d+/.*?)\\'").getMatch(0);
        if (dllink == null) dllink = br.getRegex("onClick=\"location\\.href=\\'(http://euroshare.eu/.*?)\\'\"").getMatch(0);
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

    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            br.setCustomCharset("utf-8");
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            StringBuilder sb = new StringBuilder();
            while (true) {
                sb.delete(0, sb.capacity());
                sb.append("data=");
                links.clear();
                while (true) {
                    /* we test 100 links at once */
                    if (index == urls.length || links.size() > 100) break;
                    links.add(urls[index]);
                    index++;
                }
                br.getPage("http://euroshare.eu/link-checker");
                int c = 0;
                for (DownloadLink dl : links) {
                    /*
                     * append fake filename, because api will not report
                     * anything else
                     */
                    if (c > 0) sb.append("%0D%0A");
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    c++;
                }
                br.postPage("http://euroshare.eu/link-checker", sb.toString());
                for (DownloadLink dl : links) {
                    String linkWithoutHttp = new Regex(dl.getDownloadURL(), "euroshare\\.eu(/file/\\d+/.+)").getMatch(0);
                    if (linkWithoutHttp == null) {
                        logger.warning("Euroshare.eu availablecheck is broken!");
                        return false;
                    }
                    String regexForThisLink = "(" + linkWithoutHttp + "</a><br /><div class=\"small\">(Súbor existuje \\| .*?|Súbor neexistuje)</div>)";
                    String theData = br.getRegex(regexForThisLink).getMatch(0);
                    if (theData == null) {
                        logger.warning("Euroshare.eu availablecheck is broken!");
                        return false;
                    }
                    String filename = new Regex(theData, "/file/\\d+/(.*?)(</a>)?<br />").getMatch(0);
                    String filesize = new Regex(theData, "class=\"small\">Súbor existuje \\| Veľkosť (.*?)</div>").getMatch(0);
                    if (!theData.contains("Súbor existuje") || theData.contains("Súbor neexistuje")) {
                        dl.setAvailable(false);
                        continue;
                    } else if (filename == null || filesize == null) {
                        logger.warning("Euroshare.eu availablecheck is broken!");
                        dl.setAvailable(false);
                        continue;
                    } else {
                        dl.setAvailable(true);
                    }
                    dl.setName(filename);
                    dl.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                if (index == urls.length) break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
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