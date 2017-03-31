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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LetitBitAccountBuilderImpl;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "multivip.net" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" })
public class MultiVipNet extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NOCHUNKS           = "NOCHUNKS";

    private static final String                            NICE_HOST          = "multivip.net";
    private static final String                            NICE_HOSTproperty  = "multivipnet";
    private static final String                            APIKEY             = "amQy";
    private static final boolean                           USE_API            = true;

    /* Default value is 10 */
    private static AtomicInteger                           maxPrem            = new AtomicInteger(10);

    public MultiVipNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://multivip.net/");
        this.setAccountwithoutUsername(true);
    }

    @Override
    public String getAGBLink() {
        return "http://multivip.net/contact.php";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        // define custom browser headers
        br.getHeaders().put("Accept", "application/json");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setCookie("http://multivip.net/", "lang", "en");
        return br;
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        return new LetitBitAccountBuilderImpl(callback);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        /* First check if the file is too big */
        final long max_downloadable_filesize = account.getLongProperty("max_downloadable_filesize", 0);
        if (max_downloadable_filesize > 0 && downloadLink.getDownloadSize() > max_downloadable_filesize) {
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        br.setCurrentURL(null);
        int maxChunks = -2;
        if (link.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        link.setProperty("multivipnetdirectlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info(NICE_HOST + ": Unknown download error");
            int timesFailed = link.getIntegerProperty(NICE_HOSTproperty + "timesfailed_unknowndlerror", 0);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty(NICE_HOSTproperty + "timesfailed_unknowndlerror", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
            } else {
                link.setProperty(NICE_HOSTproperty + "timesfailed_unknowndlerror", Property.NULL);
                logger.info(NICE_HOST + ": Unknown error - disabling current host!");
                tempUnavailableHoster(account, link, 60 * 60 * 1000l);
            }
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(MultiVipNet.NOCHUNKS, false) == false) {
                    link.setProperty(MultiVipNet.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(MultiVipNet.NOCHUNKS, false) == false) {
                link.setProperty(MultiVipNet.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {

        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(link.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }

        this.br = newBrowser();
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        showMessage(link, "Task 1: Generating Link");
        String dllink = checkDirectLink(link, "multivipnetdirectlink");
        if (dllink == null) {
            /* request Download */
            if (USE_API) {
                br.getPage("http://multivip.net/api.php?apipass=" + Encoding.Base64Decode(APIKEY) + "&do=addlink&vipkey=" + Encoding.urlEncode(account.getPass()) + "&ip=&link=" + Encoding.urlEncode(link.getDownloadURL()));
                /* Should never happen because size limit is set in fetchAccountInfo and handled via canHandle */
                if ("204".equals(PluginJSonUtils.getJsonValue(br, "error"))) {
                    /*
                     * Should never happen because size limit is set in fetchAccountInfo and handled via canHandle. Update 16.05.2015: This
                     * can indeed happen for links with unknown filesize!
                     */
                    account.setType(AccountType.FREE);
                    account.getAccountInfo().setStatus("Free account");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This link too big to download via " + this.getHost());
                }
                dllink = PluginJSonUtils.getJsonValue(br, "directlink");
            } else {
                br.postPage("http://multivip.net/links.php", "do=addlinks&links=" + Encoding.urlEncode(link.getDownloadURL()) + "&vipkey=" + Encoding.urlEncode(account.getPass()));
                if (br.containsHTML("Universal VIP key is missing or incorrect")) {
                    logger.info("Given Vip key is invalid");
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Vip key!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid Vip key!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } else if (br.containsHTML("This is a FREE key and File size in")) {
                    /*
                     * Should never happen because size limit is set in fetchAccountInfo and handled via canHandle. Update 16.05.2015: This
                     * can indeed happen for links with unknown filesize!
                     */
                    account.setType(AccountType.FREE);
                    account.getAccountInfo().setStatus("Free Account");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This link too big to download via " + this.getHost());
                } else if (br.containsHTML("Unfortunately this key was expired")) {
                    /* Our account has expired */
                    final String expire_date = br.getRegex("Unfortunately this key was expired <strong>([^<>\"]*?)</strong>").getMatch(0);
                    if (expire_date != null) {
                        account.getAccountInfo().setValidUntil(TimeFormatter.getMilliSeconds(expire_date, "MM-dd-yy, hh:mm", Locale.ENGLISH));
                    }
                    account.getAccountInfo().setExpired(true);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.containsHTML("Failed to get information about the file")) {
                    logger.info("Seems like the current host doesn't work anymore --> Disabling it");
                    tempUnavailableHoster(account, link, 60 * 60 * 1000l);
                }
                final Regex account_info = br.getRegex("you have after this action <strong>([^<>\"]*?)</strong> till <strong>([^<>\"]*?) </strong>");
                final String traffic_left = account_info.getMatch(0);
                final String expire_date = account_info.getMatch(1);
                if (traffic_left != null && expire_date != null) {
                    account.getAccountInfo().setTrafficLeft(SizeFormatter.getSize(traffic_left));
                    account.getAccountInfo().setValidUntil(TimeFormatter.getMilliSeconds(expire_date, "MM-dd-yy, hh:mm", Locale.ENGLISH));
                }
                dllink = br.getRegex("\"(http[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink == null) {
                logger.info(NICE_HOST + ": Unknown error");
                int timesFailed = link.getIntegerProperty(NICE_HOSTproperty + "timesfailed_unknown", 0);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    link.setProperty(NICE_HOSTproperty + "timesfailed_unknown", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
                } else {
                    link.setProperty(NICE_HOSTproperty + "timesfailed_unknown", Property.NULL);
                    logger.info(NICE_HOST + ": Unknown error - disabling current host!");
                    tempUnavailableHoster(account, link, 60 * 60 * 1000l);
                }
            }
            dllink = dllink.replace("\\", "");
        }
        showMessage(link, "Task 2: Download begins!");
        handleDL(account, link, dllink);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        account.setMaxSimultanDownloads(20);
        maxPrem.set(20);
        br.getPage("http://multivip.net/api.php?apipass=" + Encoding.Base64Decode(APIKEY) + "&do=keycheck&vipkey=" + Encoding.urlEncode(account.getPass()));
        final String error = PluginJSonUtils.getJsonValue(br, "error");
        if (error != null) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Vip key!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid Vip key!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final String expire = PluginJSonUtils.getJsonValue(br, "diedate");
        final String max_downloadable_filesize = PluginJSonUtils.getJsonValue(br, "limit");
        final String traffic_left_kb = PluginJSonUtils.getJsonValue(br, "points");
        ai.setValidUntil(Long.parseLong(expire) * 1000);
        ai.setTrafficLeft(Long.parseLong(traffic_left_kb) * 1024);
        account.setProperty("max_downloadable_filesize", Long.parseLong(max_downloadable_filesize) * 1024);
        br.getPage("/api.php?apipass=" + Encoding.Base64Decode(APIKEY) + "&do=getlist");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] hostDomains = br.getRegex("\"allow\":\\[(.*?)\\]").getColumn(0);
        for (final String domains : hostDomains) {
            final String[] realDomains = new Regex(domains, "\"(.*?)\"").getColumn(0);
            for (final String realDomain : realDomains) {
                supportedHosts.add(realDomain);
            }
        }
        if (max_downloadable_filesize.equals("0")) {
            /* Premium keys have no max downloadable filesize limit */
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Vip key");
        } else {
            /* Free keys have a max downloadable filesize limit */
            /*
             * Free keys have an expire date as well. Once expired, they cannot be used to download anything anymore and JD will not accept
             * them (shows correct message'Account expired').
             */
            account.setType(AccountType.FREE);
            ai.setStatus("Free Vip key");
        }
        ai.setMultiHostSupport(this, supportedHosts);
        account.setValid(true);
        return ai;
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, final long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait 30 mins to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}