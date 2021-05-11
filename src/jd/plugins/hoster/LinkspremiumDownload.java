//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.Arrays;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.YetiShareCore;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linkspremium.download" }, urls = { "" })
public class LinkspremiumDownload extends YetiShareCore {
    public LinkspremiumDownload(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("http://linkspremium.download/");
        this.enablePremium(this.getPurchasePremiumURL());
    }

    @Override
    public String getAGBLink() {
        return "http://linkspremium.download/";
    }

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "linkspremium.download" };
    }

    private static MultiHosterManagement mhm                = new MultiHosterManagement("linkspremium.download");
    private static final String          PROPERTY_DIRECTURL = "linkspremiumdownloaddirectlink";

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2021-05-11: null<br />
     * other: Speciel special: YetiShare used as a multihost! <br />
     */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return true;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 0;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo ai = super.fetchAccountInfoWebsite(account);
        if (account.getType() == AccountType.FREE) {
            ai.setTrafficLeft(0);
        } else {
            this.getPage("/filehostdl.html");
            /* Try to get/set traffic information */
            final Regex trafficInfo = br.getRegex("span class=\"badge badge-success\">(\\d+)\\s*/\\s*(\\d+(?:,\\d+)?) MB</span>");
            final String trafficUsedToday = trafficInfo.getMatch(0);
            if (trafficUsedToday != null) {
                final String trafficMaxToday = trafficInfo.getMatch(1);
                ai.setTrafficMax(SizeFormatter.getSize(trafficMaxToday.replace(",", "") + "MB"));
                ai.setTrafficLeft(ai.getTrafficMax() - SizeFormatter.getSize(trafficUsedToday + "MB"));
            }
            /* Double-check */
            final Regex linksleftInfo = br.getRegex("class=\"badge badge-success\">(\\d+(?:,\\d+)?)\\s*/\\s*(\\d+(?:,\\d+)?)\\s*Links</span>");
            final String linksleftUsedToday = linksleftInfo.getMatch(0);
            final String linksleftMaxToday = linksleftInfo.getMatch(1);
            if (linksleftUsedToday != null) {
                final int linksleftUsedTodayInt = Integer.parseInt(linksleftUsedToday.replace(",", ""));
                final int linksleftMaxTodayInt = Integer.parseInt(linksleftMaxToday.replace(",", ""));
                if (linksleftUsedTodayInt >= linksleftMaxTodayInt) {
                    logger.info("Account is out of traffic because user has downloaded max. number of links per day already!");
                    ai.setTrafficLeft(0);
                }
                ai.setStatus(account.getType() + ": Links left today: " + (linksleftMaxTodayInt - linksleftUsedTodayInt));
            }
            final String[] supportedHosts = br.getRegex("domain=([^\"]+)\"").getColumn(0);
            ai.setMultiHostSupport(this, Arrays.asList(supportedHosts));
        }
        return ai;
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        if (!attemptStoredDownloadurlDownload(link, account)) {
            br.setFollowRedirects(true);
            this.loginWebsite(account, false);
            this.getPage("https://" + this.getHost() + "/filehostdl.html");
            final Form dlform = br.getFormbyProperty("id", "ajaxform");
            if (dlform == null) {
                this.checkErrorsLastResort(this.br, link, account);
            }
            if (StringUtils.isEmpty(dlform.getAction())) {
                dlform.setAction("/debrid_engine/gen_process_link.php");
            }
            dlform.put("urllist", Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            if (link.getDownloadPassword() != null) {
                dlform.put("passe", Encoding.urlEncode(link.getDownloadPassword()));
            }
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            this.submitForm(brc, dlform);
            String dllink = brc.getRegex("ng-model=\"add_links_values\"[^>]*>(https?://[^<]+)").getMatch(0);
            if (dllink == null) {
                dllink = brc.getRegex("(https?://[^/]+/download/[^\">]+)\"").getMatch(0);
            }
            if (dllink == null) {
                /* Use previous browser for errorchecking here . we don't want to run into logout failure accidently! */
                this.checkErrorsLastResort(this.br, link, account);
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(account));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to downloadable content");
            }
            link.setProperty(PROPERTY_DIRECTURL, dllink);
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final Account account) throws Exception {
        final String url = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, this.isResumeable(link, account), this.getMaxChunks(account));
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    @Override
    public boolean requires_WWW() {
        /* 2021-05-11 */
        return false;
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}