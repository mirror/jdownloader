package jd.plugins.hoster;

import java.io.IOException;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "metart.com", "sexart.com" }, urls = { "https?://(?:www\\.)?metart\\.com/api/[a-z0-9]+/[A-F0-9]{32}\\.[a-z0-9]+", "https?://(?:www\\.)?sexart\\.com/api/[a-z0-9]+/[A-F0-9]{32}\\.[a-z0-9]+" })
public class MetArtCom extends PluginForHost {
    public MetArtCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://signup.met-art.com/model.htm?from=homepage");
    }

    @Override
    public String getAGBLink() {
        return "http://guests.met-art.com/faq/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        throw new AccountRequiredException();
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
        br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
        br.setFollowRedirects(true);
        final URLConnectionAdapter con = br.openGetConnection("https://members." + account.getHoster() + "/members/");
        if (con.getResponseCode() == 401) {
            throw new AccountUnavailableException("Session expired?", 5 * 60 * 1000l);
        } else {
            /* Multiple redirects */
            br.followConnection();
        }
        account.saveCookies(br.getCookies(br.getHost()), "");
    }

    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        /* TODO: Find a way to set cookies without the need to check them again prior to download */
        this.login(account, true);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 401) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Session expired?", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                /* Should never happen */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Directurl expired?");
            }
        }
        if (!link.isNameSet()) {
            link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        }
        dl.startDownload();
    }
}
