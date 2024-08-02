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

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "eroprofile.com" }, urls = { "https?://(?:www\\.)?eroprofile\\.com/m/(?:videos|photos)/view/([A-Za-z0-9\\-_]+)" })
public class EroProfileCom extends PluginForHost {
    public EroProfileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    /* Porn_plugin */
    private String dllink = null;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public void setBrowser(Browser br) {
        this.br = br;
        br.setCookie(this.getHost(), "lang", "en");
    }

    @Override
    public String getAGBLink() {
        return "http://www.eroprofile.com/p/help/termsOfUse";
    }

    private static final String VIDEOLINK = "(?i)https?://(www\\.)?eroprofile\\.com/m/videos/view/[A-Za-z0-9\\-_]+";

    public static boolean isAccountRequired(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 403) {
            return true;
        } else if (br.containsHTML("(>\\s*You do not have the required privileges to view this page|>\\s*No access\\s*<|>\\s*Access denied)")) {
            return true;
        } else {
            return false;
        }
    }

    public static String getGenericErrorMessage(final Browser br) {
        String lastResortErrormessage = br.getRegex("<div class=\"boxCnt message-text-box\"[^>]*>([^<]+)</div>").getMatch(0);
        if (lastResortErrormessage == null) {
            return null;
        }
        lastResortErrormessage = Encoding.htmlDecode(lastResortErrormessage).trim();
        return lastResortErrormessage;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        br.setFollowRedirects(true);
        br.setReadTimeout(3 * 60 * 1000);
        br.getPage(link.getDownloadURL());
        if (isAccountRequired(br)) {
            return AvailableStatus.TRUE;
        }
        final String fid = this.getFID(link);
        final String regexAlbumNotFound = ">\\s*Album not found";
        final String extDefault;
        String filename;
        if (link.getDownloadURL().matches(VIDEOLINK)) {
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(>\\s*Video not found|>\\s*The video could not be found|<title>\\s*EroProfile</title>)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(">\\s*Video processing failed")) {
                /* <h1 class="capMultiLine">Video processing failed</h1> */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(regexAlbumNotFound)) {
                /* 2023-12-28: E.g. https://www.eroprofile.com/m/videos/view/3-sisters */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = getFilename(br);
            if (filename == null) {
                /* Fallback */
                filename = fid;
            }
            if (isAccountRequired(br)) {
                link.setName(filename + ".m4v");
                link.getLinkStatus().setStatusText("This file is only available to premium members");
                return AvailableStatus.TRUE;
            }
            dllink = br.getRegex("file\\s*:\\s*\\'(https?://[^<>\"]*?)\\'").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<source\\s*src\\s*=\\s*(?:'|\")([^<>\"\\']*?)/?(?:'|\")").getMatch(0);
            }
            extDefault = ".m4v";
            final String ext = getFileNameExtensionFromString(dllink, extDefault);
            link.setFinalFileName(filename + ext);
        } else {
            if (br.containsHTML("(>\\s*Photo not found|>\\s*The photo could not be found|<title>\\s*EroProfile\\s*</title>)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(regexAlbumNotFound)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = getFilename(br);
            if (filename == null) {
                /* Fallback */
                filename = fid;
            }
            dllink = br.getRegex("<\\s*div\\s+class=\"viewPhotoContainer\">\\s*<\\s*a\\s+href=\"((?:https?:)?//[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("class\\s*=\\s*\"photoPlayer\"\\s*src\\s*=\\s*\"((?:https?:)?//[^<>\"]*?)\"").getMatch(0);
            }
            extDefault = ".jpg";
            final String ext = getFileNameExtensionFromString(dllink, extDefault);
            link.setFinalFileName(filename + ext);
        }
        if (dllink != null) {
            dllink = Encoding.htmlOnlyDecode(dllink);
        }
        final boolean isDownload = Thread.currentThread() instanceof SingleDownloadController;
        if (dllink != null && !isDownload) {
            this.basicLinkCheck(br, br.createHeadRequest(this.dllink), link, filename, extDefault);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        handleDownload(link);
    }

    public void handleDownload(final DownloadLink link) throws Exception {
        if (isAccountRequired(br)) {
            throw new AccountRequiredException();
        } else if (dllink == null) {
            final String lastResortErrormessage = getGenericErrorMessage(br);
            if (lastResortErrormessage != null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, lastResortErrormessage);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        // Resume & chunks works but server will only send 99% of the data if used
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(account.getHoster(), cookies);
                    return;
                }
                br.getHeaders().put("Accept-Language", "en-us,en;q=0.5");
                br.setCookie("https://eroprofile.com/", "lang", "en");
                br.setFollowRedirects(false);
                br.getHeaders().put("X_REQUESTED_WITH", "XMLHttpRequest");
                br.postPage("https://www." + account.getHoster() + "/ajax_v1.php", "p=profile&a=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (!isLoggedin(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.getCookie(br.getHost(), "memberID", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        login(br, account, true);
        ai.setUnlimitedTraffic();
        ai.setStatus("Free Account");
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(br, account, false);
        br.setFollowRedirects(false);
        requestFileInformation(link);
        handleDownload(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public static String getFilename(Browser br) throws PluginException {
        String filename = br.getRegex("<tr><th>Title:</th><td>([^<>\"]*?)</td></tr>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(?:EroProfile\\s*-\\s*)?([^<>\"]*?)(?:\\s*-\\s*EroProfile\\s*)?</title>").getMatch(0);
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename.trim());
        }
        return filename;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
