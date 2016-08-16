//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.download.DownloadInterface;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "1tube.to" }, urls = { "https?://(?:www\\.)?1tube\\.to/f/([^<>\"]*?\\-[A-Za-z0-9]+\\.html|[A-Za-z0-9]+)" }, flags = { 2 })
public class OneTubeTo extends antiDDoSForHost {

    public OneTubeTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://1tube.to/reg/");
    }

    @Override
    public String getAGBLink() {
        return "https://1tube.to/p/tos/";
    }

    /* Tags: 1tube.to, hdstream.to */
    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_FREE_RESUME          = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 1;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 10;

    private static final int     PREMIUM_OVERALL_MAXCON       = -ACCOUNT_PREMIUM_MAXDOWNLOADS;

    private Exception            checklinksexception          = null;

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        final String fid = getFID(link);
        link.setLinkID(getHost() + "://" + fid);
        link.setUrlDownload("https://1tube.to/f/" + fid);
    }

    @Override
    protected Browser prepBrowser(Browser prepBr, String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.setCookie(getHost(), "lang", "en");
            prepBr.getHeaders().put("User-Agent", "JDownloader");
            /* User can select https or http in his hdstream account, therefore, redirects should be allowed */
            prepBr.setFollowRedirects(true);
        }
        return prepBr;
    }

    /** Using API: https://1tube.to/p/api/ */
    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            LinkedHashMap<String, Object> api_data = null;
            LinkedHashMap<String, Object> api_data_singlelink = null;
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once (50 tested, more might be possible) */
                    if (index == urls.length || links.size() > 50) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("fun=check&check=");
                for (final DownloadLink dl : links) {
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    sb.append("\n");
                }
                postPage("https://1tube.to/p/api/?json=1", sb.toString());
                api_data = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                final Object data = api_data.get("data");
                if (data != null && data instanceof LinkedHashMap) {
                    api_data = (LinkedHashMap<String, Object>) data;
                } else {
                    /* All offline */
                    api_data = null;
                }
                for (final DownloadLink dl : links) {
                    final String fid = getFID(dl);
                    if (api_data == null) {
                        dl.setName(fid);
                        dl.setAvailable(false);
                        continue;
                    }
                    api_data_singlelink = (LinkedHashMap<String, Object>) api_data.get(fid);
                    String state = null;
                    try {
                        state = (String) api_data_singlelink.get("state");
                    } catch (final Throwable e) {
                    }
                    if (api_data_singlelink == null || state == null || state.equalsIgnoreCase("off")) {
                        dl.setName(fid);
                        dl.setAvailable(false);
                        continue;
                    }
                    final String filename = (String) api_data_singlelink.get("name");
                    final long filesize = JavaScriptEngineFactory.toLong(api_data_singlelink.get("size"), 0);
                    final String sha1 = (String) api_data_singlelink.get("hash");

                    /* Trust API */
                    dl.setAvailable(true);
                    dl.setFinalFileName(filename);
                    dl.setDownloadSize(filesize);
                    dl.setSha1Hash(sha1);
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            checklinksexception = e;
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        /* If exception happens in availablecheck it will be caught --> Browser is empty --> Throw it here to prevent further errors. */
        if (checklinksexception != null) {
            throw checklinksexception;
        }
        checkDownloadable(br);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    @SuppressWarnings("deprecation")
    private void doFree(final DownloadLink downloadLink, boolean resumable, int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        final String fid = getFID(downloadLink);
        final String premiumtoken = getPremiumToken(downloadLink);
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            br.setFollowRedirects(false);
            getPage("https://1tube.to/send/?visited=" + fid);
            final String canPlay = PluginJSonUtils.getJsonValue(br, "canPlay");
            final String server = PluginJSonUtils.getJsonValue(br, "server");
            final String waittime = PluginJSonUtils.getJsonValue(br, "wait");
            final String free_downloadable = PluginJSonUtils.getJsonValue(br, "downloadable");
            final String free_downloadable_max_filesize = new Regex(free_downloadable, "mb(\\d+)").getMatch(0);
            final String traffic_left_free = PluginJSonUtils.getJsonValue(br, "traffic");
            if ("true".equals(canPlay)) {
                /* Prefer to download the stream if possible as it has the same filesize as download but no waittime. */
                final Browser br2 = br.cloneBrowser();
                if (server == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                getPage(br2, "https://sx" + server + ".1tube.to/send/?token=" + fid + "&stream=1");
                dllink = br2.getRedirectLocation();
            }
            if (dllink == null) {
                /* Stream impossible? --> Download file! */
                /*
                 * Note that premiumtokens can override this. NOTE that premiumtokens do not (yet) exist for this host (project), see
                 * hdstream.to plugin.
                 */
                if ("premium".equals(free_downloadable) || (free_downloadable_max_filesize != null && downloadLink.getDownloadSize() >= SizeFormatter.getSize(free_downloadable_max_filesize + " mb")) && "".equals(premiumtoken)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } else if (traffic_left_free.equals("0")) {
                    /*
                     * We can never know how long we habve to wait - also while we might have this problem for one file, other, smaller
                     * files can still be downloadable --> Let's wait an hour, then try again.
                     */
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 1 * 60 * 60 * 1000l);
                }
                dllink = getDllink(downloadLink);
                if (waittime == null) {
                    /* This should never happen! */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final int wait = Integer.parseInt(waittime);
                /* Make sure that the premiumtoken is valid - if it is not valid, wait is higher than 0 */
                if (!premiumtoken.equals("") && wait == 0) {
                    logger.info("Seems like the user is using a valid premiumtoken, enabling chunks & resume...");
                    resumable = ACCOUNT_PREMIUM_RESUME;
                    maxchunks = PREMIUM_OVERALL_MAXCON;
                } else {
                    sleep(Integer.parseInt(waittime) * 1001l, downloadLink);
                }
            }
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        errorhandlingFree(dl, br);
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://1tube.to";
    private static Object       LOCK     = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(getHost(), cookies);
                    return;
                }
                br.setFollowRedirects(false);
                postPage("https://1tube.to/json/login.php", "data=%7B%22username%22%3A%22" + Encoding.urlEncode(account.getUser()) + "%22%2C+%22password%22%3A%22" + Encoding.urlEncode(account.getPass()) + "%22%7D");
                if (br.getCookie(MAINPAGE, "username") == null || br.containsHTML("\"logged_in\":false")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        return fetchAccountInfoHdstream(br, account);
    }

    private PluginForHost plugin = null;

    private void setPlugin() throws PluginException {
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("hdstream.to");
            if (plugin == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Plugin not found!");
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        checkDownloadable(br);
        login(account, false);
        br.setFollowRedirects(false);
        if (account.getType() == AccountType.FREE) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            final String dllink = getDllink(link);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            errorhandlingPremium(dl, br, account);
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
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

    /** Returns final downloadlink, same for free and premium */
    private String getDllink(final DownloadLink dl) {
        return "http://sx" + PluginJSonUtils.getJsonValue(br, "server") + ".1tube.to/send.php?token=" + getFID(dl);
    }

    /**
     * Links which contain a premium token can be downloaded via free like a premium user - in case such a token exists in a link, this
     * function will return it.
     *
     * @return: "" (empty String) if there is no token and the token if there is one
     */
    @SuppressWarnings("deprecation")
    private String getPremiumToken(final DownloadLink dl) {
        final String addedlink = dl.getDownloadURL();
        String premtoken = new Regex(addedlink, "hdstream\\.to/(f/|#\\!f=)[A-Za-z0-9]+\\-([A-Za-z0-9]+)$").getMatch(1);
        if (premtoken == null) {
            premtoken = "";
        }
        return premtoken;
    }

    private String getFID(final DownloadLink dl) {
        final String fid = new Regex(dl.getDownloadURL(), "([A-Za-z0-9]+)(?:\\.html)?$").getMatch(0);
        return fid;
    }

    private AccountInfo fetchAccountInfoHdstream(final Browser br, final Account account) throws Exception {
        setPlugin();
        return ((jd.plugins.hoster.HdStreamTo) plugin).fetchAccountInfoHdstream(br, account);
    }

    public void checkDownloadable(final Browser br) throws PluginException {
        setPlugin();
        ((jd.plugins.hoster.HdStreamTo) plugin).checkDownloadable(br);
    }

    private void errorhandlingPremium(DownloadInterface dl, Browser br, Account account) throws Exception {
        setPlugin();
        ((jd.plugins.hoster.HdStreamTo) plugin).errorhandlingPremium(dl, br, account);
    }

    private void errorhandlingFree(DownloadInterface dl, Browser br) throws Exception {
        setPlugin();
        ((jd.plugins.hoster.HdStreamTo) plugin).errorhandlingFree(dl, br);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}