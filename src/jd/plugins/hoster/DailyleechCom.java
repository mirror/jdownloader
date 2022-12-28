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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dailyleech.com" }, urls = { "" })
public class DailyleechCom extends antiDDoSForHost {
    private static final String          PROTOCOL                  = "http://";
    /* Connection limits */
    private static final int             ACCOUNT_MAXDLS            = 1;
    private static final boolean         ACCOUNT_PREMIUM_RESUME    = true;
    private static final int             ACCOUNT_PREMIUM_MAXCHUNKS = 1;
    /** This is the old project of proleech.link owner */
    private static MultiHosterManagement mhm                       = new MultiHosterManagement("dailyleech.com");

    public DailyleechCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://dailyleech.com/payment/");
    }

    @Override
    public String getAGBLink() {
        return "http://dailyleech.com/";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = prepBR(this.br);
        mhm.runCheck(account, link);
        login(account, false);
        final String dllink = getDllink(link, account);
        if (StringUtils.isEmpty(dllink)) {
            mhm.handleErrorGeneric(account, link, "dllinknull", 2, 5 * 60 * 1000l);
        }
        handleDL(account, link, dllink);
    }

    private String getDllink(final DownloadLink link, final Account account) throws Exception {
        String dllink = checkDirectLink(br, link, getCachedLinkPropertyKey(account));
        if (dllink == null) {
            dllink = getDllinkWebsite(link, account);
        }
        return dllink;
    }

    private String[] searchMyFiles(final Browser br, final DownloadLink link) throws Exception {
        final Browser brc = br.cloneBrowser();
        brc.getPage(PROTOCOL + this.getHost() + "/cbox/myfile.php");
        final String elements[] = brc.getRegex("<tr>\\s*(<td>\\s*\\d+\\s*</td>.*?)</tr>").getColumn(0);
        final PluginForHost hostPlugin = getNewPluginInstance(link.getDefaultPlugin().getLazyP());
        if (elements != null && elements.length > 0) {
            for (String element : elements) {
                final String source = new Regex(element, "href\\s*=\\s*\'(.*?)'").getMatch(0);
                final String name = new Regex(element, "</a>\\s*</td>\\s*<td>\\s*(.*?)\\s*</td").getMatch(0);
                final String size = new Regex(element, "</td>\\s*<td>\\s*([0-9\\.]+\\s*[TGMKB]+)\\s*<").getMatch(0);
                final String download = new Regex(element, "href\\s*=\\s*'(https?://" + getHost() + "/download/.*?)'").getMatch(0);
                String ret = null;
                if (StringUtils.isAllNotEmpty(source, download) && hostPlugin.canHandle(source)) {
                    // same plugin
                    if (StringUtils.equals(link.getPluginPatternMatcher(), source)) {
                        // same URL
                        ret = download;
                    } else {
                        final DownloadLink dummy = new DownloadLink(hostPlugin, hostPlugin.getHost(), source);
                        if (StringUtils.equals(link.getLinkID(), dummy.getLinkID())) {
                            // same linkID
                            ret = download;
                        }
                    }
                }
                if (ret != null) {
                    final Browser brCheck = brc.cloneBrowser();
                    final URLConnectionAdapter con = checkDirectLink(brCheck, brCheck.createHeadRequest(ret));
                    if (con != null) {
                        final String[] res = new String[] { ret, con.getURL().toExternalForm() };
                        logger.info("found searchMyFiles: " + link + "->" + Arrays.toString(res));
                        return res;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void clean() {
        try {
            super.clean();
        } finally {
            synchronized (LOCKS) {
                // WeakHashMap.expungeStaleEntries
                LOCKS.size();
            }
        }
    }

    private static WeakHashMap<ReentrantLock, String> LOCKS = new WeakHashMap<ReentrantLock, String>();

    private ReentrantLock getLock(final DownloadLink link, final Account account) {
        synchronized (LOCKS) {
            final String id = link.getHost() + account.getId().getID();
            for (Entry<ReentrantLock, String> lock : LOCKS.entrySet()) {
                if (id.equals(lock.getValue())) {
                    return lock.getKey();
                }
            }
            final ReentrantLock lock = new ReentrantLock();
            LOCKS.put(lock, id);
            return lock;
        }
    }

    private String getDllinkWebsite(final DownloadLink link, final Account account) throws Exception {
        final ReentrantLock lock = getLock(link, account);
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new PluginException(LinkStatus.ERROR_RETRY, null, e);
        }
        try {
            final String readable_filesize = SizeFormatter.formatBytes(link.getDownloadSize());
            final String target_filename = link.getName();
            if (target_filename == null) {
                /* 2019-06-28: We cannot download URLs without filenames */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Cannot download URLs without filename");
            }
            /*
             * Okay this website is an absolute chaos. We need to generate downloadlinks through a chatbox ... after adding new URLs, we
             * need to try to find our downloadlinks by going through the chat and need to identify our file by filename!
             */
            getPage(PROTOCOL + this.getHost() + "/cbox/cbox.php");
            final String cbox_first_access_url = br.getRegex("name=\"cboxform\"\\s*?scrolling=\"no\"\\s*?src=\"(http[^\"]+)").getMatch(0);
            final String cbox_main_url = br.getRegex("name=\"cboxmain\"[^<>]*?src=\"(http[^\"]+)").getMatch(0);
            /* Get main parameters. */
            final String username = br.getRegex("nme=([^<>\"\\&]+)").getMatch(0);
            final String key = br.getRegex("nmekey=([a-f0-9]{32})").getMatch(0);
            final String boxid = new Regex(cbox_main_url, "boxid=(\\d+)").getMatch(0);
            final String boxtag = new Regex(cbox_main_url, "boxtag=([^\\&]+)").getMatch(0);
            if (cbox_main_url == null || username == null || key == null || boxid == null || boxtag == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String[] foundMyFiles = searchMyFiles(br, link);
            String dllink = foundMyFiles != null ? foundMyFiles[1] : null;
            if (dllink != null) {
                return dllink;
            }
            String host = new Regex(cbox_main_url, "https?://(www\\d+)\\..+").getMatch(0);
            if (host == null) {
                if (cbox_first_access_url != null) {
                    getPage(cbox_first_access_url);
                    host = br.getRegex("s_phost\\s*?=\\s*?\"([a-z0-9]+)\"").getMatch(0);
                }
                if (host == null) {
                    /* Fallback */
                    host = "www4";
                }
            }
            // /* Find parameters to load single posts in that chat. */
            // getPage("http://flr-eu0.cbox.ws/4/lp/_?pool=" + boxid + "-0-" + boxtag);
            // final String id = br.getRegex("id=(\\d+)").getMatch(0);
            // final String hash = br.getRegex("hash=(\\d+)").getMatch(0);
            // /* That is easentially boxid + "-0" */
            // final String pool = br.getRegex("pool=([0-9\\-]+)").getMatch(0);
            // if (id == null || hash == null || pool == null) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            {
                /* Post downloadurl in chat --> Wait for answer of bot containing downloadlink */
                String downloadurlStr = link.getDefaultPlugin().buildExternalDownloadURL(link, this);
                if (link.getDownloadPassword() != null) {
                    /* Add download-password if needed */
                    downloadurlStr += "|" + link.getDownloadPassword();
                }
                final Form dlform = new Form();
                dlform.setMethod(MethodType.POST);
                dlform.setAction("http://" + host + ".cbox.ws/box/index.php?boxid=" + boxid + "&boxtag=" + boxtag + "&sec=submit");
                final String param_post = "%5Bcenter%5D%20" + Encoding.urlEncode(downloadurlStr) + "%20good_link%20%7C%20%5Bcolor%3Dred%5D%5Bb%5D%20" + Encoding.urlEncode("JDOWNLOADER_" + target_filename) + "%20%5B%2Fb%5D%5B%2Fcolor%5D%20%7C%20%5Bcolor%3Dblack%5D%5Bb%5D%20" + Encoding.urlEncode(readable_filesize) + "%20%5B%2Fb%5D%5B%2Fcolor%5D%5Bbr%5D%20%5Bden%5DChecked%20By%5Bxanh%5D%20Dailyleech.com%5B%2Fcenter%5D%20";
                dlform.put("nme", username);
                dlform.put("eml", "");
                dlform.put("key", key);
                dlform.put("fkey", "");
                dlform.put("pic", "");
                dlform.put("auth", "");
                dlform.put("pst", param_post);
                dlform.put("captme", "");
                dlform.put("capword", "");
                dlform.put("caphash", "");
                dlform.put("aj", "x");
                dlform.put("lp", "0");
                br.submitForm(dlform);
            }
            /* Load the list of recent posts and try to find the answer which contains our downloadurl */
            {
                int counter = 0;
                final int maxLoops = 120;
                do {
                    /* Every time we call this URL we will go back in time one single post ... */
                    /* Wait here on the first loop as bots need some seconds to reply with downloadlinks. */
                    this.sleep(5000, link);
                    logger.info("Searching downloadlink : Attempt " + counter + " of " + maxLoops);
                    getPage(cbox_main_url);
                    final String archive_url = br.getRegex("\\'([^\"\\']+sec=archive[^\"\\']+i=)\\'").getMatch(0);
                    final String archive_id = br.getRegex("\\?cf\\.op:(\\d+)\\)").getMatch(0);
                    dllink = searchDownloadlink(link, account);
                    if (StringUtils.isEmpty(dllink)) {
                        if (archive_url != null && archive_id != null) {
                            /* Let's also go back into the archive (if possible) just in case there are many posts in a short time. */
                            getPage(archive_url + archive_id);
                            dllink = searchDownloadlink(link, account);
                        }
                    }
                    counter++;
                } while (dllink == null && counter <= maxLoops);
            }
            return dllink;
        } finally {
            lock.unlock();
        }
    }

    private void handlePostError(final String post, final DownloadLink link, final Account account) throws Exception {
        final String message = new Regex(post, "<span[^>]*class\\s*=\\s*\"bbColor\"[^>]*style\\s*=\\s*\"color:red\"[^>]*>(.*?)</span>").getMatch(0);
        if (message != null) {
            if (message.matches("(?i).*Your file is big.*when only allowed.*")) {
                // <span class="bbColor" style="color:red">Your file is big! (5.1 GB) when allowed only 5.0 GB</span>
                throw new PluginException(LinkStatus.ERROR_FATAL, message);
            } else if (message.matches("(?i).*hoster (unavailable|unavailable).*")) {
                // <span class="bbColor" style="color:red"> hoster: Hoster unvailable. _RANDOMNUM_ </span>
                mhm.putError(account, link, 15 * 60 * 1000l, message);
            } else if (message.matches("(?i).*error getting the link.*")) {
                // <span class="bbColor" style="color:red"> Error getting the link from this account. _RANDOMNUM_ </span>
                mhm.putError(account, link, 15 * 60 * 1000l, message);
            } else if (message.matches("(?i).*No account is working.*")) {
                // <span class="bbColor" style="color:red"> No account is working. Try repost later. </span>
                mhm.putError(account, link, 15 * 60 * 1000l, message);
            } else if (message.matches("(?i).*No account is working.*")) {
                // <span class="bbColor" style="color:red">Your file is big! (875.2 MB). You have left (756.4 MB) bandwidth limit 3.0 GB.
                // Try this
                // host tomorrow <img class.....> <br> [....]Time Left To Reset Your Bandwith For This Host: [do]4 Hours 47 Minutes 16
                // Seconds</span>
                mhm.putError(account, link, 30 * 60 * 1000l, message);
            }
        }
        if (new Regex(post, "I have a problem. Please repost your link later").patternFind()) {
            // <b>I have a problem. Please repost your link later. </b>
            mhm.putError(account, link, 30 * 60 * 1000l, "I have a problem. Please repost your link later");
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, message);
    }

    private String searchDownloadlink(final DownloadLink link, final Account account) throws Exception {
        final Set<String> domains = new HashSet<String>();
        domains.add(link.getHost());
        final String[] siteSupportedNames = link.getDefaultPlugin().siteSupportedNames();
        if (siteSupportedNames != null) {
            domains.addAll(Arrays.asList(siteSupportedNames));
        }
        final String username = "RicardoXV";
        final String[] posts = br.getRegex("tr id=\"\\d+\">.*?</tr>").getColumn(-1);
        for (final String post : posts) {
            final String adminPost = new Regex(post, "class\\s*=\\s*\"[^\"]*pn_adm[^\"]*\"[^>]*>\\s*(.*?)\\s*<").getMatch(0);
            if (adminPost == null) {
                // not from admin/bot
                continue;
            } else if (new Regex(post, "(" + Pattern.quote(username) + ":)").getMatch(0) == null) {
                // no reply to our user
                continue;
            }
            String domain = null;
            for (final String checkDomain : domains) {
                if (new Regex(post, "(?i)(domain=" + Pattern.quote(checkDomain) + ")").getMatch(0) != null || new Regex(post, "(?i)(\\[" + Pattern.quote(checkDomain) + "\\])").getMatch(0) != null) {
                    domain = checkDomain;
                    break;
                }
            }
            if (domain == null) {
                // different domain
                continue;
            }
            /* 2022-12-28 - user can post multiple urls , resulting in /multi */
            // final String url_multi = br.getRegex("(https?://(?:www\\.)?dailyleech\\.com/multi/[^<>\"]+)\"").getMatch(0);
            final String url_download_tmp = new Regex(post, "(https?://(?:www\\.)?dailyleech\\.com/download/[^<>\"]+)\"").getMatch(0);
            if (url_download_tmp == null) {
                handlePostError(post, link, account);
            }
            final String todayUsed = new Regex(post, "(?i)Today\\s*used\\s*:\\s*([0-9\\.]+\\s*[TGMKB]+)\\s*<").getMatch(0);
            final String hosterUsed = new Regex(post, "(?i)" + Pattern.quote(domain) + "\\s*used\\s*:\\s*([0-9\\.]+\\s*[TGMKB]+)\\s*<").getMatch(0);
            final String hosterLeft = new Regex(post, "(?i)" + Pattern.quote(domain) + "\\s*left\\s*:\\s*([0-9\\.]+\\s*[TGMKB]+)\\s*<").getMatch(0);
            final String foundMyFiles[] = searchMyFiles(br, link);
            logger.info("Today used:" + todayUsed + "|" + domain + " used:" + hosterUsed + "|" + domain + " left:" + hosterLeft);
            if (foundMyFiles != null && foundMyFiles[0].equals(url_download_tmp)) {
                logger.info("found searchMyFiles: " + link + "->" + Arrays.toString(foundMyFiles));
                return foundMyFiles[1];
            } else {
                logger.info("found message: " + link + "->" + url_download_tmp);
                return url_download_tmp;
            }
        }
        return null;
    }

    private String getCachedLinkPropertyKey(final Account account) {
        return this.getHost() + "directlink";
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            link.setProperty(getCachedLinkPropertyKey(account), dllink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                link.removeProperty(getCachedLinkPropertyKey(account));
                br.followConnection(true);
                mhm.handleErrorGeneric(account, link, "unknowndlerror", 2, 5 * 60 * 1000l);
            }
            this.dl.startDownload();
        }
    }

    private URLConnectionAdapter checkDirectLink(final Browser br, final Request request) {
        URLConnectionAdapter con = null;
        try {
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            con = brc.openRequestConnection(request);
            if (!looksLikeDownloadableContent(con)) {
                brc.followConnection(true);
                throw new IOException();
            } else {
                return con;
            }
        } catch (final Exception e) {
            logger.log(e);
            return null;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    private String checkDirectLink(final Browser br, final DownloadLink downloadLink, final String property) {
        final String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                if (checkDirectLink(brc, brc.createHeadRequest(dllink)) != null) {
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.setProperty(property, Property.NULL);
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = prepBR(this.br);
        final AccountInfo ai = fetchAccountInfoWebsite(account);
        return ai;
    }

    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        /*
         * 2017-11-29: Lifetime premium not (yet) supported via website mode! But by the time we might need the website version again, they
         * might have stopped premium lifetime sales already as that has never been a good idea for any (M)OCH.
         */
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        long expire = 0;
        final String expireStr = br.getRegex("Until\\&nbsp;([^<>\"]+)<").getMatch(0);
        if (expireStr != null) {
            expire = TimeFormatter.getMilliSeconds(expireStr, "E',' dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
        }
        ArrayList<String> supportedHosts = new ArrayList<String>();
        if (expire > System.currentTimeMillis()) {
            account.setType(AccountType.PREMIUM);
            /* More simultaneous downloads are theoretically possibly but this script will then fail to find downloadlinks! */
            account.setMaxSimultanDownloads(ACCOUNT_MAXDLS);
            ai.setValidUntil(expire);
            ai.setUnlimitedTraffic();
        } else {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_MAXDLS);
            ai.setTrafficLeft(0);
        }
        getPage("/hostsp/");
        final String[] hostlist = br.getRegex("domain=([^<>\"\\'/]+)\"").getColumn(0);
        if (hostlist != null) {
            supportedHosts = new ArrayList<String>(Arrays.asList(hostlist));
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            /* Load cookies */
            br.setCookiesExclusive(true);
            this.br = prepBR(this.br);
            loginWebsite(account, force);
        }
    }

    private void loginWebsite(final Account account, final boolean force) throws Exception {
        try {
            Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                /* Try to avoid login-captcha! */
                this.br.setCookies(this.getHost(), cookies);
                /*
                 * Even though login is forced first check if our cookies are still valid --> If not, force login!
                 */
                getPage(PROTOCOL + this.getHost() + "/cbox/cbox.php");
                if (isLoggedIn()) {
                    logger.info("Login via cached cookies successful");
                    cookies = this.br.getCookies(this.getHost());
                    account.saveCookies(cookies, "");
                    return;
                } else {
                    logger.info("Login via cached cookies failed");
                }
                /* Clear cookies to prevent unknown errors as we'll perform a full login below now. */
                this.br = prepBR(createNewBrowserInstance());
            }
            getPage(PROTOCOL + this.getHost() + "/cbox/login.php");
            final Form loginform = br.getFormbyProperty("class", "omb_loginForm");
            if (loginform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            loginform.put("Email", Encoding.urlEncode(account.getUser()));
            loginform.put("Password", Encoding.urlEncode(account.getPass()));
            final String image = loginform.getRegex("(captcha_code_file\\.php\\?rand=\\d+)").getMatch(0);
            if (image != null) {
                /* Captcha seems to always be present! */
                final DownloadLink dummyLink = new DownloadLink(this, "Account", getHost(), "http://" + getHost(), true);
                final String captcha = getCaptchaCode(image, dummyLink);
                loginform.put("6_letters_code", Encoding.urlEncode(captcha));
            }
            submitForm(loginform);
            getPage("/cbox/cbox.php");
            if (!isLoggedIn()) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                cookies = this.br.getCookies(this.getHost());
                account.saveCookies(cookies, "");
            }
        } catch (final PluginException e) {
            account.clearCookies("");
            throw e;
        }
    }

    private boolean isLoggedIn() {
        return br.containsHTML("logout\\.php");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return ACCOUNT_MAXDLS;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_MAXDLS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}