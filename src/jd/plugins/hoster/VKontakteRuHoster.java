//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

//Links are coming from a decrypter
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vkontakte.ru" }, urls = { "http://vkontaktedecrypted\\.ru/(picturelink/(\\-)?\\d+_\\d+(\\?tag=\\d+)?|audiolink/\\d+|videolink/\\d+)" }, flags = { 2 })
public class VKontakteRuHoster extends PluginForHost {

    private static final String DOMAIN         = "http://vk.com";
    private static Object       LOCK           = new Object();
    private String              FINALLINK      = null;
    private final String        USECOOKIELOGIN = "USECOOKIELOGIN";
    private static final String AUDIOLINK      = "http://vkontaktedecrypted\\.ru/audiolink/\\d+";
    private static final String VIDEOLINK      = "http://vkontaktedecrypted\\.ru/videolink/\\d+";
    private int                 MAXCHUNKS      = 1;

    public VKontakteRuHoster(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://vk.com/help.php?page=terms";
    }

    @SuppressWarnings("unchecked")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        FINALLINK = null;
        this.setBrowserExclusive();

        br.setFollowRedirects(false);
        // Login required to check/download
        final Account aa = AccountController.getInstance().getValidAccount(this);
        // This shouldn't happen
        if (aa == null) {
            link.getLinkStatus().setStatusText("Only downlodable via account!");
            return AvailableStatus.UNCHECKABLE;
        }
        login(br, aa, false);
        if (link.getDownloadURL().matches(AUDIOLINK)) {
            String finalFilename = link.getFinalFileName();
            if (finalFilename == null) finalFilename = link.getName();
            final String audioID = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
            FINALLINK = link.getStringProperty("directlink", null);
            if (!linkOk(link, finalFilename)) {
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage("http://vk.com/audio", link.getStringProperty("postdata", null));
                FINALLINK = br.getRegex("\\'" + audioID + "\\',\\'(http://cs\\d+\\.[a-z0-9]+\\.[a-z]{2,4}/u\\d+/audios?/[a-z0-9]+\\.mp3)\\'").getMatch(0);
                if (FINALLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (!linkOk(link, finalFilename)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else if (link.getDownloadURL().matches(VIDEOLINK)) {
            MAXCHUNKS = 0;
            br.setFollowRedirects(true);

            br.getPage("http://vk.com/video_ext.php?oid=" + link.getStringProperty("userid", null) + "&id=" + link.getStringProperty("videoid", null) + "&hash=" + link.getStringProperty("embedhash", null));
            // final String brReplaced = br.toString().replace("\\", "");
            final String server = br.getRegex("var video_host = \\'(http://)?([^<>\"]*?)(/)?\\'").getMatch(1);
            final String uid = br.getRegex("var video_uid = \\'(\\d+)\\'").getMatch(0);
            final String vtag = br.getRegex("var video_vtag = \\'([a-z0-9\\-]+)\\'").getMatch(0);
            final String vkid = br.getRegex("\"vkid\":(\\d+)").getMatch(0);
            String filename = br.getRegex("var video_title = \\'([^<>\"]*?)\\';").getMatch(0);
            if (filename == null || server == null || uid == null || vtag == null || vkid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String quality = null;
            String inbetween = "u" + uid + "/videos/" + vtag;
            if (br.containsHTML("\"hd\":3")) {
                /** http://vk.com/video_ext.php?oid=220692&id=158972513&hash=73774f79545aa0be */
                quality = ".720.mp4";
            } else if (br.containsHTML("\"hd\":2")) {
                /** http://vk.com/video_ext.php?oid=220692&id=159139817&hash=77f796b5a24ff1ef */
                quality = ".480.mp4";
            } else if (br.containsHTML("\"hd\":1")) {
                quality = ".360.mp4";
            } else if (br.containsHTML("\"hd\":0,\"no_flv\":1")) {
                quality = ".240.mp4";
            } else if (br.containsHTML("\"no_flv\":0") && vtag.contains("-")) {
                /** http://vk.com/video_ext.php?oid=220692&id=126736025&hash=a3d2fb0c6daffa31 */
                quality = ".vk.flv";
                inbetween = "assets/video/" + vtag + vkid;
            } else if (br.containsHTML("\"no_flv\":0")) {
                /** http://vk.com/video_ext.php?oid=220692&id=137826312&hash=25c45b09a2660b33 */
                quality = ".flv";
            }
            filename = Encoding.htmlDecode(filename) + quality;
            FINALLINK = "http://" + server + "/" + inbetween + quality;
            if (!linkOk(link, filename)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            String albumID = link.getStringProperty("albumid");
            String photoID = new Regex(link.getDownloadURL(), "vkontaktedecrypted\\.ru/picturelink/((\\-)?\\d+_\\d+)").getMatch(0);
            if (albumID == null || photoID == null) {
                // This should never happen
                logger.warning("A property couldn't be found!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage("http://vk.com/photo" + photoID);
            /* seems we have to refesh the login process */
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            br.postPage("http://vk.com/al_photos.php", "act=show&al=1&module=photos&list=" + albumID + "&photo=" + photoID);
            final String correctedBR = br.toString().replace("\\", "");
            /**
             * Try to get best quality and test links till a working link is found as it can happen that the found link is offline but
             * others are online
             */
            final String[] qs = { "w_", "z_", "y_", "x_", "m_" };
            for (String q : qs) {
                /* large image */
                if (FINALLINK == null || (FINALLINK != null && !linkOk(link, null))) {
                    String base = new Regex(correctedBR, "\"id\":\"" + photoID + "\",\"base\":\"(http://.*?)\"").getMatch(0);
                    if (base == null) base = "";
                    String section = new Regex(correctedBR, "(\\{\"id\":\"" + photoID + "\",\"base\":\"" + base + ".*?)((,\\{)|$)").getMatch(0);
                    if (base != null) FINALLINK = new Regex(section, "\"id\":\"" + photoID + "\",\"base\":\"" + base + "\".*?\"" + q + "src\":\"(" + base + ".*?)\"").getMatch(0);
                } else {
                    break;
                }
            }
            if (FINALLINK == null) {
                logger.warning("Finallink is null!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return AvailableStatus.TRUE;

    }

    private boolean linkOk(final DownloadLink downloadLink, final String finalfilename) throws IOException {
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(FINALLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
                if (finalfilename == null)
                    downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                else
                    downloadLink.setFinalFileName(finalfilename);
            } else {
                return false;
            }
            return true;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_FATAL, "Download only possible with account!");
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        /**
         * Chunks disabled because (till now) this plugin only exists to download pictures
         */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, FINALLINK, true, MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            if (this.getPluginConfig().getBooleanProperty(USECOOKIELOGIN, false)) {
                logger.info("Logging in with cookies.");
                login(br, account, false);
                logger.info("Logged in successfully with cookies...");
            } else {
                logger.info("Logging in without cookies (forced login)...");
                login(br, account, true);
                logger.info("Logged in successfully without cookies (forced login)!");
            }
        } catch (PluginException e) {
            logger.info("Login failed!");
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(br, account, false);
        doFree(link);
    }

    @SuppressWarnings("unchecked")
    public void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:16.0) Gecko/20100101 Firefox/16.0");
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(DOMAIN, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                // Set english language
                br.setCookie(DOMAIN, "remixlang", "3");
                br.getPage("http://vk.com/login.php");
                String damnIPH = br.getRegex("name=\"ip_h\" value=\"(.*?)\"").getMatch(0);
                if (damnIPH == null) damnIPH = br.getRegex("\\{loginscheme: \\'https\\', ip_h: \\'(.*?)\\'\\}").getMatch(0);
                if (damnIPH == null) damnIPH = br.getRegex("loginscheme: \\'https\\'.*?ip_h: \\'(.*?)\\'").getMatch(0);
                if (damnIPH == null) {
                    logger.info("damnIPH String is null, marking account as invalid...");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.postPage("https://login.vk.com/", "act=login&success_url=&fail_url=&try_to_login=1&to=&vk=1&al_test=3&from_host=vk.com&from_protocol=http&ip_h=" + damnIPH + "&email=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&expire=");
                if (br.getCookie(DOMAIN, "remixsid") == null) {
                    logger.info("remixsid cookie is null, marking account as invalid...");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                // Finish login
                final Form lol = br.getFormbyProperty("name", "login");
                if (lol != null) {
                    lol.put("email", Encoding.urlEncode(account.getUser()));
                    lol.put("pass", Encoding.urlEncode(account.getPass()));
                    lol.put("expire", "0");
                    br.submitForm(lol);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(DOMAIN);
                for (final Cookie c : add.getCookies()) {
                    if ("deleted".equals(c.getValue())) continue;
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

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USECOOKIELOGIN, JDL.L("plugins.hoster.vkontakteruhoster.alwaysUseCookiesForLogin", "Always use cookies for login (this can cause out of date errors)")).setDefaultValue(false));
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}