//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.UserAgents;
import jd.plugins.decrypter.RomHustlerCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { RomHustlerCrawler.class })
public class RomHustler extends PluginForHost {
    public RomHustler(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.romhustler.org/user/sign-up");
    }

    public static List<String[]> getPluginDomains() {
        return RomHustlerCrawler.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            // 2023-05-17: Plugin has no RegEx anymore as URLs get pushed in via crawler plugin.
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://romhustler.org/disclaimer";
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        if (agent.get() == null) {
            agent.set(UserAgents.stringUserAgent());
        }
        br.getHeaders().put("User-Agent", agent.get());
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String contentID = getInternalContentID(link);
        if (contentID != null) {
            return this.getHost() + "://" + this.getPartPosition(link);
        } else {
            return super.getLinkID(link);
        }
    }

    private static AtomicReference<String> agent                       = new AtomicReference<String>();
    public static final String             PROPERTY_MAIN_CONTENT_URL   = "decrypterLink";
    public static final String             PROPERTY_PARTS_OVERVIEW_URL = "part_overview_url";
    public static final String             PROPERTY_DIRECTURL          = "directurl";
    public static final String             PROPERTY_POSITION           = "position";
    public static final String             PROPERTY_NUMBEROF_PARTS     = "numberof_parts";

    /** 2022-10-31: This website is similar to romulation.org? */
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException, IOException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String mainContentLink = getMainContentURL(link);
        if (mainContentLink == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(mainContentLink);
        if (RomHustlerCrawler.isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    private String getInternalContentID(final DownloadLink link) {
        final String partsOverviewURL = getPartsDownloadOverviewURL(link);
        if (partsOverviewURL != null) {
            return new Regex(partsOverviewURL, "/download/guest/(\\d+)").getMatch(0);
        } else {
            return null;
        }
    }

    private String getMainContentURL(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_MAIN_CONTENT_URL);
    }

    private String getPartsDownloadOverviewURL(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_PARTS_OVERVIEW_URL);
    }

    private boolean isLoggedIn(final Browser br) {
        if (br.containsHTML("/user/logout")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        return RomulationOrg.fetchAccountInfo(this, br, account);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        if (account != null) {
            RomulationOrg.login(this, br, account, false);
        }
        final String mainContentLink = getMainContentURL(link);
        if (mainContentLink == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final RomHustlerCrawler crawler = (RomHustlerCrawler) this.getNewPluginForDecryptInstance(this.getHost());
        final ArrayList<DownloadLink> crawledParts = crawler.decryptIt(new CryptedLink(mainContentLink), null);
        if (crawledParts == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        DownloadLink matchingPart = null;
        for (final DownloadLink crawledPart : crawledParts) {
            if (StringUtils.equals(this.getLinkID(link), this.getLinkID(crawledPart))) {
                matchingPart = crawledPart;
                break;
            }
        }
        if (matchingPart == null && !link.hasProperty(PROPERTY_POSITION)) {
            /* Backward compatibility for items added before- and until revision 47763. */
            matchingPart = crawledParts.get(0);
        }
        /* Check for errors first. */
        if (br.containsHTML("(?i)>\\s*File too big for guests")) {
            throw new AccountRequiredException();
        } else if (br.containsHTML("class=\"alert alert-danger restricted-button\"")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Sorry, this game is restricted.");
        }
        final String otherErrormessage = br.getRegex("<ul class=\"list-unstyled\">\\s*<li>([^<]+)</li>").getMatch(0);
        if (!StringUtils.isEmpty(otherErrormessage)) {
            /* E.g. "Sorry, this game is restricted." */
            throw new PluginException(LinkStatus.ERROR_FATAL, otherErrormessage);
        }
        if (matchingPart == null) {
            /* This should never happen. */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        boolean skipWaittime = true;
        if (!skipWaittime) {
            int wait = 8;
            final String waittime = br.getRegex("start=\"(\\d+)\"></span>").getMatch(0);
            if (waittime != null) {
                wait = Integer.parseInt(waittime);
            }
            sleep(wait * 1001l, link);
        }
        final String directurl = matchingPart.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(directurl) || !directurl.startsWith("http") || directurl.length() > 500) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, true, account != null && AccountType.PREMIUM.equals(account.getType()) ? -10 : -4);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Error 503 too many connections", 2 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final String filename = getFileNameFromHeader(dl.getConnection());
        if (filename != null) {
            link.setFinalFileName(Encoding.htmlDecode(filename));
        }
        dl.startDownload();
    }

    private int getPartPosition(final DownloadLink link) {
        return link.getIntegerProperty(PROPERTY_POSITION, 0);
    }

    @Deprecated
    private boolean isSplitPartInPosHigherThanZero(final DownloadLink link) {
        if (link.getBooleanProperty("splitlink", false)) {
            /* Legacy compatibility */
            return true;
        } else {
            final int position = link.getIntegerProperty(PROPERTY_POSITION, -1);
            if (position > 0) {
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public void resetPluginGlobals() {
    }
}