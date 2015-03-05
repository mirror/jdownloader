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
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1fichier.com" }, urls = { "https?://(?!www\\.)[a-z0-9\\-]+\\.(dl4free\\.com|alterupload\\.com|cjoint\\.net|desfichiers\\.com|dfichiers\\.com|megadl\\.fr|mesfichiers\\.org|piecejointe\\.net|pjointe\\.com|tenvoi\\.com|1fichier\\.com)/?|https?://(?:www\\.)?(dl4free\\.com|alterupload\\.com|cjoint\\.net|desfichiers\\.com|dfichiers\\.com|megadl\\.fr|mesfichiers\\.org|piecejointe\\.net|pjointe\\.com|tenvoi\\.com|1fichier\\.com)/\\?[a-z0-9]+" }, flags = { 2 })
public class OneFichierCom extends PluginForHost {

    private static AtomicInteger maxPrem                      = new AtomicInteger(1);
    private final String         PASSWORDTEXT                 = "(Accessing this file is protected by password|Please put it on the box bellow|Veuillez le saisir dans la case ci-dessous)";

    private final String         FREELINK                     = "freeLink";
    private final String         PROPERTY_PREMLINK            = "premLink";
    private final String         PREFER_RECONNECT             = "PREFER_RECONNECT";
    private final String         PREFER_SSL                   = "PREFER_SSL";
    private static final String  MAINPAGE                     = "http://1fichier.com/";
    private boolean              pwProtected                  = false;
    private Account              currAcc                      = null;
    private DownloadLink         currDownloadLink             = null;

    /* Max total connections for premium = 50 (RE: admin) */
    private static final int     maxchunks_account_premium    = -4;
    private static final int     maxdownloads_account_premium = 12;

    private static final int     maxchunks_free               = 1;
    private static final int     maxdownloads_free            = 1;

    public OneFichierCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.1fichier.com/en/register.pl");
        setConfigElements();
    }

    private String correctProtocol(final String input) {
        return input.replaceFirst("http://", "https://");
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        // link + protocol correction
        String url = correctProtocol(link.getDownloadURL());
        // Remove everything after the domain
        String linkID;
        if (link.getDownloadURL().matches("https?://[a-z0-9\\.]+(/|$)")) {
            final String[] idhostandName = new Regex(url, "(https?://)(.*?\\.)(.*?)(/|$)").getRow(0);
            if (idhostandName != null) {
                link.setUrlDownload(idhostandName[0] + idhostandName[1] + idhostandName[2]);
                linkID = getHost() + "://" + idhostandName[1];
                try {
                    link.setLinkID(linkID);
                } catch (final Throwable t) {
                    link.setProperty("LINKDUPEID", linkID);
                }
            }
        } else {
            linkID = getHost() + "://" + new Regex(url, "([a-z0-9]+)$").getMatch(0);
            try {
                link.setLinkID(linkID);
            } catch (final Throwable t) {
                link.setProperty("LINKDUPEID", linkID);
            }
        }
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            final Browser br = new Browser();
            br.getHeaders().put("User-Agent", "");
            br.getHeaders().put("Accept", "");
            br.getHeaders().put("Accept-Language", "");
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 100 links at once */
                    if (index == urls.length || links.size() > 100) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                for (final DownloadLink dl : links) {
                    sb.append("links[]=");
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    sb.append("&");
                }
                // remove last &
                sb.deleteCharAt(sb.length() - 1);
                br.postPageRaw(correctProtocol("http://1fichier.com/check_links.pl"), sb.toString());
                for (final DownloadLink dllink : links) {
                    // final String addedLink = dllink.getDownloadURL();
                    final String addedlink_id = this.getFID(dllink);
                    if (br.containsHTML(addedlink_id + ";;;(NOT FOUND|BAD LINK)")) {
                        dllink.setAvailable(false);
                        dllink.setName(addedlink_id);
                    } else if (br.containsHTML(addedlink_id + ";;;PRIVATE")) {
                        dllink.setProperty("privatelink", true);
                        dllink.setAvailable(true);
                        dllink.setName(addedlink_id);
                    } else {
                        final String[] linkInfo = br.getRegex(addedlink_id + ";([^;]+);(\\d+)").getRow(0);
                        if (linkInfo.length != 2) {
                            logger.warning("Linkchecker for 1fichier.com is broken!");
                            return false;
                        }
                        dllink.setProperty("privatelink", false);
                        dllink.setAvailable(true);
                        dllink.setFinalFileName(Encoding.htmlDecode(linkInfo[0]));
                        dllink.setDownloadSize(SizeFormatter.getSize(linkInfo[1]));
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        correctDownloadLink(link);
        pwProtected = false;
        this.setBrowserExclusive();
        prepareBrowser(br);
        br.setFollowRedirects(false);
        br.setCustomCharset("utf-8");
        br.postPage(correctProtocol("http://1fichier.com/check_links.pl"), "links[]=" + Encoding.urlEncode(link.getDownloadURL()));
        if (br.containsHTML(";;;NOT FOUND|;;;BAD LINK")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(">Software error:<")) {
            link.getLinkStatus().setStatusText("Cannot check availibility because of a server error!");
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.toString().equals("wait")) {
            br.getPage(link.getDownloadURL());
            final String siteFilename = br.getRegex(">Nom du fichier :</th><td>([^<>\"]*?)</td>").getMatch(0);
            String siteFilesize = br.getRegex("<th>Taille :</th><td>([^<>\"]*?)</td>").getMatch(0);
            if (siteFilename == null || siteFilesize == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setName(Encoding.htmlDecode(siteFilename));
            siteFilesize = siteFilesize.replace("ko", "kb");
            siteFilesize = siteFilesize.replace("Mo", "MB");
            siteFilesize = siteFilesize.replace("Go", "GB");
            link.setDownloadSize(SizeFormatter.getSize(siteFilesize));
            return AvailableStatus.TRUE;
        }
        if (br.containsHTML("password")) {
            pwProtected = true;
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.onefichiercom.passwordprotected", "This link is password protected"));
            return AvailableStatus.UNCHECKABLE;
        }
        final String[] linkInfo = br.getRegex("https?://[^;]+;([^;]+);(\\d+)").getRow(0);
        if (linkInfo == null || linkInfo.length == 0) {
            logger.warning("Available Status broken for link: " + link.getDownloadURL());
            return null;
        }
        String filename = linkInfo[0];
        if (filename == null) {
            filename = br.getRegex(">File name :</th><td>([^<>\"]*?)</td>").getMatch(0);
        }
        String filesize = linkInfo[1];
        if (filesize == null) {
            filesize = br.getRegex(">File size :</th><td>([^<>\"]*?)</td></tr>").getMatch(0);
        }
        if (filename != null) {
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        }
        if (filesize != null) {
            long size = 0;
            link.setDownloadSize(size = SizeFormatter.getSize(filesize));
            if (size > 0) {
                link.setProperty("VERIFIEDFILESIZE", size);
            }
        }

        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        if (account == null && (link != null && link.getProperty("HOTLINK", null) != null)) {
            return Integer.MAX_VALUE;
        }
        return super.getMaxSimultanDownload(link, account);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        AvailableStatus availableStatus = requestFileInformation(downloadLink);
        if (availableStatus != null) {
            switch (availableStatus) {
            case FALSE:
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case UNCHECKABLE:
            case UNCHECKED:
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File temporarily not available", 15 * 60 * 1000l);
            case TRUE:
                doFree(downloadLink);
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File temporarily not available", 15 * 60 * 1000l);
        }
    }

    /** Makes sure that we're allowed to download a link. */
    private void checkDownloadable(final DownloadLink dl) throws PluginException {
        if (dl.getBooleanProperty("privatelink", false)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This link is private! You're not authozized to download it.");
        }
    }

    @SuppressWarnings("deprecation")
    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        checkDownloadable(downloadLink);
        // to prevent wasteful requests.
        int i = 0;
        // 20140920 - stable needs this, as it seems to behave differently! raztoki
        // String dllink = downloadLink.getStringProperty(FREELINK, downloadLink.getDownloadURL() + "/en/index.html");
        String dllink = downloadLink.getStringProperty(FREELINK, downloadLink.getDownloadURL());
        br.setFollowRedirects(true);
        // at times the second chunk creates 404 errors!
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, maxchunks_free);
        if (dl.getConnection().getContentType().contains("html")) {
            /* could not resume, fetch new link */
            br.followConnection();
            downloadLink.setProperty(FREELINK, Property.NULL);
            dllink = null;
            br.setFollowRedirects(false);
            br.setCustomCharset("utf-8");
        } else {
            /* resume download */
            dl.startDownload();
            downloadLink.setProperty(FREELINK, dllink);
            return;
        }
        // use the English page, less support required
        boolean retried = false;
        while (true) {
            i++;
            br.setFollowRedirects(true);
            // redirect log 2414663166931
            if (i != 1) {
                // no need to do this link twice as it's been done above.
                br.getPage(downloadLink.getDownloadURL() + "/en/index.html");
            }
            br.setFollowRedirects(false);

            errorHandling(downloadLink, br, true);
            if (br.containsHTML(PASSWORDTEXT) || pwProtected) {
                handlePassword(downloadLink);
            } else {
                // base > submit:Free Download > submit:Show the download link + t:35140198 == link
                final Browser br2 = br.cloneBrowser();
                // final Form a1 = br2.getForm(0);
                // if (a1 == null) {
                // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                // }
                // a1.remove(null);
                br2.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                sleep(2000, downloadLink);
                // br2.submitForm(a1);
                br2.postPageRaw(br.getURL(), "");
                errorHandling(downloadLink, br2, true);
                dllink = br2.getRedirectLocation();
                if (dllink == null) {
                    sleep(2000, downloadLink);
                    Browser br3 = br.cloneBrowser();
                    Form a2 = br2.getForm(0);
                    if (a2 == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    br3.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                    sleep(2000, downloadLink);
                    br3.submitForm(a2);
                    errorHandling(downloadLink, br3, true);
                    if (dllink == null) {
                        dllink = br3.getRedirectLocation();
                    }
                    if (dllink == null) {
                        dllink = br3.getRegex("window\\.location\\s*=\\s*('|\")(https?://[a-zA-Z0-9_\\-]+\\.(1fichier|desfichiers)\\.com/[a-zA-Z0-9]+/.*?)\\1").getMatch(1);
                    }
                    if (dllink == null) {
                        String wait = br3.getRegex(" var count = (\\d+);").getMatch(0);
                        if (wait != null && retried == false) {
                            retried = true;
                            sleep(1000 * Long.parseLong(wait), downloadLink);
                            continue;
                        }
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            if (dllink != null) {
                break;
            }
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, maxchunks_free);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            errorHandling(downloadLink, this.br, true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(FREELINK, dllink);
        dl.startDownload();
    }

    private void errorHandling(final DownloadLink downloadLink, final Browser ibr, final boolean checkAll) throws Exception {
        if (br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 15 * 60 * 1000l);
        } else if (ibr.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        } else if (ibr.containsHTML(">Software error:<")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Software error'", 10 * 60 * 1000l);
        } else if (br.containsHTML(">Connexion à la base de données impossible<")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Internal database error", 5 * 60 * 1000l);
        } else if (br.containsHTML(">Votre adresse IP ouvre trop de connexions vers le serveur")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many connections - wait before starting new downloads", 3 * 60 * 1000l);
        }
        errorIpBlockedHandling(ibr);
    }

    private void errorIpBlockedHandling(Browser br) throws PluginException {
        String waittime = br.getRegex("you must wait (at least|up to) (\\d+) minutes between each downloads").getMatch(1);
        if (waittime == null) {
            waittime = br.getRegex(">You must wait (\\d+) minutes<").getMatch(0);
        }
        boolean isBlocked = waittime != null;
        isBlocked |= br.containsHTML("/>Téléchargements en cours");
        isBlocked |= br.containsHTML("En téléchargement standard, vous ne pouvez télécharger qu\\'un seul fichier");
        isBlocked |= br.containsHTML(">veuillez patienter avant de télécharger un autre fichier");
        isBlocked |= br.containsHTML(">You already downloading (some|a) file");
        isBlocked |= br.containsHTML(">You can download only one file at a time");
        isBlocked |= br.containsHTML(">Please wait a few seconds before downloading new ones");
        isBlocked |= br.containsHTML(">You must wait for another download");
        isBlocked |= br.containsHTML("Without premium status, you can download only one file at a time");
        isBlocked |= br.containsHTML("Without Premium, you must wait between downloads");
        // <div style="text-align:center;margin:auto;color:red">Warning ! Without premium status, you must wait between each
        // downloads<br/>Your last download finished 05 minutes ago</div>
        isBlocked |= br.containsHTML("you must wait between each downloads");
        // <div style="text-align:center;margin:auto;color:red">Warning ! Without premium status, you must wait 15 minutes between each
        // downloads<br/>You must wait 15 minutes to download again or subscribe to a premium offer</div>
        isBlocked |= br.containsHTML("you must wait \\d+ minutes between each downloads<");
        if (isBlocked) {
            final boolean preferReconnect = this.getPluginConfig().getBooleanProperty("PREFER_RECONNECT", false);

            if (waittime != null && preferReconnect) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 60 * 1001l);
            } else if (waittime != null && Integer.parseInt(waittime) >= 10) {
                /* High waittime --> Reconnect */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 60 * 1001l);
            } else if (preferReconnect) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
            } else if (waittime != null) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait between download, Reconnect is disabled in plugin settings", Integer.parseInt(waittime) * 60 * 1001l);
            } else {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait between download, Reconnect is disabled in plugin settings", 5 * 60 * 1001);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        AccountInfo ai = new AccountInfo();
        if (account.getUser() == null || !account.getUser().matches(".+@.+")) {
            ai.setStatus(":\r\nYou need to use Email as username!");
            account.setValid(false);
            return ai;
        }
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        // API login workaround for slow servers
        for (int i = 1; i <= 3; i++) {
            logger.info("1fichier.com: API login try 1 / " + i);
            try {
                br.getPage("https://1fichier.com/console/account.pl?user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + JDHash.getMD5(account.getPass()));
            } catch (final ConnectException e) {
                logger.info("1fichier.com: API login try 1 / " + i + " FAILED, trying again...");
                Thread.sleep(3 * 1000l);
                continue;
            }
            break;
        }
        String timeStamp = br.getRegex("(\\d+)").getMatch(0);
        String freeCredits = br.getRegex("0[\r\n]+([0-9\\.]+)").getMatch(0);
        // Use site login/site download if either API is not working or API says that there are no credits available
        if ("error".equalsIgnoreCase(br.toString()) || ("0".equals(timeStamp) && freeCredits == null)) {
            /**
             * Only used if the API fails and is wrong but that usually doesn't happen!
             */
            try {
                br = new Browser();
                login(true);
            } catch (final Exception e) {
                ai.setStatus("Username/Password also invalid via site login!");
                account.setValid(false);
                throw e;
            }
            ai.setStatus("Free User (Credits available)");
            account.setValid(true);
            maxPrem.set(maxdownloads_free);
            try {
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(maxdownloads_free);
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
                account.setProperty("free", true);
            }
            account.setProperty("freeAPIdisabled", true);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("https://www.1fichier.com/en/console/details.pl");
            String freeCredits2 = br.getRegex(">Your account have ([^<>\"]*?) of direct download credits").getMatch(0);
            if (freeCredits2 != null) {
                ai.setTrafficLeft(SizeFormatter.getSize(freeCredits2));
            } else {
                ai.setUnlimitedTraffic();
            }
            return ai;
        } else if ("0".equalsIgnoreCase(timeStamp)) {
            if (freeCredits != null) {
                /* not finished yet */
                account.setValid(true);
                if (Float.parseFloat(freeCredits) > 0) {
                    ai.setStatus("Free Account (Credits available)");
                } else {
                    ai.setStatus("Free Account (No credits available)");
                }
                ai.setTrafficLeft(SizeFormatter.getSize(freeCredits + " GB"));
                try {
                    account.setType(AccountType.FREE);
                    maxPrem.set(1);
                    account.setMaxSimultanDownloads(1);
                    account.setConcurrentUsePossible(false);
                } catch (final Throwable e) {
                    /* Not available in old 0.9.581 Stable */
                    account.setProperty("free", true);
                }
                account.setProperty("freeAPIdisabled", false);
            }
            return ai;
        } else {
            account.setValid(true);
            ai.setStatus("Premium Account");
            ai.setValidUntil(Long.parseLong(timeStamp) * 1000l + (24 * 60 * 60 * 1000l));
            ai.setUnlimitedTraffic();
            try {
                account.setType(AccountType.PREMIUM);
                maxPrem.set(maxdownloads_account_premium);
                account.setMaxSimultanDownloads(maxdownloads_account_premium);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
                account.setProperty("free", false);
            }
            return ai;
        }
    }

    @SuppressWarnings("unchecked")
    private void login(final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                prepareBrowser(br);
                final Object ret = this.currAcc.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(this.currAcc.getUser()).equals(this.currAcc.getStringProperty("name", Encoding.urlEncode(this.currAcc.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(this.currAcc.getPass()).equals(this.currAcc.getStringProperty("pass", Encoding.urlEncode(this.currAcc.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (this.currAcc.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(this.getHost(), key, value);
                        }
                        return;
                    }
                }
                logger.info("Using site login because API is either wrong or no free credits...");
                br.postPage("https://1fichier.com/login.pl", "lt=on&valider=Send&mail=" + Encoding.urlEncode(this.currAcc.getUser()) + "&pass=" + this.currAcc.getPass());
                final String logincheck = br.getCookie(MAINPAGE, "SID");
                if (logincheck == null || logincheck.equals("")) {
                    logger.info("Username/Password also invalid via site login!");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /* Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(this.getHost());
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                this.currAcc.setProperty("name", Encoding.urlEncode(this.currAcc.getUser()));
                this.currAcc.setProperty("pass", Encoding.urlEncode(this.currAcc.getPass()));
                this.currAcc.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                this.currAcc.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    private static final Object LOCK = new Object();

    @Override
    public String getAGBLink() {
        return "http://www.1fichier.com/en/cgu.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxdownloads_free;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    private String handlePassword(final DownloadLink downloadLink) throws IOException, PluginException {
        logger.info("This link seems to be password protected, continuing...");
        String passCode = downloadLink.getStringProperty("pass", null);
        if (passCode == null) {
            passCode = Plugin.getUserInput("Password?", downloadLink);
        }
        br.postPage(br.getURL(), "pass=" + passCode);
        if (br.containsHTML(PASSWORDTEXT)) {
            downloadLink.setProperty("pass", Property.NULL);
            throw new PluginException(LinkStatus.ERROR_RETRY, JDL.L("plugins.hoster.onefichiercom.wrongpassword", "Password wrong!"));
        }
        // set after regex checks
        downloadLink.setProperty("pass", passCode);
        return passCode;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        String dllink;
        setConstants(account, link);
        requestFileInformation(link);
        checkDownloadable(link);
        br = new Browser();
        if (account.getBooleanProperty("free", false) && account.getBooleanProperty("freeAPIdisabled")) {
            /**
             * Only used if the API fails and is wrong but that usually doesn't happen!
             */
            login(false);
            ensureSiteLogin();
            doFree(link);
        } else {
            dllink = checkDirectLink(link, PROPERTY_PREMLINK);
            dllink = null;
            if (dllink == null) {
                br.setFollowRedirects(true);
                sleep(2 * 1000l, link);
                /* TODO: This acts based in the users' setting 'Force download menu'. We're in touch with the admin to get this solved. */
                final String url = "https://" + getFID(link) + ".1fichier.com/" + "?u=" + Encoding.urlEncode(account.getUser()) + "&p=" + JDHash.getMD5(account.getPass());
                URLConnectionAdapter con = br.openGetConnection(url);
                if (con.getResponseCode() == 401) {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (con.isContentDisposition()) {
                    con.disconnect();
                    dllink = br.getURL();
                } else {
                    // for some silly reason we have reverted from api to webmethod, so we need cookies!. 20150201
                    br = new Browser();
                    login(true);
                    ensureSiteLogin();
                    br.setFollowRedirects(false);
                    br.getPage("https://" + getFID(link) + ".1fichier.com/");
                    dllink = br.getRedirectLocation();
                    if (pwProtected || br.containsHTML("password")) {
                        handlePassword(link);
                        dllink = br.getRedirectLocation();
                    }
                    try {
                        errorIpBlockedHandling(br);
                    } catch (PluginException e) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 45 * 1000l);
                    }
                }
                if (dllink == null) {
                    /* The link is always SSL - based on user setting it will redirect to either https or http. */
                    final String postLink = "https://1fichier.com/?" + getFID(link);
                    String postData = "did=0&";
                    if (this.getPluginConfig().getBooleanProperty(PREFER_SSL, false)) {
                        logger.info("User prefers download with SSL");
                        postData += "dlssl=SSL+Download";
                    } else {
                        logger.info("User prefers download without SSL");
                        postData += "dl=Download";
                    }
                    br.postPage(postLink, postData);
                    dllink = br.getRedirectLocation();
                    if (dllink == null) {
                        if (br.containsHTML("\">Warning \\! Without premium status, you can download only")) {
                            logger.info("Seems like this is no premium account or it's vot valid anymore -> Disabling it");
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            logger.warning("Final downloadlink 'dllink' is null");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                }
            }
            for (int i = 0; i != 2; i++) {
                br.setFollowRedirects(true);
                try {
                    logger.info("Connecting to " + dllink);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxchunks_account_premium);
                } catch (final ConnectException e) {
                    logger.info("Download failed because connection timed out, NOT a JD issue!");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Connection timed out", 60 * 60 * 1000l);
                } catch (final Exception e) {
                    logger.info("Download failed because: " + e.getMessage());
                    throw e;
                }

                if (dl.getConnection().getContentType().contains("html")) {
                    if ("http://www.1fichier.com/?c=DB".equalsIgnoreCase(br.getURL())) {
                        dl.getConnection().disconnect();
                        continue;
                    }
                    logger.warning("The final dllink seems not to be a file!");
                    br.followConnection();
                    errorHandling(link, this.br, false);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                link.setProperty(PROPERTY_PREMLINK, dllink);
                dl.startDownload();
                return;
            }
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                if (isJDStable()) {
                    con = br2.openGetConnection(dllink);
                } else {
                    con = br2.openHeadConnection(dllink);
                }
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

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    /** This function is there to make sure that we're really logged in (handling without API). */
    private boolean ensureSiteLogin() throws Exception {
        br.getPage("https://1fichier.com/console/index.pl");
        final String logincheck = br.getCookie(MAINPAGE, "SID");
        if (logincheck == null || logincheck.equals("") || !br.containsHTML("id=\"fileTree\"")) {
            logger.info("Site login seems not to be valid anymore - trying to refresh cookie");
            this.login(true);
            if (!ensureSiteLogin()) {
                logger.warning("Failed to refresh login cookie");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Successfully refreshed login cookie");
        } else {
            logger.info("Success: our login cookie is fine - no need to do anything");
        }
        return true;
    }

    private String getFID(final DownloadLink dl) {
        String test = new Regex(dl.getDownloadURL(), "/\\?([a-z0-9]+)$").getMatch(0);
        if (test == null) {
            test = new Regex(dl.getDownloadURL(), "://(?!www\\.)([a-z0-9]+)\\.").getMatch(0);
            if (test != null && test.matches("www")) {
                test = null;
            }
        }
        return test;
    }

    private boolean default_prefer_reconnect = false;
    private boolean default_prefer_ssl       = true;

    private void setConfigElements() {
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), PREFER_RECONNECT, JDL.L("plugins.hoster.onefichiercom.com.preferreconnect", "Reconnect, even if the wait time is only short (1-6 minutes)")).setDefaultValue(default_prefer_reconnect));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), PREFER_SSL, JDL.L("plugins.hoster.onefichiercom.com.preferreconnect", "Premium download: Prefer SSL?")).setDefaultValue(default_prefer_ssl));
    }

    private void prepareBrowser(final Browser br) {
        try {
            if (br == null) {
                return;
            }
            br.setConnectTimeout(3 * 60 * 1000);
            br.setReadTimeout(3 * 60 * 1000);
            br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.117 Safari/537.36");
            br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            br.getHeaders().put("Accept-Language", "en-us,en;q=0.5");
            br.getHeaders().put("Pragma", null);
            br.getHeaders().put("Cache-Control", null);
            // we want ENGLISH!
            br.setCookie(this.getHost(), "LG", "en");
        } catch (Throwable e) {
            /* setCookie throws exception in 09580 */
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}