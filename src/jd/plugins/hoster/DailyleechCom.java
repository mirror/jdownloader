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
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
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
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dailyleech.com" }, urls = { "" })
public class DailyleechCom extends antiDDoSForHost {
    private static final String          PROTOCOL                  = "http://";
    /* Connection limits */
    private static final int             ACCOUNT_MAXDLS            = 1;
    private static final boolean         ACCOUNT_PREMIUM_RESUME    = true;
    private static final int             ACCOUNT_PREMIUM_MAXCHUNKS = 1;
    private static final boolean         USE_API                   = false;
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
        /* 2019-06-13: They've blocked this User-Agent for unknown reasons. Accessing their login-page will return 403 when it is used! */
        // br.getHeaders().put("User-Agent", "JDownloader");
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
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = prepBR(this.br);
        mhm.runCheck(account, link);
        login(account, false);
        final String dllink = getDllink(link);
        if (StringUtils.isEmpty(dllink)) {
            mhm.handleErrorGeneric(account, link, "dllinknull", 2, 5 * 60 * 1000l);
        }
        handleDL(account, link, dllink);
    }

    private String getDllink(final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        if (dllink == null) {
            if (USE_API) {
                dllink = getDllinkAPI(link);
            } else {
                dllink = getDllinkWebsite(link);
            }
        }
        return dllink;
    }

    private String getDllinkAPI(final DownloadLink link) throws IOException, PluginException {
        return null;
    }

    private String getDllinkWebsite(final DownloadLink link) throws Exception {
        final String readable_filesize = SizeFormatter.formatBytes(link.getDownloadSize());
        final String target_filename = link.getName();
        if (target_filename == null) {
            /* 2019-06-28: We cannot download URLs without filenames */
            throw new PluginException(LinkStatus.ERROR_FATAL, "Cannot download URLs without filename");
        }
        /*
         * Okay this website is an absolute chaos. We need to generate downloadlinks through a chatbox ... after adding new URLs, we need to
         * try to find our downloadlinks by going through the chat and need to identify our file by filename!
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
        String dllink = null;
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
                dllink = searchDownloadlink(link);
                if (StringUtils.isEmpty(dllink)) {
                    if (archive_url != null && archive_id != null) {
                        /* Let's also go back into the archive (if possible) just in case there are many posts in a short time. */
                        getPage(archive_url + archive_id);
                        dllink = searchDownloadlink(link);
                    }
                }
                if (dllink != null) {
                    break;
                }
                counter++;
            } while (counter <= maxLoops);
        }
        return dllink;
    }

    private String searchDownloadlink(final DownloadLink link) {
        /*
         * Find downloadlink! This is a nightmare ... let's get back to the last X posts in this chatbox and try to find our downloadlink BY
         * FILENAME ...
         */
        final String linkid = link.getLinkID();
        final String target_filename = link.getName();
        /*
         * Veeery ugly - server changes filenames so we try to build the filenames which it maybe uses later to find the correct
         * downloadlink ...
         */
        /* Attention: They do allow dots in filenames! */
        final String assumedServersideFilename = target_filename.replaceAll("( |@|\\-|_|\\(|\\)|\\'|\\[|\\]|\\||#|\\+|,)", "");
        final String assumedServersideFilename_first_half = assumedServersideFilename.substring(0, assumedServersideFilename.length() / 2);
        final String assumedServersideFilename_second_half = assumedServersideFilename.substring(assumedServersideFilename.length() / 2, assumedServersideFilename.length());
        final String[] posts = br.getRegex("tr id=\"\\d+\">.*?</tr>").getColumn(-1);
        String dllink = null;
        String firstDownloadlinkFound = null;
        for (final String post : posts) {
            /* 2019-06-28: Seems like large files can get split up in multiple parts (???) */
            // final String url_multi = br.getRegex("(https?://(?:www\\.)?dailyleech\\.com/multi/[^<>\"]+)\"").getMatch(0);
            final String url_download_tmp = new Regex(post, "(https?://(?:www\\.)?dailyleech\\.com/download/[^<>\"]+)\"").getMatch(0);
            if (url_download_tmp != null && firstDownloadlinkFound == null) {
                firstDownloadlinkFound = url_download_tmp;
            }
            /** TODO: Improve matching */
            final boolean foundPost = post.contains(linkid) || post.contains(target_filename) || post.contains(assumedServersideFilename);
            /* Matching only parts of our assumed filename increases the chances of finding it ... */
            final boolean foundPostUnsafe = post.contains(assumedServersideFilename_first_half) || post.contains(assumedServersideFilename_second_half);
            if (url_download_tmp != null && (foundPost || foundPostUnsafe)) {
                /* We might have found the correct downloadurl! */
                dllink = url_download_tmp;
                break;
            }
        }
        /* Very bad and unreliable workaround!! */
        // if (dllink == null && firstDownloadlinkFound != null) {
        // logger.warning("Failed to find ");
        // dllink = firstDownloadlinkFound;
        // }
        return dllink;
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        link.setProperty(this.getHost() + "directlink", dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html")) {
                br.followConnection();
                handleAPIErrors(this.br);
                mhm.handleErrorGeneric(account, link, "unknowndlerror", 2, 5 * 60 * 1000l);
            }
            this.dl.startDownload();
        } catch (final Exception e) {
            link.setProperty(this.getHost() + "directlink", Property.NULL);
            throw e;
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = prepBR(this.br);
        final AccountInfo ai;
        if (USE_API) {
            ai = fetchAccountInfoAPI(account);
        } else {
            ai = fetchAccountInfoWebsite(account);
        }
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

    public AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        return null;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            /* Load cookies */
            br.setCookiesExclusive(true);
            this.br = prepBR(this.br);
            if (USE_API) {
                loginAPI(account, force);
            } else {
                loginWebsite(account, force);
            }
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
                this.br = prepBR(new Browser());
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
            }
            cookies = this.br.getCookies(this.getHost());
            account.saveCookies(cookies, "");
        } catch (final PluginException e) {
            account.clearCookies("");
            throw e;
        }
    }

    private boolean isLoggedIn() {
        return br.containsHTML("logout\\.php");
    }

    private void loginAPI(final Account account, final boolean force) throws Exception {
    }

    /** Keep this for possible future API implementation */
    private void handleAPIErrors(final Browser br) throws PluginException {
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