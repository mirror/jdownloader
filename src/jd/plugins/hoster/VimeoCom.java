//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vimeo.com" }, urls = { "http://(www\\.)?vimeo\\.com/\\d+" }, flags = { 2 })
public class VimeoCom extends PluginForHost {

    private static final String MAINPAGE = "http://vimeo.com";
    static private final String AGB      = "http://www.vimeo.com/terms";
    private String              clipData;
    private String              finalURL;
    private static final Object LOCK     = new Object();

    public VimeoCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://vimeo.com/join");
    }

    public void doFree(final DownloadLink downloadLink) throws Exception {
        if (!"FREE".equals(downloadLink.getStringProperty("LASTTYPE", "FREE"))) {
            downloadLink.setProperty("LASTTYPE", "FREE");
            downloadLink.setChunksProgress(null);
            downloadLink.setDownloadSize(0);
        }
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalURL, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Set the final filename here because downloads via account have other
        // extensions
        downloadLink.setFinalFileName(downloadLink.getName());
        downloadLink.setProperty("LASTTYPE", "FREE");
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        synchronized (LOCK) {
            final AccountInfo ai = new AccountInfo();
            if (!new Regex(account.getUser(), ".*?@.*?\\..+").matches()) {
                account.setProperty("cookies", null);
                account.setValid(false);
                ai.setStatus("Invalid email address");
                return ai;

            }
            try {
                login(account, true);
            } catch (final PluginException e) {
                account.setProperty("cookies", null);
                account.setValid(false);
                return ai;
            }
            br.getPage("http://vimeo.com/settings");
            final String type = br.getRegex("acct_status\">.*?>(.*?)<").getMatch(0);
            if (type != null) {
                ai.setStatus(type);
            } else {
                ai.setStatus(null);
            }
            account.setValid(true);
            ai.setUnlimitedTraffic();
            return ai;
        }
    }

    @Override
    public String getAGBLink() {
        return AGB;
    }

    private String getClipData(final String tag) {
        return new Regex(clipData, "<" + tag + ">(.*?)</" + tag + ">").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        if (!"FREE".equals(downloadLink.getStringProperty("LASTTYPE", "FREE"))) {
            downloadLink.setProperty("LASTTYPE", "FREE");
            downloadLink.setChunksProgress(null);
            downloadLink.setDownloadSize(0);
        }
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (!"PREMIUM".equals(link.getStringProperty("LASTTYPE", "FREE"))) {
            link.setProperty("LASTTYPE", "PREMIUM");
            link.setChunksProgress(null);
            link.setDownloadSize(0);
        }
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("\">Sorry, not available for download")) {
            logger.info("No download available for link: " + link.getDownloadURL() + " , downloading as unregistered user...");
            doFree(link);
            return;
        }
        String dllink = br.getRegex("class=\"download\">[\t\n\r ]+<a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(/?download/video:\\d+\\?v=\\d+\\&e=\\d+\\&h=[a-z0-9]+\\&uh=[a-z0-9]+)\"").getMatch(0);
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!dllink.startsWith("/")) {
            dllink = MAINPAGE + "/" + dllink;
        } else {
            dllink = MAINPAGE + dllink;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String oldName = link.getName();
        final String newName = getFileNameFromHeader(dl.getConnection());
        final String name = oldName.substring(0, oldName.lastIndexOf(".")) + newName.substring(newName.lastIndexOf("."));
        link.setName(name);
        link.setProperty("LASTTYPE", "ACCOUNT");
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                setBrowserExclusive();
                br.setFollowRedirects(true);
                br.setDebug(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = account.getUser().matches(account.getStringProperty("name", account.getUser()));
                if (acmatch) {
                    acmatch = account.getPass().matches(account.getStringProperty("pass", account.getPass()));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (cookies.containsKey("vimeo") && account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.getPage(MAINPAGE);
                br.getPage(MAINPAGE + "/log_in");
                final String token = br.getRegex("name=\"token\" value=\"(.*?)\"").getMatch(0);
                if (token == null) {
                    account.setProperty("cookies", null);
                    logger.warning("Login is broken!");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /* important, else we get a 401 */
                br.setCookie(MAINPAGE, "xsrft", token);
                if (!new Regex(account.getUser(), ".*?@.*?\\..+").matches()) {
                    account.setProperty("cookies", null);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.postPage(MAINPAGE + "/log_in", "sign_in%5Bemail%5D=" + Encoding.urlEncode(account.getUser()) + "&sign_in%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()) + "&token=" + Encoding.urlEncode(token));
                if (br.getCookie(MAINPAGE, "vimeo") == null) {
                    account.setProperty("cookies", null);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", account.getUser());
                account.setProperty("pass", account.getPass());
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL() + "?hd=1");
        if (br.containsHTML(">Page not found on Vimeo<")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (br.containsHTML(">This is a private video<") && br.containsHTML("Do you have permission to watch this video\\?")) { throw new PluginException(LinkStatus.ERROR_FATAL, "This is a private video. Do you have no permission to watch this video!"); }
        final String clipID = br.getRegex("targ_clip_id:   (\\d+)").getMatch(0);
        clipData = br.getPage("/moogaloop/load/clip:" + clipID + "/local?param_force_embed=0&param_clip_id=" + clipID + "&param_show_portrait=0&param_multimoog=&param_server=vimeo.com&param_show_title=0&param_autoplay=0&param_show_byline=0&param_color=00ADEF&param_fullscreen=1&param_md5=0&param_context_id=&context_id=null");
        String title = getClipData("caption");
        String clipId = getClipData("clip_id");
        if (clipId == null) {
            clipId = getClipData("nodeId");
        }
        final String dlURL = "/moogaloop/play/clip:" + clipId + "/" + getClipData("request_signature") + "/" + getClipData("request_signature_expires") + "/?q=" + (getClipData("isHD").equals("1") ? "hd" : "sd");
        br.setFollowRedirects(false);
        br.getPage(dlURL);
        finalURL = br.getRedirectLocation();
        if (finalURL == null || title == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        title = Encoding.htmlDecode(title);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(finalURL);
            if (con.getContentType() != null && con.getContentType().contains("mp4")) {
                downloadLink.setName(title + ".mp4");
            } else {
                downloadLink.setName(title + ".flv");
            }
            if ("FREE".equals(downloadLink.getStringProperty("LASTTYPE", "FREE"))) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        /* reset downloadtype back to free */
        link.setProperty("LASTTYPE", "FREE");
    }

    @Override
    public void resetPluginGlobals() {
    }

}