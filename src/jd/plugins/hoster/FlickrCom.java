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

import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flickr.com" }, urls = { "http://(www\\.)?flickrdecrypted\\.com/photos/[^<>\"/]+/\\d+" }, flags = { 2 })
public class FlickrCom extends PluginForHost {

    public FlickrCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://flickr.com";
    }

    private static final Object LOCK     = new Object();
    private static final String MAINPAGE = "http://flickr.com";

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("flickrdecrypted.com/", "flickr.com/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        br.clearCookies(MAINPAGE);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            login(aa, false, br);
        } else {
            logger.info("File not checkable without logindata!");
            return AvailableStatus.UNCHECKABLE;
        }
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("div class=\"Four04Case\">")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = getFilename();
        if (filename == null) {
            logger.warning("Filename not found, plugin must be broken...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("(photo\\-div video\\-div|class=\"video\\-wrapper\"|<meta name=\"medium\" content=\"video\")")) {
            final String lq = createGuid();
            final String secret = br.getRegex("photo_secret=(.*?)\\&").getMatch(0);
            final String nodeID = br.getRegex("data\\-comment\\-id=\"(\\d+\\-\\d+)\\-").getMatch(0);
            if (secret == null || nodeID == null || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage("http://www.flickr.com/video_playlist.gne?node_id=" + nodeID + "&tech=flash&mode=playlist&lq=" + lq + "&bitrate=700&secret=" + secret + "&rd=video.yahoo.com&noad=1");
            final Regex parts = br.getRegex("<STREAM APP=\"(http://.*?)\" FULLPATH=\"(/.*?)\"");
            final String part1 = parts.getMatch(0);
            final String part2 = parts.getMatch(1);
            if (part1 == null || part2 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DLLINK = part1 + part2.replace("&amp;", "&");
            if (DLLINK.contains(".mp4"))
                filename += ".mp4";
            else
                filename += ".flv";
        } else {
            br.getPage(downloadLink.getDownloadURL() + "/in/photostream");
            DLLINK = getFinalLink();
            if (DLLINK == null) DLLINK = br.getRegex("\"(http://farm\\d+\\.(static\\.flickr|staticflickr)\\.com/\\d+/.*?)\"").getMatch(0);
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
            if (ext == null || ext.length() > 5) ext = ".jpg";
            filename = Encoding.htmlDecode(filename.trim() + ext);
        }
        downloadLink.setFinalFileName(filename);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for registered users");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, false, br);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Registered (free) User");
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    public void login(final Account account, final boolean force, Browser br) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                br.setCookie(MAINPAGE, "localization", "en-us%3Bde%3Bde");
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?>) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (cookies.containsKey("cookie_epass") && cookies.containsKey("cookie_accid") && account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        br.setCookie(MAINPAGE, "localization", "en-us%3Bde%3Bde");
                        Browser brc = br.cloneBrowser();
                        brc.getPage("http://www.flickr.com");
                        String global_dbid = brc.getRegex("global_dbid = '(\\d+)'").getMatch(0);
                        if (global_dbid != null && global_dbid.equals(cookies.get("cookie_accid"))) { return; }
                    }
                }
                br.setFollowRedirects(true);
                br.getPage("http://www.flickr.com/signin/");
                final String u = br.getRegex("type=\"hidden\" name=\"\\.u\" value=\"([^<>\"\\'/]+)\"").getMatch(0);
                final String challenge = br.getRegex("type=\"hidden\" name=\"\\.challenge\" value=\"([^<>\"\\'/]+)\"").getMatch(0);
                final String done = br.getRegex("type=\"hidden\" name=\"\\.done\" value=\"([^<>\"\\']+)\"").getMatch(0);
                final String pd = br.getRegex("type=\"hidden\" name=\"\\.pd\" value=\"([^<>\"\\'/]+)\"").getMatch(0);
                if (u == null || challenge == null || done == null || pd == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                br.postPage("https://login.yahoo.com/config/login", ".tries=1&.src=flickrsignin&.md5=&.hash=&.js=&.last=&promo=&.intl=us&.lang=en-US&.bypass=&.partner=&.u=" + u + "&.v=0&.challenge=" + Encoding.urlEncode(challenge) + "&.yplus=&.emailCode=&pkg=&stepid=&.ev=&hasMsgr=0&.chkP=Y&.done=" + Encoding.urlEncode(done) + "&.pd=" + Encoding.urlEncode(pd) + "&.ws=1&.cp=0&pad=15&aad=15&popup=1&login=" + Encoding.urlEncode(account.getUser()) + "&passwd=" + Encoding.urlEncode(account.getPass()) + "&.persistent=y&.save=&passwd_raw=");
                if (br.containsHTML("\"status\" : \"error\"")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                String stepForward = br.getRegex("\"url\" : \"(https?://[^<>\"\\']+)\"").getMatch(0);
                if (stepForward == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                br.getPage(stepForward);
                stepForward = br.getRegex("Please <a href=\"(http://(www\\.)?flickr\\.com/[^<>\"]+)\"").getMatch(0);
                if (stepForward == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                br.getPage(stepForward);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);

            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    private String createGuid() {
        String a = "";
        final String b = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._";
        int c = 0;
        while (c < 22) {
            final int index = (int) Math.floor(Math.random() * b.length());
            a = a + b.substring(index, index + 1);
            c++;
        }
        return a;
    }

    private String getFinalLink() {
        final String[] sizes = { "o", "k", "h", "l", "c", "z", "m", "n", "s", "t", "q", "sq" };
        String finallink = null;
        for (String size : sizes) {
            finallink = br.getRegex(size + ": \\{[\t\n\r ]+url: \\'(http://[^<>\"]*?)\\',[\t\n\r ]+").getMatch(0);
            if (finallink != null) break;
        }
        return finallink;
    }

    private String getFilename() {
        String filename = br.getRegex("<meta name=\"title\" content=\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"photo\\-title\">(.*?)</h1").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) \\| Flickr \\- (F|Ph)otosharing\\!</title>").getMatch(0);
            }
        }
        return filename;
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
