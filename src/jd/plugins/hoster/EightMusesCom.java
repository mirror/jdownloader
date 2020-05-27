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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.utils.net.URLHelper;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "8muses.com" }, urls = { "https?://(?:www\\.|comics\\.)?8muses\\.com/(?:(comics/)?picture/([^/]+/){1,}\\d+|forum/(?:data/)?attachments/.+)" })
public class EightMusesCom extends antiDDoSForHost {
    public EightMusesCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://comics.8muses.com/forum/register/");
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other: Helper plugin which most likely downloads directurls and also has additional account support
    private String              dllink       = null;
    private static final String TYPE_DIRECT  = ".+8muses\\.com/forum/.+";
    private static final String TYPE_PICTURE = "https?://[^/]+/(?:comics/)?picture/(.+)";
    public static final String  TYPE_FORUM   = "https?://[^/]+/forum/(?:data/)?attachments/(.+)";

    @Override
    public String getAGBLink() {
        return "http://www.8muses.com/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private String getFilenameURL(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(TYPE_FORUM)) {
            /* TYPE_FORUM */
            return getURLNameForum(link.getPluginPatternMatcher());
        } else {
            /* TYPE_PICTURE */
            return new Regex(link.getPluginPatternMatcher(), TYPE_PICTURE).getMatch(0);
        }
    }

    public static String getURLNameForum(final String url) {
        String urlname = new Regex(url, TYPE_FORUM).getMatch(0);
        /*
         * Try to remove fileID from string and change last "-" to "." to have a file-extension for e.g. if linkcheck fails/file
         * offline/premiumonly so that we still got a nice filename.
         */
        /* E.g. "<sometitle>-<ext>.<fileID>/" --> "<sometitle>.<ext>" */
        final Regex extensionAndFileIDRegex = new Regex(urlname, "-([a-z0-9]+)(\\.\\d+/?$)");
        final String ext = extensionAndFileIDRegex.getMatch(0);
        final String fileID = extensionAndFileIDRegex.getMatch(1);
        if (fileID != null) {
            urlname = urlname.replace(fileID, "");
            urlname = urlname.substring(0, urlname.lastIndexOf("-")) + "." + ext;
        }
        return urlname;
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        dllink = null;
        /* 2020-05-27: Don't do this as handlePremium could have authorized us in beforehand! */
        // this.setBrowserExclusive();
        br.setFollowRedirects(true);
        /*
         * Make sure uncheckable content does not get "Unknown filename" and thus gets errors like "File already exists" or gets recognized
         * as mirror files!
         */
        final String filename_url = getFilenameURL(link);
        if (filename_url != null) {
            link.setName(filename_url);
        }
        String filename = null;
        if (link.getPluginPatternMatcher().matches(TYPE_DIRECT)) {
            dllink = link.getPluginPatternMatcher();
        } else {
            getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<b>Notice</b>:")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = new Regex(link.getPluginPatternMatcher(), "8muses\\.com/(?:[^/]*/)?picture/(?:\\d+\\-)?(.+)").getMatch(0);
            filename = filename.replace("/", "_");
            final String ractive_public = n(br.getRegex("<script id=\"ractive-public\" type=\"text/plain\">\\s*(.*?)\\s*<").getMatch(0));
            final String imageDir = br.getRegex("imageDir\"\\s*value\\s*=\\s*\"(/data/.{2}/)\"").getMatch(0);
            final String imageName = br.getRegex("imageName\"\\s*value\\s*=\\s*\"([^<>\"]*?)\"").getMatch(0);
            final String imageHost = br.getRegex("imageHost\"\\s*value\\s*=\\s*\"([^<>\"]*?)\"").getMatch(0);
            if (imageDir != null && imageName != null) {
                dllink = imageDir + imageName;
            } else if (imageHost != null && imageName != null) {
                dllink = imageHost + "/image/fl/" + imageName;
            } else if (imageName != null) {
                /* 2018-02-09 */
                dllink = br.getURL("/image/fl/" + imageName).toString();
            } else if (ractive_public != null) {
                final String image = new Regex(ractive_public, "\"picture\"\\s*:\\s*\\{.*?\"public.*?\"\\s*:\\s*\"(.*?)\"").getMatch(0);
                if (image != null) {
                    dllink = br.getURL("/image/fl/" + image + ".jpg").toString();
                }
            }
            if (ractive_public.contains("\"pictures\":[]")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (filename == null || dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = Encoding.htmlDecode(filename);
            filename = filename.trim();
            filename = encodeUnicode(filename);
            String ext = getFileNameExtensionFromString(dllink);
            /* Make sure that we get a correct extension */
            if (ext == null || !ext.matches("\\.[A-Za-z0-9]{3,5}")) {
                ext = ".jpg";
            }
            if (!filename.endsWith(ext)) {
                filename += ext;
            }
            link.setFinalFileName(filename);
        }
        br.setFollowRedirects(true);
        if (dllink != null && !isDownload) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    if (con.getHeaderField("cf-bgj") != null && !link.hasProperty(BYPASS_CLOUDFLARE_BGJ)) {
                        link.setProperty(BYPASS_CLOUDFLARE_BGJ, Boolean.TRUE);
                    }
                    link.setDownloadSize(con.getLongContentLength());
                    if (filename == null) {
                        link.setFinalFileName(Plugin.getFileNameFromHeader(con));
                    }
                } else if (con.getResponseCode() == 403) {
                    /* Account required to download */
                    return AvailableStatus.TRUE;
                    // throw new AccountRequiredException();
                } else if (con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
                }
                link.setProperty("directlink", dllink);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    public static final String BYPASS_CLOUDFLARE_BGJ = "bpCfBgj";

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (link.getProperty(BYPASS_CLOUDFLARE_BGJ) != null) {
            logger.info("Apply Cloudflare BGJ bypass");
            dllink = URLHelper.parseLocation(br.getURL(dllink), "&bpcfbgj=" + System.nanoTime());
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                /* 2020-05-14: Typically this means a URL is only downloadable via account */
                // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                throw new AccountRequiredException();
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        }
        dl.startDownload();
    }

    private String n(String t) {
        if (t == null) {
            return null;
        }
        if (!t.startsWith("!")) {
            return t;
        }
        final Matcher m = Pattern.compile("([\\x21-\\x7e])").matcher(t.substring(1).replace("&gt;", ">").replace("&lt;", "<").replace("&amp;", "&"));
        final StringBuffer sb = new StringBuffer(t.length());
        while (m.find()) {
            final String search = m.group(1);
            if (search == null) {
                break;
            } else {
                final String replacement = String.valueOf((char) (33 + (search.codePointAt(0) + 14) % 94));
                m.appendReplacement(sb, replacement);
            }
        }
        return sb.toString();
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force && System.currentTimeMillis() - account.getCookiesTimeStamp("") < 5 * 60 * 1000l) {
                        logger.info("Cookies are still fresh --> Trust cookies without login");
                        return false;
                    }
                    br.getPage("https://comics." + this.getHost() + "/forum/");
                    if (this.isLoggedin()) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://comics." + this.getHost() + "/forum/login/login");
                final Form loginform = br.getFormbyActionRegex(".*forum/login/login.*");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("login", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                /* We want to get long lasting cookies! */
                loginform.put("remember", "1");
                br.submitForm(loginform);
                if (!isLoggedin()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedin() {
        return br.getCookie(this.getHost(), "forum_session", Cookies.NOTDELETEDPATTERN) != null && br.getCookie(this.getHost(), "forum_user", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setConcurrentUsePossible(true);
        ai.setStatus("Registered (free) user");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        this.handleFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        /* 2020-05-27: No captchas at all */
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
