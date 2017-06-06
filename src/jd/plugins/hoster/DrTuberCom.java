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

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
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
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "drtuber.com" }, urls = { "http://(www\\.|m\\.)?drtuber\\.com/(video/\\d+|player/config_embed3\\.php\\?vkey=[a-z0-9]+|embed/\\d+)" })
public class DrTuberCom extends PluginForHost {
    public DrTuberCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.drtuber.com/signup?track=top_menu");
    }

    /* Similar sites: drtuber.com, proporn.com, viptube.com, tubeon.com, winporn.com */
    /*
     * Last revision with old filecheck handling: 29195. Checking the directlinks inside requestFileInformation may lead to 404 errors and
     * offline links which are online via browser so better avoid that.
     */
    @Override
    public String getAGBLink() {
        return "http://www.drtuber.com/static/terms";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        String newlink = link.getDownloadURL().toLowerCase();
        newlink = newlink.replace("m.drtuber.com/", "drtuber.com/");
        link.setUrlDownload(newlink);
    }

    /* IMPORTANT: This can be used as a workaround if the normal handling fails and there is no time to fix it or it's not easily fixable... */
    private boolean              use_mobile                   = false;
    /*
     * Allow usage of uncrypted finallinks - quality might be lower than when using the complicated way but overall it might stability..
     */
    private boolean              allow_uncrypted_downloadlink = false;
    private static final String  normalUA                     = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:35.0) Gecko/20100101 Firefox/35.0";
    private static final String  mobileUA                     = "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus One Build/FRG83) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile";
    private final String         type_embed                   = "https?://(?:www\\.)?drtuber\\.com/embed/\\d+";
    private final String         type_normal                  = "https?://(?:www\\.)?drtuber\\.com/video/\\d+(?:/[a-z0-9\\-_]+)?";
    private String               DLLINK                       = null;
    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 0;
    private static final int     FREE_MAXDOWNLOADS            = 20;
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 0;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);

    private String getContinueLink(String fun) {
        if (fun == null) {
            return null;
        }
        fun = fun.replaceAll("s1\\.addVariable\\(\\'config\\',", "var result = ").replaceAll("params\\);", "params;");
        Object result = new Object();
        ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            engine.eval(fun);
            result = engine.get("result");
        } catch (Throwable e) {
            return null;
        }
        return result.toString();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException, InterruptedException {
        DLLINK = null;
        setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBR(this.br);
        br.setCookie("http://drtuber.com", "lang", "en");
        String continueLink = null, filename = null;
        // Check if link is an embedded link e.g. from a decrypter
        /* embed v3 */
        String vk = new Regex(downloadLink.getDownloadURL(), "vkey=(\\w+)").getMatch(0);
        if (vk != null) {
            logger.info("Accessing embedded video - trying to find original video URL");
            br.getPage(downloadLink.getDownloadURL() + "&pkey=" + JDHash.getMD5(vk + Encoding.Base64Decode("S0s2Mml5aUliWFhIc2J3")));
            if (br.containsHTML("Invalid video key\\!") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String original_video_link = br.getRegex("type=video_click\\&amp;target_url=(http.*?)</url>").getMatch(0);
            if (original_video_link == null) {
                logger.warning("Failed to find original link for: " + downloadLink.getDownloadURL());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setUrlDownload(Encoding.htmlDecode(original_video_link));
        }
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("This video was deleted") || br.getURL().contains("missing=true") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // No account support -> No support for private videos
        if (br.containsHTML("Sorry\\.\\.\\. Video is private")) {
            logger.info("Private video --> Unsupported --> Offline");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (use_mobile) {
            /* Basically a way to avoid all the crypto stuff but eventually gives you lower quality than the normal site. */
            final String fid = getFID(downloadLink);
            br.clearCookies("http://drtuber.com");
            br.clearCookies("http://www.drtuber.com");
            br.getHeaders().put("User-Agent", mobileUA);
            br.setCookie("http://drtuber.com", "lang", "en");
            br.setCookie("http://drtuber.com", "no_popups", "1");
            br.setCookie("http://drtuber.com", "no_ads", "1");
            br.setCookie("http://drtuber.com", "_gat", "1");
            br.setCookie("http://m.drtuber.com", "traffic_type", "3");
            br.setCookie("http://m.drtuber.com", "adv_show", "1");
            br.setCookie("http://m.drtuber.com", "dwnld_speed", "7.436709219858156");
            /*
             * There are 2 mobile versions: 'light' and 'full'. 'light' seems to be a low quality .3gp and 'full' a higher quality .mp4
             * though not necessarily as high as via the non-mobile site.
             */
            br.getPage("http://m.drtuber.com/video/" + fid + "/");
            filename = br.getRegex("<title>Download Free Mobile Porn \\- ([^<>\"]*?) \\- DrTuber\\.com</title>").getMatch(0);
            br.getPage("http://m.drtuber.com/play/" + fid + "?from=video_bottom");
            filename = br.getRegex("<title>Download Free Mobile Porn \\-([^<>\"]*?)\\- Download Preview \\- DrTuber\\.com</title>").getMatch(0);
            DLLINK = br.getRegex("\"(https?://[a-z0-9\\.\\-]+/(mp4|3gp)/[^<>\"]*?)\"").getMatch(0);
        } else {
            /* 2016-04-29: They're playing games with us with their embed html --> Avoid that! */
            if (downloadLink.getDownloadURL().matches(type_embed)) {
                String source_url = this.br.getRegex("target_url=(http[^<>\"\\'=\\&]+)").getMatch(0);
                if (source_url != null) {
                    source_url = Encoding.htmlDecode(source_url);
                }
                if (source_url != null && source_url.matches(type_normal)) {
                    downloadLink.setUrlDownload(source_url);
                    this.br.getPage(source_url);
                }
            }
            String vkey = null;
            /* Normal links */
            if (new Regex(downloadLink.getDownloadURL(), Pattern.compile(type_normal)).matches()) {
                filename = br.getRegex("<title>(.*?) \\- Free Porn.*?DrTuber\\.com</title>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<h1 class=\"name\">(.*?)</h1>").getMatch(0);
                }
                if (filename == null) {
                    filename = br.getRegex("class=\"hd_title\" style=\"text-align:left;\">([^<>\"]*?)</h1>").getMatch(0);
                }
                if (filename == null) {
                    filename = br.getRegex("<title>([^<>\"]*?) \\- \\d+ \\- DrTuber\\.com</title>").getMatch(0);
                }
                if (allow_uncrypted_downloadlink) {
                    DLLINK = getUncryptedFinallink();
                }
                if (DLLINK == null) {
                    final boolean new_handling = true;
                    if (new_handling) {
                        /*
                         * Very very very very bad js workaround
                         *
                         * IMPORTANT: If we find no other way to fix this in the future, switch to /embed/ links, old handling still works
                         * fine for them
                         */
                        continueLink = "http://www.drtuber.com/player_config/?";
                        final String[] params = br.getRegex("params \\+= ([^<>\"]*?);").getColumn(0);
                        for (String param : params) {
                            param = param.replace("'", "");
                            param = param.replace("+", "");
                            param = param.replace(" ", "");
                            param = Encoding.htmlDecode(param);
                            if (vkey == null) {
                                vkey = new Regex(param, "vkey=([a-z0-9]+)").getMatch(0);
                            }
                            continueLink += Encoding.htmlDecode(param);
                        }
                        if (vkey != null) {
                            continueLink += "&pkey=" + JDHash.getMD5(vkey + Encoding.Base64Decode("UFQ2bDEzdW1xVjhLODI3"));
                        }
                        continueLink += "&aid=&domain_id=";
                    } else {
                        continueLink = getContinueLink(br.getRegex("(var configPath.*?addVariable\\(\\'config\\',.*?;)").getMatch(0));
                        vkey = new Regex(continueLink, "vkey=(\\w+)").getMatch(0);
                        if (continueLink == null || vkey == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        if (!continueLink.startsWith("http://")) {
                            continueLink = "http://drtuber.com" + Encoding.htmlDecode(continueLink) + "&pkey=" + JDHash.getMD5(vkey + Encoding.Base64Decode("UFQ2bDEzdW1xVjhLODI3"));
                        } else {
                            continueLink = Encoding.htmlDecode(continueLink) + "&pkey=" + JDHash.getMD5(vkey + Encoding.Base64Decode("UFQ2bDEzdW1xVjhLODI3"));
                        }
                    }
                }
            } else if (downloadLink.getDownloadURL().matches("https?://(?:www\\.)?drtuber\\.com/embed/\\d+")) {
                /* embed v4 */
                /* 2016-04-29: They're playing games with us with their embed html ... */
                String nextUrl = br.getRegex("config=(http%3A%2F%2F(www\\.)?drtuber\\.com%2Fplayer_config%2F[^<>\"]*?)\"").getMatch(0);
                if (nextUrl == null) {
                    String[] hashEncValues = br.getRegex("flashvars=\"id_video=(\\d+)\\&t=(\\d+)").getRow(0);
                    if (hashEncValues == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    continueLink = "/player/config_embed4.php?id_video=" + hashEncValues[0] + "&t=" + hashEncValues[1] + "&pkey=" + JDHash.getMD5(hashEncValues[0] + hashEncValues[1] + Encoding.Base64Decode("RXMxaldDemZOQmRsMlk4"));
                } else {
                    nextUrl = Encoding.htmlDecode(nextUrl);
                    vk = new Regex(nextUrl, "vkey=(\\w+)").getMatch(0);
                    continueLink = nextUrl + "&pkey=" + JDHash.getMD5(vk + Encoding.Base64Decode("UFQ2bDEzdW1xVjhLODI3"));
                }
                filename = br.getRegex("<title>(.*?)\\s+\\-\\s+Free Porn Videos").getMatch(0);
            }
            if (DLLINK == null) {
                if (continueLink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getPage(continueLink);
                DLLINK = br.getRegex("<video_file>(<\\!\\[CDATA\\[)?(http://.*?)(\\]\\]>)?</video_file>").getMatch(1);
            }
        }
        if (filename == null || DLLINK == null) {
            if (br.containsHTML("<video_file><\\!\\[CDATA\\[\\]\\]></video_file>")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        DLLINK = Encoding.htmlDecode(DLLINK.trim());
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        String ext;
        if (DLLINK.contains(".flv")) {
            ext = ".flv";
        } else if (DLLINK.contains(".mp4")) {
            ext = ".mp4";
        } else {
            /* Should usually not happen. */
            ext = ".3gp";
        }
        downloadLink.setProperty("ftitle", filename);
        downloadLink.setProperty("fext", ext);
        downloadLink.setName(filename + ext);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, FREE_RESUME, FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            handleServerErrors();
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String ftitle_saved = downloadLink.getStringProperty("ftitle", null);
        final String fext_saved = downloadLink.getStringProperty("fext", null);
        downloadLink.setFinalFileName(ftitle_saved + fext_saved);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
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

    private static final String MAINPAGE = "http://drtuber.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBR(this.br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getPage("http://www.drtuber.com/ajax/popup_forms?form=login");
                br.postPage("/ajax/login", "submit_login=true&login_remember=true&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "remember") == null || !br.containsHTML("\"success\":true")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /* Only free accounts supported so far. */
                account.setProperty("free", true);
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

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        if (account.getBooleanProperty("free", false)) {
            maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
            try {
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(maxPrem.get());
                /* Free accounts can't have captchas! */
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            ai.setStatus("Registered (free) user");
        } else {
            /* Not yet supported... */
            final String expire = br.getRegex("").getMatch(0);
            if (expire == null) {
                final String lang = System.getProperty("user.language");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder nicht unterstützter Account Typ!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or unsupported account type!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
            }
            maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            try {
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            ai.setStatus("Premium Account");
        }
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        final String ftitle_saved = link.getStringProperty("ftitle", null);
        requestFileInformation(link);
        /* We don't need the old cookies anymore... */
        this.br = new Browser();
        login(account, false);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (account.getBooleanProperty("free", false)) {
            /* Try not to waste new link generation as we can only download 3 videos a day... */
            DLLINK = this.checkDirectLink(link, "account_free_directlink");
            if (DLLINK == null) {
                DLLINK = getUncryptedFinallink();
            }
            if (DLLINK == null) {
                /* Only 'use the download button' if we cannot find any stream URL as filesizes/video quality should be the same. */
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getPage("http://www.drtuber.com/video/download/" + getFID(link));
                if (br.containsHTML("you have reached the download limit")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Daily downloadlimit reached!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                DLLINK = "http://www.drtuber.com/video/download/save/" + getFID(link);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                handleServerErrors();
                br.followConnection();
                if (br.containsHTML("You have exceeded free downloads count")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Daily downloadlimit reached!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setFinalFileName(ftitle_saved + ".mp4");
            link.setProperty("account_free_directlink", dl.getConnection().getURL().toString());
            dl.startDownload();
        } else {
            DLLINK = this.checkDirectLink(link, "premium_directlink");
            if (DLLINK == null) {
                DLLINK = br.getRegex("").getMatch(0);
                if (DLLINK == null) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                handleServerErrors();
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setFinalFileName(ftitle_saved + ".mp4");
            link.setProperty("premium_directlink", dl.getConnection().getURL().toString());
            dl.startDownload();
        }
    }

    private String getUncryptedFinallink() {
        return br.getRegex("<source src=\"(https?://[^<>\"]*?)\" type=\"video/mp4\"").getMatch(0);
    }

    private void handleServerErrors() throws PluginException {
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
        }
    }

    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "(?:embed|video)/(\\d+)$").getMatch(0);
    }

    private void prepBR(final Browser br) {
        br.getHeaders().put("User-Agent", normalUA);
        br.getHeaders().put("Accept-Language", "de,en-US;q=0.7,en;q=0.3");
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.UnknownPornScript9;
    }
}