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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
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
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dailyleech.com" }, urls = { "" })
public class DailyleechCom extends PluginForHost {
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
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        } else {
            mhm.runCheck(account, link);
            return true;
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
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
        prepBR(this.br);
        mhm.runCheck(account, link);
        login(account, false);
        final String directurlproperty = getCachedLinkPropertyKey(account);
        // if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
        // link.removeProperty(directurlproperty);
        // }
        final String storedDirecturl = link.getStringProperty(directurlproperty);
        final String directurl;
        if (storedDirecturl != null) {
            directurl = storedDirecturl;
        } else {
            directurl = this.getDllinkWebsite(link, account);
            if (StringUtils.isEmpty(directurl)) {
                mhm.handleErrorGeneric(account, link, "Failed to find final downloadurl", 50, 3 * 60 * 1000l);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        try {
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                mhm.handleErrorGeneric(account, link, "Final downloadurl did not lead to file", 20, 3 * 60 * 1000l);
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directurlproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired?");
            } else {
                throw e;
            }
        }
        link.setProperty(directurlproperty, directurl);
        this.dl.startDownload();
    }

    private String getDllinkWebsite(final DownloadLink link, final Account account) throws Exception {
        final ReentrantLock lock = getLock(link, account);
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new PluginException(LinkStatus.ERROR_RETRY, null, e);
        }
        try {
            final String target_filename = link.getName();
            if (target_filename == null) {
                /* 2019-06-28: We cannot download URLs without filenames */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Cannot download URLs without filename");
            }
            /**
             * Okay this website is an absolute chaos: </br>
             * We need to generate downloadlinks through a chatbox ... after adding new URLs, we need to try to find our downloadlinks by
             * going through the chat and need to identify our file by filename! </br>
             * Direct-downloadurls can be broken so we need to ignore the ones we know are broken to speed-up the process of finding the
             * correct one.
             */
            br.getPage(PROTOCOL + this.getHost() + "/cbox/cbox.php");
            final String cbox_first_access_url = br.getRegex("name=\"cboxform\"\\s*?scrolling=\"no\"\\s*?src=\"(http[^\"]+)").getMatch(0);
            final String cbox_main_url = br.getRegex("name=\"cboxmain\"[^<>]*?src=\"(http[^\"]+)").getMatch(0);
            /* Get main parameters. */
            final String username = br.getRegex("nme=([^<>\"\\&]+)").getMatch(0);
            final String key = br.getRegex("nmekey=([a-f0-9]{32})").getMatch(0);
            final String boxid = new Regex(cbox_main_url, "boxid=(\\d+)").getMatch(0);
            final String boxtag = new Regex(cbox_main_url, "boxtag=([^\\&]+)").getMatch(0);
            if (cbox_main_url == null || username == null || key == null || boxid == null || boxtag == null) {
                logger.warning("One or more required parameters are missing: cbox_main_url = " + cbox_main_url + " username = " + username + " key = " + key + " boxid = " + boxid + " boxtag = " + boxtag);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final HashSet<String> invalidatedUrls = new HashSet<String>();
            String dllink = findFinalDownloadurl(invalidatedUrls, br.cloneBrowser(), link, account, true);
            if (dllink != null) {
                /* User has attempted to download this URL before so we can re-use the resulting directurl. */
                logger.info("Re-using directurl found via searchMyFiles: " + dllink);
                return dllink;
            }
            String internalSubdomain = new Regex(cbox_main_url, "^https?://(www\\d+)\\..*").getMatch(0);
            if (internalSubdomain == null) {
                if (cbox_first_access_url != null) {
                    logger.info("Failed to find internalSubdomain right away -> Accessing cbox_first_access_url to find it");
                    br.getPage(cbox_first_access_url);
                    internalSubdomain = br.getRegex("s_phost\\s*?=\\s*?\"([a-z0-9]+)\"").getMatch(0);
                }
                if (internalSubdomain == null) {
                    /* Fallback */
                    internalSubdomain = "www4";
                    logger.warning("Using hardcoded internalSubdomain as fallback: " + internalSubdomain);
                }
            }
            /* Post downloadurl in chat --> Wait for answer of bot containing downloadlink */
            logger.info("POSTing downloadurl in bot chat");
            String downloadurlStr = getDownloadurlForMultihost(link);
            if (link.getDownloadPassword() != null) {
                /* Add download-password if needed */
                downloadurlStr += "|" + link.getDownloadPassword();
            }
            final String humanReadableFilesize;
            if (link.getView().getBytesTotal() == -1) {
                humanReadableFilesize = "NAN";
            } else {
                humanReadableFilesize = SizeFormatter.formatBytes(link.getView().getBytesTotal());
            }
            final Form dlform = new Form();
            dlform.setMethod(MethodType.POST);
            dlform.setAction("http://" + internalSubdomain + ".cbox.ws/box/index.php?boxid=" + boxid + "&boxtag=" + boxtag + "&sec=submit");
            /* The text "good_link" will indicate to the bot/chat that this file has been checked and is valid. */
            final String param_post = Encoding.urlEncode("[center] good_link " + downloadurlStr + " [br] Filename: " + link.getName() + " ([b][color=red]" + humanReadableFilesize + "[/color][/b]) [br] HashInfo: " + link.getHashInfo() + " [br] [b]Automatically added by JDownloader[/b] [br] [den]Checked by JDownloader[/center]");
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
            /* Load the list of recent posts and try to find the answer which contains our downloadurl. */
            int counter = 0;
            final int maxLoops = 120;
            do {
                /* Every time we call this URL we will go back in time one single post ... */
                /* Wait here on the first loop as bots need some seconds to reply with downloadlinks. */
                this.sleep(5000, link);
                logger.info("Searching final downloadlink | Attempt " + counter + " of " + maxLoops);
                br.getPage(cbox_main_url);
                final Browser brc = br.cloneBrowser();
                brc.getPage(PROTOCOL + this.getHost() + "/cbox/myfile.php");
                dllink = findFinalDownloadurl(invalidatedUrls, brc, link, account, false);
                if (StringUtils.isEmpty(dllink)) {
                    final String archive_url = br.getRegex("\\'([^\"\\']+sec=archive[^\"\\']+i=)\\'").getMatch(0);
                    final String archive_id = br.getRegex("\\?cf\\.op:(\\d+)\\)").getMatch(0);
                    if (archive_url != null && archive_id != null) {
                        /* Let's also go back into the archive (if possible) just in case there are many posts in a short time. */
                        final String archiveurl_full = archive_url + archive_id;
                        logger.info("Searching for downloadurl in archive: " + archiveurl_full);
                        brc.getPage(archiveurl_full);
                        dllink = findFinalDownloadurl(invalidatedUrls, brc, link, account, false);
                    }
                }
                if (this.isAbort()) {
                    logger.info("Stopping because: Aborted by user");
                    break;
                } else if (counter >= maxLoops) {
                    logger.info("Stopping because: Failed to find final downloadurl");
                    break;
                } else if (dllink != null) {
                    logger.info("Stopping because: Found final downloadurl: " + dllink);
                    break;
                } else {
                    counter++;
                    continue;
                }
            } while (true);
            if (dllink != null) {
                /* Print additional information */
                this.searchDownloadlinkBotPost(link, account, false);
                return dllink;
            } else {
                /* Print additional information and look for reason of failure */
                this.searchDownloadlinkBotPost(link, account, true);
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    private String getDownloadurlForMultihost(final DownloadLink link) {
        return link.getDefaultPlugin().buildExternalDownloadURL(link, this);
    }

    private String findFinalDownloadurl(final HashSet<String> invalidatedUrls, final Browser br, final DownloadLink link, final Account account, final boolean checkResult) throws Exception {
        final String foundMyFiles[] = findFinalDownloadurlCore(invalidatedUrls, br, link, account, checkResult);
        if (foundMyFiles == null) {
            logger.info("Failed to find any result");
            return null;
        } else {
            return foundMyFiles[1];
        }
    }

    private String[] findFinalDownloadurlCore(final HashSet<String> invalidatedUrls, final Browser br, final DownloadLink link, final Account account, final boolean checkResult) throws Exception {
        final String elements[] = br.getRegex("<tr>\\s*(<td>\\s*\\d+\\s*</td>.*?)</tr>").getColumn(0);
        if (elements == null || elements.length == 0) {
            return null;
        }
        // final PluginForHost hostPlugin = getNewPluginInstance(link.getDefaultPlugin().getLazyP());
        final String filehosterSourceDownloadurl = this.getDownloadurlForMultihost(link);
        final PluginForHost hostPlugin = getNewPluginInstance(link.getDefaultPlugin().getLazyP());
        for (final String element : elements) {
            final String sourceurl = new Regex(element, "href\\s*=\\s*\'([^<>\"']+)'").getMatch(0);
            final String filename = new Regex(element, "</a>\\s*</td>\\s*<td>\\s*(.*?)\\s*</td").getMatch(0);
            // final String size = new Regex(element, "</td>\\s*<td>\\s*([0-9\\.]+\\s*[TGMKB]+)\\s*<").getMatch(0);
            final String possibleDownloadurl = new Regex(element, "href\\s*=\\s*'(https?://" + Pattern.quote(getHost()) + "/download/[^<>\"']+)'").getMatch(0);
            if (sourceurl == null || possibleDownloadurl == null) {
                /* This should never happen. */
                logger.warning("Skipping element because: Required parameter is missing | filename = " + filename + " | possibleDownloadurl = " + possibleDownloadurl);
                logger.warning("element = " + element);
                continue;
            }
            /*
             * Try to identify postText even if file is not (yet) downloadable so that we can at least find the reason of failure in the
             * end.
             */
            final DownloadLink dummy = new DownloadLink(hostPlugin, hostPlugin.getHost(), sourceurl);
            if (sourceurl.equals(filehosterSourceDownloadurl)) {
                logger.info("Matched post via filehosterSourceDownloadurl");
            } else if (StringUtils.equals(link.getLinkID(), dummy.getLinkID())) {
                /* same linkID */
                /**
                 * This extra check is necessary because in theory this website may display the submitted URL in a slightly modified version
                 * than the original. </br>
                 * Using the linkIDs for comparison might increase our chances of finding a result.
                 */
                logger.info("Matched post via linkID");
            } else if (StringUtils.equals(filename, link.getName())) {
                logger.info("Matched post via filename");
            } else {
                logger.info("Skipping non-matching element: " + element);
                continue;
            }
            if (invalidatedUrls.contains(possibleDownloadurl)) {
                logger.info("Skipping element because: Directurl was previously invalidated: " + possibleDownloadurl);
                continue;
            }
            if (possibleDownloadurl != null) {
                String[] res = null;
                if (checkResult) {
                    logger.info("Checking possible result: " + possibleDownloadurl);
                    final Browser brCheck = br.cloneBrowser();
                    final URLConnectionAdapter con = checkDirectLink(brCheck, brCheck.createHeadRequest(possibleDownloadurl));
                    if (con != null) {
                        res = new String[] { possibleDownloadurl, con.getURL().toExternalForm(), element };
                    } else {
                        logger.warning("Possible final downloadurl looks to be broken");
                        invalidatedUrls.add(possibleDownloadurl);
                    }
                } else {
                    res = new String[] { possibleDownloadurl, possibleDownloadurl, element };
                }
                if (res != null) {
                    logger.info("Returning result: " + link + "->" + Arrays.toString(res));
                    return res;
                }
            }
        }
        return null;
    }

    /** Searches bot-post about state of added downloads in chatbox: https://dailyleech.com/cbox/cbox.php */
    private String searchDownloadlinkBotPost(final DownloadLink link, final Account account, final boolean checkErrors) throws Exception {
        final Set<String> domains = new HashSet<String>();
        domains.add(link.getHost());
        final String[] siteSupportedNames = link.getDefaultPlugin().siteSupportedNames();
        if (siteSupportedNames != null) {
            domains.addAll(Arrays.asList(siteSupportedNames));
        }
        final String[] posts = br.getRegex("tr id=\"\\d+\">.*?</tr>").getColumn(-1);
        String resultPostText = null;
        final String filehosterSourceDownloadurl = this.getDownloadurlForMultihost(link);
        for (final String post : posts) {
            if (post.contains(filehosterSourceDownloadurl)) {
                resultPostText = post;
                break;
            }
        }
        if (resultPostText != null) {
            logger.info("Found postText: " + resultPostText);
            /* That information is only given for hosts that have daily limits. */
            final String todayUsed = new Regex(resultPostText, "(?i)Today\\s*used\\s*:\\s*([0-9\\.]+\\s*[TGMKB]+)\\s*<").getMatch(0);
            final String hosterUsed = new Regex(resultPostText, "(?i)\\s*used\\s*:\\s*([0-9\\.]+\\s*[TGMKB]+)\\s*<").getMatch(0);
            final String hosterLeft = new Regex(resultPostText, "(?i)\\s*left\\s*:\\s*([0-9\\.]+\\s*[TGMKB]+)\\s*<").getMatch(0);
            logger.info("Today used: " + todayUsed + " | " + link.getHost() + " used: " + hosterUsed + " | " + link.getHost() + " left: " + hosterLeft);
            if (checkErrors) {
                handlePostErrors(resultPostText, link, account);
            }
            return resultPostText;
        } else {
            logger.info("Failed to find any result");
            return null;
        }
    }

    /** Checks for errormessage in text posted by bot in "cbox chat". */
    private void handlePostErrors(final String postText, final DownloadLink link, final Account account) throws Exception {
        if (postText == null) {
            return;
        }
        final String message = new Regex(postText, "<span[^>]*class\\s*=\\s*\"bbColor\"[^>]*style\\s*=\\s*\"color:red\"[^>]*>(.*?)</span>").getMatch(0);
        if (message != null) {
            if (message.matches("(?i).*Your file is big.*when only allowed.*")) {
                // <span class="bbColor" style="color:red">Your file is big! (5.1 GB) when allowed only 5.0 GB</span>
                throw new PluginException(LinkStatus.ERROR_FATAL, message);
            } else if (message.matches("(?i).*hoster (unavailable|unavailable).*")) {
                // <span class="bbColor" style="color:red"> hoster: Hoster unvailable. _RANDOMNUM_ </span>
                mhm.putError(account, link, 5 * 60 * 1000l, message);
            } else if (message.matches("(?i).*error getting the link.*")) {
                // <span class="bbColor" style="color:red"> Error getting the link from this account. _RANDOMNUM_ </span>
                mhm.putError(account, link, 5 * 60 * 1000l, message);
            } else if (message.matches("(?i).*No account is working.*")) {
                // <span class="bbColor" style="color:red"> No account is working. Try repost later. </span>
                mhm.putError(account, link, 5 * 60 * 1000l, message);
            } else if (message.matches("(?i).*No account is working.*")) {
                // <span class="bbColor" style="color:red">Your file is big! (875.2 MB). You have left (756.4 MB) bandwidth limit 3.0 GB.
                // Try this
                // host tomorrow <img class.....> <br> [....]Time Left To Reset Your Bandwith For This Host: [do]4 Hours 47 Minutes 16
                // Seconds</span>
                mhm.putError(account, link, 5 * 60 * 1000l, message);
            }
        }
        if (new Regex(postText, "(?i)I have a problem. Please repost your link later").patternFind()) {
            // <b>I have a problem. Please repost your link later. </b>
            mhm.putError(account, link, 5 * 60 * 1000l, "I have a problem. Please repost your link later");
        } else {
            logger.info("Unable to find any errormessage in given postText");
        }
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

    private String getCachedLinkPropertyKey(final Account account) {
        return this.getHost() + "directlink";
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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        /*
         * 2017-11-29: Lifetime premium not (yet) supported via website mode! But by the time we might need the website version again, they
         * might have stopped premium lifetime sales already as that has never been a good idea for any (M)OCH.
         */
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        long expire = 0;
        final String expireStr = br.getRegex("(?i)Until(?:\\&nbsp;)?([^<>\"]+)<").getMatch(0);
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
        br.getPage("/hostsp/");
        final String[] hostlist = br.getRegex("domain=([^<>\"\\'/]+)\"").getColumn(0);
        if (hostlist != null) {
            supportedHosts = new ArrayList<String>(Arrays.asList(hostlist));
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            prepBR(this.br);
            loginWebsite(account, force);
        }
    }

    private void loginWebsite(final Account account, final boolean force) throws Exception {
        try {
            final Cookies cookies = account.loadCookies("");
            /* Re-use cookies to try to avoid login-captcha! */
            if (cookies != null) {
                this.br.setCookies(cookies);
                /*
                 * Even though login is forced first check if our cookies are still valid --> If not, force login!
                 */
                br.getPage(PROTOCOL + this.getHost() + "/cbox/cbox.php");
                if (isLoggedIn(br)) {
                    logger.info("Login via cached cookies successful");
                    account.saveCookies(br.getCookies(this.getHost()), "");
                    return;
                } else {
                    logger.info("Login via cached cookies failed");
                    br.clearCookies(null);
                }
            }
            br.getPage(PROTOCOL + this.getHost() + "/cbox/login.php");
            final Form loginform = br.getFormbyProperty("class", "omb_loginForm");
            if (loginform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            loginform.put("Email", Encoding.urlEncode(account.getUser()));
            loginform.put("Password", Encoding.urlEncode(account.getPass()));
            /* Login-Captcha seems to be always required. */
            final String image = loginform.getRegex("(captcha_code_file\\.php\\?rand=\\d+)").getMatch(0);
            if (image != null) {
                final DownloadLink dummyLink = new DownloadLink(this, "Account", getHost(), "https://" + getHost(), true);
                final String captcha = getCaptchaCode(image, dummyLink);
                loginform.put("6_letters_code", Encoding.urlEncode(captcha));
            }
            /*
             * Sending this form will always redirect us to the login page once again. We need to refresh this once to see if we're actually
             * logged in or not but let's check for invalid captcha status before.
             */
            br.submitForm(loginform);
            if (!isLoggedIn(br) && br.containsHTML("(?i)>\\s*The captcha code does not match")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            logger.info("Looks like correct login captcha has been entered -> Checking if we're logged in");
            br.getPage("/cbox/cbox.php");
            if (!isLoggedIn(br)) {
                throw new AccountInvalidException();
            } else {
                account.saveCookies(br.getCookies(this.getHost()), "");
            }
        } catch (final PluginException e) {
            account.clearCookies("");
            throw e;
        }
    }

    private boolean isLoggedIn(final Browser br) {
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