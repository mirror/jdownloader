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

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "doci.pl" }, urls = { "" })
public class DociPl extends PluginForHost {
    public DociPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://doci.pl/");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.USERNAME_IS_EMAIL };
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        prepBR(br);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://doci.pl/regulations-pl";
    }

    private String getContentURL(final DownloadLink link) {
        /* Correction for links added up until revision 48815. */
        return link.getPluginPatternMatcher().replace("docidecrypted://", "https://");
    }

    public static Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 410 });
        return br;
    }

    public static boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return "docipl://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_FILE_ID);
    }

    public static final String PROPERTY_FILE_ID = "file_id";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        if (account != null) {
            this.login(account, false);
        }
        final String contenturl = this.getContentURL(link);
        if (!link.isNameSet()) {
            /* Fallback */
            final String url_filename = new Regex(contenturl, "[^:/]+://[^/]+/(.+)").getMatch(0).replace("/", "_") + ".pdf";
            link.setName(url_filename);
        }
        br.getPage(contenturl);
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        setDownloadlinkInformation(this.br, link);
        return AvailableStatus.TRUE;
    }

    public static void setDownloadlinkInformation(final Browser br, final DownloadLink link) {
        String filename = br.getRegex("class=\"content\"\\s*?><section><h1>([^<>\"]+)</h1>").getMatch(0);
        String filesize = br.getRegex("<td>\\s*Rozmiar\\s*:\\s*</td>\\s*<td>\\s*([^<>\"]+)\\s*<").getMatch(0);
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            link.setName(filename);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        final String documentID = getDocumentID(br);
        if (documentID != null) {
            link.setProperty(PROPERTY_FILE_ID, documentID);
        }
    }

    public static String getDocumentID(final Browser br) {
        String docid = br.getRegex("id=\"file\\-download\"[^<>]*?data\\-file\\-id=(\\d+)").getMatch(0);
        if (docid == null) {
            docid = br.getRegex("stream\\.[^/]+/pdf/(\\d+)").getMatch(0);
        }
        return docid;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        final String directlinkproperty = "directurl" + (account != null ? "_" + account.getType() : "");
        final String storedDirecturl = link.getStringProperty(directlinkproperty);
        String dllink = null;
        if (storedDirecturl != null) {
            logger.info("Re-using stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            /* Generate fresh directurl */
            /**
             * Important: File-ID is not visible in html code when logged in so we are not providing an account object for
             * requestFileInformation even if there is one. </br>
             * Instead we login later.
             */
            requestFileInformation(link, null);
            String fidStr = br.getRegex("data-file-id=(\\d+)").getMatch(0);
            if (fidStr == null) {
                fidStr = br.getRegex("data-item-id=\"(\\d+)\"").getMatch(0);
                if (fidStr == null) {
                    /* Fallback to stored ID */
                    fidStr = this.getFID(link);
                }
            }
            if (fidStr == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Login if account is present. */
            if (account != null) {
                this.login(account, false);
            }
            final int fid = Integer.parseInt(fidStr);
            final boolean useNewWay = true;
            if (useNewWay) {
                /* 2020-10-02 */
                final Map<String, Object> postData = new HashMap<String, Object>();
                postData.put("code", "");
                postData.put("download_from_dir", 0);
                postData.put("file_id", fid);
                postData.put("item_id", fid);
                postData.put("item_type", 1);
                postData.put("menu_visible", 0);
                postData.put("no_headers", 1);
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPageRaw("/download/payment_info", JSonStorage.serializeToJson(postData));
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                if (Boolean.FALSE.equals(entries.get("success"))) {
                    if (account == null) {
                        /* 2024-03-25: Account required to download files bigger than 1 MB. */
                        throw new AccountRequiredException("Account required to download bigger files");
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                final Map<String, Object> response = (Map<String, Object>) entries.get("response");
                final Map<String, Object> download_data = (Map<String, Object>) response.get("download_data");
                dllink = download_data.get("download_url").toString();
                final String time = download_data.get("time").toString();
                if (StringUtils.isEmpty(dllink) || StringUtils.isEmpty(time)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink += fidStr + "/" + time;
            } else {
                /* This seems to be needed to view a document on thei website - not (yet) useful for downloading! */
                final String rcKey = br.getRegex("data-rcp=\"([^\"]+)\"").getMatch(0);
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, rcKey).getToken();
                final Map<String, Object> postData = new HashMap<String, Object>();
                postData.put("file_id", fidStr);
                postData.put("file_size", link.getView().getBytesTotal());
                postData.put("file_extension", Plugin.getFileNameExtensionFromString(link.getName(), "mobi"));
                postData.put("rc", recaptchaV2Response);
                br.getHeaders().put("x-requested-with", "XMLHttpRequest");
                br.postPageRaw("/file/file_data/show", JSonStorage.serializeToJson(postData));
                dllink = PluginJSonUtils.getJson(br, "url");
                if (!StringUtils.isEmpty(dllink)) {
                    /* Check for: docs.google.com/viewer?embedded=true&url=http... */
                    final UrlQuery query = UrlQuery.parse(dllink);
                    final String embeddedURL = query.get("url");
                    if (!StringUtils.isEmpty(embeddedURL)) {
                        dllink = embeddedURL;
                    }
                }
            }
            /* 2020-09-21: Old way without captcha was easier */
            // dllink = String.format("http://stream.%s/pdf/%s", this.br.getHost(), docid);
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
                }
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        final String final_server_filename = getFileNameFromConnection(dl.getConnection());
        if (final_server_filename != null) {
            link.setFinalFileName(Encoding.htmlDecode(final_server_filename).trim());
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toExternalForm());
        dl.startDownload();
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setFollowRedirects(true);
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                logger.info("Attempting cookie login");
                this.br.setCookies(this.getHost(), cookies);
                if (!force) {
                    /* Don't validate cookies */
                    return false;
                }
                br.getPage("https://" + this.getHost() + "/");
                if (this.isLoggedin(br)) {
                    logger.info("Cookie login successful");
                    /* Refresh cookie timestamp */
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return true;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                }
            }
            logger.info("Performing full login");
            br.getPage("https://" + getHost());
            final Map<String, Object> loginmap = new HashMap<String, Object>();
            loginmap.put("email_login", account.getUser());
            loginmap.put("password_login", account.getPass());
            loginmap.put("provider_login", "");
            loginmap.put("remember_login", 1);
            br.postPageRaw("/account/signin_set", JSonStorage.serializeToJson(loginmap));
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            if (Boolean.FALSE.equals(entries.get("success"))) {
                String errormsg = null;
                final Map<String, Object> responsemap = (Map<String, Object>) entries.get("response");
                if (responsemap != null) {
                    errormsg = (String) responsemap.get("info");
                }
                throw new AccountInvalidException(errormsg);
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            return true;
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("btnLogout\"");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        login(account, true);
        final AccountInfo ai = new AccountInfo();
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
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
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}