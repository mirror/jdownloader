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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class OboomIo extends PluginForHost {
    public OboomIo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + getHost() + "/plans");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        br.getHeaders().put(HTTPConstants.HEADER_REQUEST_USER_AGENT, "JDownloader");
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "oboom.io" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/file/([A-Za-z0-9_\\-]+)(/[^/#\\?]+)?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return false;
        } else if (AccountType.PREMIUM.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    /** Returns unique fileID. */
    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String getApiBase() {
        return "https://oboom.io/api/2.0";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, null);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        final UrlQuery query = new UrlQuery();
        query.add("id", this.getFID(link));
        br.postPage(getApiBase() + "/apiGetFileInfo/", query);
        final Map<String, Object> entries = this.handleErrors(br, account, link);
        link.setFinalFileName(entries.get("fileName").toString());
        link.setVerifiedFileSize(Long.parseLong(entries.get("fileSize").toString()));
        link.setMD5Hash(entries.get("fileMd5").toString());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        throw new AccountRequiredException();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    private Map<String, Object> login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            if (!force) {
                /* Do nothing */
                return null;
            }
            final UrlQuery query = new UrlQuery();
            query.add("email", Encoding.urlEncode(account.getUser()));
            query.add("pass", Encoding.urlEncode(account.getPass()));
            br.postPage(getApiBase() + "/apiGetUserInfo/", query);
            final Map<String, Object> usermap = this.handleErrors(br, account, null);
            return usermap;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final Map<String, Object> usermap = login(account, true);
        final String userPlan = usermap.get("userPlan").toString();
        final long trafficleft = ((Number) usermap.get("userTrafficLeft")).longValue();
        final long trafficmax = ((Number) usermap.get("userTraffic")).longValue();
        if (trafficleft > 0 && !StringUtils.equalsIgnoreCase(userPlan, "Free")) {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(this.getMaxSimultanPremiumDownloadNum());
        } else {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(this.getMaxSimultanFreeDownloadNum());
        }
        ai.setTrafficLeft(trafficleft);
        ai.setTrafficMax(trafficmax);
        ai.setStatus("Plan: " + usermap.get("userPlan"));
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        /* No need to check availablestatus of file again. If it is offline, API will return that status on download attempt. */
        // requestFileInformation(link, account);
        login(account, false);
        final UrlQuery query = new UrlQuery();
        query.add("id", this.getFID(link));
        query.add("email", Encoding.urlEncode(account.getUser()));
        query.add("pass", Encoding.urlEncode(account.getPass()));
        br.postPage(getApiBase() + "/apiGetDownloadFile/", query);
        final Map<String, Object> entries = this.handleErrors(br, account, link);
        final String timestampNextDownloadPossible = (String) entries.get("nextDownloadLink");
        if (timestampNextDownloadPossible != null) {
            final long timestampMillis = TimeFormatter.getMilliSeconds(timestampNextDownloadPossible, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            final long minWaitMillis = 1 * 60 * 1000;
            final long timeUntilNextDownloadPossibleMillis = timestampMillis - System.currentTimeMillis();
            final long finalWaitMillis = Math.max(minWaitMillis, timeUntilNextDownloadPossibleMillis);
            final String errortext = "Wait until next downloads can be started";
            if (account != null) {
                throw new AccountUnavailableException(errortext, finalWaitMillis);
            } else {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, errortext, finalWaitMillis);
            }
        }
        final String directurl = entries.get("downloadLink").toString();
        if (StringUtils.isEmpty(directurl)) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find final downloadlink");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, this.isResumeable(link, account), 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection();
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadlink did not lead to downloadable content");
            }
        }
        dl.startDownload();
    }

    private Map<String, Object> handleErrors(final Browser br, final Account account, final DownloadLink link) throws PluginException {
        Map<String, Object> entries = null;
        try {
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        } catch (final JSonMapperException ignore) {
            /* This should never happen. */
            final String errormsg = "Invalid API response";
            final long wait = 1 * 60 * 1000l;
            if (link != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormsg, wait);
            } else {
                throw new AccountUnavailableException(errormsg, wait);
            }
        }
        final String error = (String) entries.get("error");
        if (error == null) {
            /* No error */
            return entries;
        }
        if (error.equalsIgnoreCase("fileNotFound")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (error.equalsIgnoreCase("userNotFound")) {
            /* Login failure */
            throw new AccountInvalidException();
        } else {
            final String errormsg = "Unknown error: " + error;
            final long wait = 1 * 60 * 1000l;
            if (link == null) {
                throw new AccountUnavailableException(errormsg, wait);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormsg, wait);
            }
        }
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account != null) {
            return true;
        } else {
            /* without account its not possible to download any link for this host */
            return false;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}