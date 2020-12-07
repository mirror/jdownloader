package jd.plugins.hoster;

import java.io.IOException;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "metart.com", "sexart.com" }, urls = { "https?://members\\.met-art\\.com/members/(media/.+|movie\\.php.+|movie\\.mp4.+|zip\\.php\\?zip=[A-Z0-9]+\\&type=(high|med|low))|decryptedmetartcom://.+", "decryptedsexartcom://.+" })
public class MetArtCom extends PluginForHost {
    public MetArtCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://signup.met-art.com/model.htm?from=homepage");
    }

    @Override
    public String getAGBLink() {
        return "http://guests.met-art.com/faq/";
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("decrypted[a-z0-9]+://", "https://"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        String name = new Regex(link.getDownloadURL(), "file=([^\\&]+)").getMatch(0);
        if (link.getDownloadURL().contains("/media/")) {
            name = new Regex(link.getDownloadURL(), "/media/.*?/[A-F0-9]+/(.+)").getMatch(0);
        } else if (link.getDownloadURL().contains("movie.php")) {
            name = new Regex(link.getDownloadURL(), "movie\\.php.+?file=(.*?)($|&)").getMatch(0);
        } else if (link.getDownloadURL().contains("movie.mp4")) {
            name = new Regex(link.getDownloadURL(), "movie\\.mp4.+?file=(.*?)($|&)").getMatch(0);
        } else if (link.getDownloadURL().contains("zip.php")) {
            name = new Regex(link.getDownloadURL(), "zip\\.php\\?zip=([A-Z0-9]+)\\&").getMatch(0);
        }
        if (name == null) {
            name = "Unknown Filename";
        }
        String type = new Regex(link.getDownloadURL(), "movie\\.(php|mp4).*?type=(.*?)&").getMatch(1);
        if (link.getDownloadURL().contains("zip.php")) {
            type = ".zip";
        }
        if (type != null) {
            if ("avi".equalsIgnoreCase(type)) {
                name = name + ".avi";
            } else if ("wmv".equalsIgnoreCase(type)) {
                name = name + ".wmv";
            } else if ("mpg".equalsIgnoreCase(type)) {
                name = name + ".mpg";
            } else if (".zip".equalsIgnoreCase(type)) {
                name = name + type;
            } else {
                name = name + "-" + type + ".mp4";
            }
        }
        link.setName(name);
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        this.login(account, true);
        ai.setStatus("Account valid");
        return ai;
    }

    public void login(final Account account, final boolean verifyCredentials) throws PluginException, IOException {
        br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
        if (!verifyCredentials) {
            logger.info("Trust credentials without check");
            return;
        }
        final Cookies cookies = account.loadCookies("");
        if (cookies != null) {
            logger.info("Attempting cookie login");
            br.setCookies(account.getHoster(), cookies);
            // br.setCookies(cookies);
            if (!verifyCredentials) {
                logger.info("Not verifying cookies");
                return;
            } else {
                br.getPage("https://www." + account.getHoster() + "/api/user-data");
                final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final boolean loggedIN = ((Boolean) entries.get("user")).booleanValue();
                if (loggedIN) {
                    logger.info("Cookie login successful");
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return;
                } else {
                    logger.info("Cookie login failed");
                }
            }
        }
        /* 2020-12-07: This way to login is not used anymore by the current version of the website but it is still working fine! */
        logger.info("Performing full login");
        br.setFollowRedirects(true);
        final URLConnectionAdapter con = br.openGetConnection("https://members." + account.getHoster() + "/members/");
        if (con.getResponseCode() == 401) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            /* Multiple redirects */
            br.followConnection();
        }
        account.saveCookies(br.getCookies(br.getHost()), "");
    }

    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        this.login(account, false);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), true, 0);
        if (dl.getConnection().getResponseCode() == 401) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Session expired?", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Directurl expired?");
        }
        if (!link.isNameSet()) {
            link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        }
        dl.startDownload();
    }
}
