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
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "euroshare.eu" }, urls = { "http://(www\\.)?euroshare\\.(eu|sk)/file/\\d+/[^<>\"/]+" }, flags = { 2 })
public class EuroShareEu extends PluginForHost {

    private static final String  TOOMANYSIMULTANDOWNLOADS = "<p>Naraz je z jednej IP adresy možné sťahovať iba jeden súbor";
    private static final Object  LOCK                     = new Object();
    private static AtomicInteger maxPrem                  = new AtomicInteger(1);

    public EuroShareEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://euroshare.eu/premium-accounts");
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
                sb.append("links=");
                links.clear();
                while (true) {
                    /* we test 100 links at once */
                    if (index == urls.length || links.size() > 100) break;
                    links.add(urls[index]);
                    index++;
                }
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
                    Regex regexForThisLink = br.getRegex("\"(http://(www\\.)?euroshare\\.eu" + linkWithoutHttp + "\">http://(www\\.)?euroshare\\.sk" + linkWithoutHttp + "</a></td>[\t\n\r ]+<td class=\"velikost\"><img src=\"images/ok\\.png\")");
                    String theData = regexForThisLink.getMatch(0);
                    if (theData == null) theData = br.getRegex("(\">http://(www\\.)?euroshare\\.eu" + linkWithoutHttp + "</td>[\t\n\r ]+<td class=\"velikost\"><img src=\"images/zrusit-nahravani\\.jpg\")").getMatch(0);
                    if (theData == null) {
                        logger.warning("Euroshare.eu availablecheck is broken!");
                        return false;
                    }
                    String filename = new Regex(theData, "/file/\\d+/(.*?)\"").getMatch(0);
                    if (!theData.contains("images/ok.png") || theData.contains("zrusit-nahravani.jpg")) {
                        dl.setAvailable(false);
                        continue;
                    } else if (filename == null) {
                        logger.warning("Euroshare.eu availablecheck is broken!");
                        dl.setAvailable(false);
                        continue;
                    } else {
                        dl.setAvailable(true);
                    }
                    dl.setName(filename);
                }
                if (index == urls.length) break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("euroshare.sk", "euroshare.eu"));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }

        ai.setUnlimitedTraffic();
        account.setValid(true);
        String expire = br.getRegex(">Prémium účet máte aktívny do: ([^<>\"]*?)</a>").getMatch(0);
        if (expire == null) {
            ai.setStatus("Free (registered) User");
            try {
                maxPrem.set(-1);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
            }
            account.setProperty("FREE", true);
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy", null) + (1000l * 60 * 60 * 24));
            ai.setStatus("Premium User");
            try {
                maxPrem.set(-1);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
            }
            account.setProperty("FREE", false);
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://euroshare.eu/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxPrem.get();
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(TOOMANYSIMULTANDOWNLOADS)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
        if (br.containsHTML("(>Všetky sloty pre Free užívateľov sú obsadené|Skúste prosím neskôr\\.<br)")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.euroshareeu.nofreeslots", "No free slots available"), 5 * 60 * 1000l);
        // br.setFollowRedirects(false);
        // String dllink =
        // br.getRegex("iba jeden súbor\\.<a href=\"(http://.*?)\"").getMatch(0);
        // if (dllink == null) dllink =
        // br.getRegex("\"(http://euroshare\\.eu/download/\\d+/.*?/\\d+/.*?)\"").getMatch(0);
        // if (dllink == null) throw new
        // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL() + "/download/", false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(TOOMANYSIMULTANDOWNLOADS)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        if (account.getBooleanProperty("FREE")) {
            doFree(link);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL() + "/download/", true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage("http://euroshare.eu/customer-zone/login/");
        br.postPage("http://euroshare.eu/customer-zone/login/", "trvale=1&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        // There are no cookies so we can only check via text on the website
        if (!br.containsHTML("title=\"Môj profil\"")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (br.containsHTML(">Nesprávne prihlasovacie meno alebo heslo")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}