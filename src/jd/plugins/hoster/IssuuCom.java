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
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.IssuuComConfig;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "issuu.com" }, urls = { "https?://issuu\\.com/([a-z0-9\\-_\\.]+)/docs/([a-z0-9\\-_]+)" })
public class IssuuCom extends PluginForHost {
    public IssuuCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://issuu.com/signup");
    }

    @Override
    public String getAGBLink() {
        return "https://issuu.com/acceptterms";
    }

    private String             documentID           = null;
    public static final String PROPERTY_FINAL_NAME  = "finalname";
    public static final String PROPERTY_DOCUMENT_ID = "document_id";

    private String getUsername(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String getDocumentSlug(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
    }

    /** Using oembed API: http://developers.issuu.com/api/oembed.html */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        /* Typically this oembed API returns 501 for offline content */
        this.br.setAllowedResponseCodes(501);
        this.br.setFollowRedirects(true);
        final String filename = link.getStringProperty("finalname");
        if (filename != null) {
            link.setFinalFileName(filename);
        }
        this.br.getPage("https://issuu.com/oembed?format=json&url=" + Encoding.urlEncode(link.getPluginPatternMatcher()));
        if (this.br.getHttpConnection().getResponseCode() != 200) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (filename == null) {
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            link.setFinalFileName(entries.get("title") + ".pdf");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies userCookies = account.loadUserCookies();
                if (userCookies == null) {
                    showCookieLoginInfo();
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
                }
                this.br.setCookies(this.getHost(), userCookies);
                if (!force) {
                    /* Do not check cookies */
                    return;
                }
                br.setFollowRedirects(true);
                br.getPage("https://" + this.getHost() + "/home/publisher");
                if (isLoggedIn(br)) {
                    logger.info("User cookie login successful");
                    return;
                } else {
                    logger.info("User cookie login failed");
                    if (account.hasEverBeenValid()) {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                    } else {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                    }
                }
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIn(final Browser br) {
        if (br.containsHTML("data-track=\"logout\"")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        documentID = this.br.getRegex("\"thumbnail_url\":\"https?://image\\.issuu\\.com/([^<>\"/]*?)/").getMatch(0);
        if (documentID == null) {
            this.br.getPage(link.getPluginPatternMatcher());
            if (br.containsHTML(">We can\\'t find what you\\'re looking for") || this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            documentID = PluginJSonUtils.getJsonValue(br, "documentId");
        }
        if (documentID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (account != null) {
            login(account, true);
        }
        br.getPage("/call/document-page/document-download/" + getUsername(link) + "/" + this.getDocumentSlug(link));
        Map<String, Object> entries = null;
        try {
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        } catch (final JSonMapperException e) {
            if (br.getHttpConnection().getResponseCode() == 403) {
                /* No json response --> Document not downloadable --> Check for errormessage in plaintext */
                final String text = br.getRequest().getHtmlCode();
                if (text.length() <= 100 && br.getRequest().getResponseHeader("Content-Type").equalsIgnoreCase("text/plain")) {
                    /* E.g. "The publisher does not have the license to enable download" */
                    throw new PluginException(LinkStatus.ERROR_FATAL, text);
                } else {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Error 403: Document is not downloadable");
                }
            } else {
                throw e;
            }
        }
        final Number code = (Number) entries.get("code");
        final String message = (String) entries.get("message");
        if ("Download limit reached".equals(message) || (code != null && code.intValue() == 15)) {
            throw new AccountUnavailableException("Downloadlimit reached", 5 * 60 * 1000);
        }
        if ("Document access denied".equals(message)) {
            /* TODO: Find errorcode for this */
            throw new PluginException(LinkStatus.ERROR_FATAL, "This document is not downloadable");
        }
        final String dllink = (String) entries.get("url");
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // We have to wait here, otherwise we'll get an empty file!
        sleep(3 * 1000l, link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public Class<? extends IssuuComConfig> getConfigInterface() {
        return IssuuComConfig.class;
    }
}