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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datoid.cz", "pornoid.cz" }, urls = { "https?://(?:www\\.)?datoid\\.(?:cz|sk)/([A-Za-z0-9]+)(/([^/]+))?", "https?://(?:www\\.)?pornoid\\.(?:cz|sk)/([A-Za-z0-9]+)(/([^/]+))?" })
public class DatoidCz extends PluginForHost {
    public DatoidCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://datoid.cz/cenik");
        // Prevents server errors
        this.setStartIntervall(2 * 1000);
    }

    @Override
    public String getAGBLink() {
        return "http://datoid.cz/kontakty";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("datoid.sk/", "datoid.cz/"));
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        br = new Browser();
        this.setBrowserExclusive();
        final String linkid = this.getLinkID(link);
        final String filename_url = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
        boolean set_final_filename = true;
        String filename = null;
        String filesize = null;
        String downloadURL = link.getPluginPatternMatcher();
        String api_param_url = prepareApiParam_URL(downloadURL);
        boolean api_failed = false;
        final boolean trust_API = true;
        br.getPage("https://api.datoid.cz/v1/get-file-details?url=" + Encoding.urlEncode(api_param_url));
        if (fileIsOfflineAPI() && StringUtils.startsWithCaseInsensitive(downloadURL, "https://")) {
            downloadURL = downloadURL.replace("https://", "http://");
            api_param_url = prepareApiParam_URL(downloadURL);
            br.getPage("https://api.datoid.cz/v1/get-file-details?url=" + Encoding.urlEncode(api_param_url));
            if (fileIsOfflineAPI() && !trust_API) {
                /* Double-check - API fallback */
                api_failed = true;
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                brc.getPage(downloadURL);
                if (!brc.getURL().contains(linkid) || brc.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                filename = brc.getRegex("class=\"filename\">([^<>\"]+)<").getMatch(0);
                filesize = brc.getRegex("class=\"icon-size\"></i>([^<>\"]+)<").getMatch(0);
                if (filename != null) {
                    link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
                    if (filesize != null) {
                        link.setDownloadSize(SizeFormatter.getSize(filesize));
                    }
                }
            }
        }
        if (!api_failed) {
            if (fileIsOfflineAPI()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("\"error\":\"File is password protected\"")) {
                logger.info("Password protected links are not yet supported (via API)!");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = PluginJSonUtils.getJsonValue(br, "filename");
            filesize = PluginJSonUtils.getJsonValue(br, "filesize_bytes");
        }
        if (StringUtils.isEmpty(filename)) {
            /* Fallback */
            filename = filename_url;
            if (StringUtils.isEmpty(filename)) {
                /* Final fallback */
                filename = linkid;
            }
        }
        if (StringUtils.isEmpty(filename)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (set_final_filename) {
            link.setFinalFileName(filename);
        } else {
            link.setName(filename);
        }
        if (filesize != null && filesize.matches("\\d+")) {
            /* Filesize via API */
            link.setDownloadSize(Long.parseLong(filesize));
        } else if (filesize != null) {
            /* Filesize via website */
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    /* 2019-02-04: Basically a workaround as their API only accepts URLs with one of their (at least) 2 domains.F */
    private String prepareApiParam_URL(final String url_source) {
        final String curr_domain = Browser.getHost(url_source);
        return url_source.replace(curr_domain, "datoid.cz");
    }

    private boolean fileIsOfflineAPI() {
        return br.containsHTML("\"error\":\"(File not found|File was blocked|File was deleted)\"");
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getPluginPatternMatcher());
        if (br.containsHTML("<div class=\"bPopup free-popup file-on-page big-file\">")) {
            logger.info("Only downloadable by Premium Account holders");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        final String continue_url = br.getRegex("class=\"btn btn-large btn-download detail-download\" href=\"(/f/[^<>\"]+)\"").getMatch(0);
        if (continue_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(continue_url);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getPage(continue_url + "?request=1&_=" + System.currentTimeMillis());
        final String redirect = PluginJSonUtils.getJson(br, "redirect");
        if (!StringUtils.isEmpty(redirect)) {
            /* Redirect will lead to main-page and we don't want that! */
            // br.getPage(redirect);
            br.getPage("/detail/popup-download?code=" + getLinkID(downloadLink) + "&_=" + System.currentTimeMillis());
        }
        if (br.containsHTML("\"error\":\"IP in use\"")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        } else if (br.containsHTML("\"No anonymous free slots\"") || br.containsHTML("class=\"hidden free-slots-in-use\"") /* 2018-10-15 */) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available", 5 * 60 * 1000l);
        }
        // final int wait = Integer.parseInt(getJson("wait"));
        String dllink = PluginJSonUtils.getJsonValue(br, "download_link");
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* 2019-02-04: Waittime can be skipped */
        // sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String login(final Account account) throws Exception {
        br.setFollowRedirects(false);
        br.getPage("http://api.datoid.cz/v1/login?email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        try {
            if (br.containsHTML("\\{\"success\":false\\}")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            final String token = PluginJSonUtils.getJsonValue(br, "token");
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            account.setProperty("logintoken", token);
            return token;
        } catch (PluginException e) {
            account.removeProperty("logintoken");
            throw e;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        final String token;
        try {
            token = login(account);
        } catch (final PluginException e) {
            throw e;
        }
        br.getPage("http://api.datoid.cz/v1/get-user-details?token=" + token);
        /** 1 Credit = 1 MB */
        final String credits = PluginJSonUtils.getJsonValue(br, "credits");
        long trafficleft = 0;
        if (!StringUtils.isEmpty(credits)) {
            trafficleft = SizeFormatter.getSize(credits + " MB");
        }
        ai.setTrafficLeft(trafficleft);
        if (trafficleft > 0) {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
        } else {
            account.setType(AccountType.FREE);
            ai.setStatus("Free Account");
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (account.getType() == AccountType.FREE) {
            /* 2019-02-04: Basically download without account as free accounts usually don't have any traffic. */
            handleFree(link);
        } else {
            requestFileInformation(link);
            br.setFollowRedirects(false);
            final String token = account.getStringProperty("logintoken", null);
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            String downloadURL = link.getPluginPatternMatcher();
            String api_param_url = prepareApiParam_URL(downloadURL);
            br.getPage("http://api.datoid.cz/v1/get-download-link?token=" + token + "&url=" + Encoding.urlEncode(api_param_url));
            if (br.containsHTML("\"error\":\"File not found\"") && StringUtils.startsWithCaseInsensitive(downloadURL, "https://")) {
                /* Workaround */
                downloadURL = downloadURL.replace("https://", "http://");
                api_param_url = prepareApiParam_URL(downloadURL);
                br.getPage("http://api.datoid.cz/v1/get-download-link?token=" + token + "&url=" + Encoding.urlEncode(api_param_url));
            }
            if (br.containsHTML("\"error\":\"Lack of credits\"")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (fileIsOfflineAPI()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String dllink = PluginJSonUtils.getJsonValue(br, "download_link");
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, -3);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 5;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}