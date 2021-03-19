package jd.plugins.hoster;

import java.io.IOException;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.jdownloader.plugins.components.config.MetartConfig;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "metart.com", "sexart.com" }, urls = { "https?://(?:www\\.)?metart\\.com/api/download-media/[A-F0-9]{32}.+", "https?://(?:www\\.)?sexart\\.com/api/download-media/[A-F0-9]{32}.+" })
public class MetArtCom extends PluginForHost {
    public MetArtCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://signup.met-art.com/model.htm?from=homepage");
    }

    public static final String PROPERTY_UUID    = "uuid";
    public static final String PROPERTY_QUALITY = "quality";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String uuid = link.getStringProperty(PROPERTY_UUID);
        if (uuid != null) {
            String linkid = this.getHost() + "://" + uuid;
            if (link.hasProperty(PROPERTY_QUALITY)) {
                linkid += link.getStringProperty(PROPERTY_QUALITY);
            }
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public String getAGBLink() {
        return "http://guests.met-art.com/faq/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        /* URLs are added via crawler and will get checked there already. */
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
    public void resetDownloadlink(final DownloadLink link) {
    }

    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
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
        if (br.getURL() == null || !br.getURL().contains("/api/user-data")) {
            br.getPage("https://www." + account.getHoster() + "/api/user-data");
            getSetAccountType(account);
        }
        return ai;
    }

    private void getSetAccountType(final Account account) {
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("initialState");
        entries = (Map<String, Object>) entries.get("auth");
        entries = (Map<String, Object>) entries.get("user");
        final boolean isPremium = ((Boolean) entries.get("validSubscription")).booleanValue();
        if (isPremium) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
    }

    public void login(final Account account, final boolean verifyCredentials) throws PluginException, IOException {
        final Cookies cookies = account.loadCookies("");
        if (cookies != null) {
            logger.info("Attempting cookie login");
            br.setCookies(account.getHoster(), cookies);
            if (!verifyCredentials) {
                logger.info("Not verifying cookies");
                return;
            } else {
                br.getPage("https://www." + account.getHoster() + "/api/user-data");
                try {
                    getSetAccountType(account);
                    logger.info("Cookie login successful");
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return;
                } catch (final Throwable e) {
                    /* Not logged in = Different json -> Exception */
                    logger.info("Cookie login failed");
                    br.clearAll();
                }
            }
        }
        /* 2020-12-07: This way to login is not used anymore by the current version of the website but it is still working fine! */
        logger.info("Performing full login");
        br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
        br.setFollowRedirects(true);
        /*
         * 2021-03-16: TODO: Add support for their other portals/domains, visible only when logged in here:
         * https://account-new.metartnetwork.com/ (about 13 more websites)
         */
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
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 401) {
                throw new AccountUnavailableException("Session expired?", 5 * 60 * 1000l);
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                /* Should never happen as their URLs are static */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                /* Should never happen */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Directurl expired?");
            }
        }
        if (!link.isNameSet()) {
            link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        }
        dl.startDownload();
    }

    @Override
    public Class<? extends MetartConfig> getConfigInterface() {
        return MetartConfig.class;
    }
}
