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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.http.Cookies;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DegooCom extends PluginForHost {
    public DegooCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("https://degoo.com/");
    }

    @Override
    public String getAGBLink() {
        return "https://app.degoo.com/";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "degoo.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://cloud\\." + buildHostsPatternPart(domains) + "/share/([A-Za-z0-9\\-_]+)\\?ID=(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private final boolean        FREE_RESUME                  = false;
    private final int            FREE_MAXCHUNKS               = 1;
    private final int            FREE_MAXDOWNLOADS            = 20;
    private static final boolean ACCOUNT_FREE_RESUME          = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    public static final String   PROPERTY_DIRECTURL           = "free_directlink";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getUniqueLinkIDFromURL(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getUniqueLinkIDFromURL(final DownloadLink link) {
        final String folderID = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        final String fileID = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
        if (folderID != null && fileID != null) {
            return folderID + "_" + fileID;
        } else {
            return null;
        }
    }

    private String              dllink   = null;
    private static final String API_BASE = "https://rest-api.degoo.com";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 400 });
        final String folderID = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        final String fileID = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
        if (folderID == null || fileID == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("HashValue", folderID);
        params.put("FileID", fileID);
        br.postPageRaw(API_BASE + "/overlay", JSonStorage.serializeToJson(params));
        /* 2021-01-17: HTTP 400: {"Error": "Not authorized!"} == File offline */
        if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String filename = (String) entries.get("Name");
        final int filesize = ((Integer) entries.get("Size")).intValue();
        this.dllink = (String) entries.get("URL");
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename);
        }
        if (filesize > 0) {
            link.setVerifiedFileSize(filesize);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, PROPERTY_DIRECTURL);
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (this.dllink == null) {
            logger.warning("Failed to find final downloadurl");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 429) {
                /**
                 * 2021-01-17: Plaintext response: "Rate Limit" </br>
                 * This limit sits on the files themselves and/or the uploader account. There is no way to bypass this by reconnecting!
                 * </br>
                 * Displayed error on website: "Daily limit reached, upgrade to increase this limit or wait until tomorrow"
                 */
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Daily limit reached");
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error");
            }
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting token login");
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        logger.info("Trust token without checking");
                        return false;
                    }
                    /* TODO */
                    // br.getPage("https://" + this.getHost() + "/");
                    // if (this.isLoggedin()) {
                    // logger.info("Cookie login successful");
                    // /* Refresh cookie timestamp */
                    // account.saveCookies(this.br.getCookies(this.getHost()), "");
                    // return true;
                    // } else {
                    // logger.info("Cookie login failed");
                    // }
                }
                logger.info("Performing full login");
                final boolean useAPI = true;
                if (useAPI) {
                    /* Login is possible with both requests */
                    br.postPageRaw(API_BASE + "/login", "{\"Username\":\"" + account.getUser() + "\",\"Password\":\"" + account.getPass() + "\",\"GenerateToken\":true}");
                    // br.postPageRaw(API_BASE + "/register", "{\"Username\":\"" + account.getUser() + "\",\"Password\":\"" +
                    // account.getPass() + "\",\"LanguageCode\":\"de-DE\",\"CountryCode\":\"DE\",\"Source\":\"Web
                    // App\",\"GenerateToken\":true}");
                    final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    final String refreshToken = (String) entries.get("RefreshToken");
                    final String token = (String) entries.get("Token");
                    if (StringUtils.isEmpty(refreshToken) || StringUtils.isEmpty(token)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    /* Response 400: {"Error": "Not authorized!"} */
                } else {
                    /* Unfinished code */
                    br.getPage("https://" + this.getHost() + "/me/login");
                    final Form loginform = br.getFormbyActionRegex(".*me/login");
                    if (loginform == null) {
                        logger.warning("Failed to find loginform");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginform.put("Email", Encoding.urlEncode(account.getUser()));
                    loginform.put("Password", Encoding.urlEncode(account.getPass()));
                    br.submitForm(loginform);
                    account.saveCookies(this.br.getCookies(this.getHost()), "");
                }
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        String space = br.getRegex("").getMatch(0);
        if (space != null) {
            ai.setUsedSpace(space.trim());
        }
        ai.setUnlimitedTraffic();
        // if (br.containsHTML("")) {
        // account.setType(AccountType.FREE);
        // account.setConcurrentUsePossible(false);
        // ai.setStatus("Registered (free) user");
        // } else {
        // final String expire = br.getRegex("").getMatch(0);
        // if (expire == null) {
        // throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        // } else {
        // ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
        // }
        // account.setType(AccountType.PREMIUM);
        // account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        // account.setConcurrentUsePossible(true);
        // ai.setStatus("Premium account");
        // }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        this.handleFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        /* 2020-07-21: No captchas at all */
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}