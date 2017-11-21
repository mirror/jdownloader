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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.net.HTTPHeader;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.logging.LogController;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.TwentyOneMembersVariantInfo;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "21members.com" }, urls = { "http://21members\\.com/dummy/file/\\d+" })
public class TwentyOneMembersCom extends PluginForHost {
    private final String LOGIN_ERROR_REGEX = "<ul.*?class=\"loginErrors\".*?>.*?<li class=\"warning\">(.+?)</li>";

    public TwentyOneMembersCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://21members.com/");
    }

    @Override
    public String getAGBLink() {
        return "http://21sextury.zendesk.com/entries/457783-pressplay-entertainment-legal-notices";
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        synchronized (account) {
            try {
                if (!login(br, account, -1)) {
                    final String error = br.getRegex(LOGIN_ERROR_REGEX).getMatch(0);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, error, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final AccountInfo ai = new AccountInfo();
                parseAccountInfo(account, ai);
                return ai;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    public boolean login(final Browser br, final Account account, final long trustCookiesAge) throws Exception {
        synchronized (account) {
            final boolean followRedirect = br.isFollowingRedirects();
            br.setFollowRedirects(true);
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trustCookiesAge) {
                        return true;
                    }
                    br.getPage("https://members.21members.com/en");
                    if (loggedinHTML(br)) {
                        /* Refresh cookie timestamp */
                        account.saveCookies(br.getCookies(getHost()), "");
                        return true;
                    }
                    account.clearCookies("");
                }
                br.getPage("https://www.21members.com/en/login");
                final String relCaptchaUrl = br.getRegex("<span>Auth code\\:</span><img src='([^']+)\' .*?alt='captcha'").getMatch(0);
                final Form login = br.getFormbyKey("username");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final DownloadLink before = getDownloadLink();
                try {
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "21members.com", "https://21members.com", true);
                    setDownloadLink(dummyLink);
                    if (relCaptchaUrl != null) {
                        // cookie appears after at least one bad login
                        final String code;
                        code = getCaptchaCode(br.getBaseURL() + relCaptchaUrl, dummyLink);
                        login.getInputField("code").setValue(Encoding.urlEncode(code));
                    } else {
                        /* 2017-11-21: New */
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                        login.put("g-recaptcha-response", recaptchaV2Response);
                    }
                } finally {
                    setDownloadLink(before);
                }
                login.setEncoding("application/x-www-form-urlencoded");
                login.getInputField("username").setValue(Encoding.urlEncode(account.getUser()));
                login.getInputField("password").setValue(Encoding.urlEncode(account.getPass()));
                Request request = br.createFormRequest(login);
                request.getHeaders().put(new HTTPHeader("Origin", "https://21members.com"));
                br.openRequestConnection(request);
                br.followConnection();
                final String error = br.getRegex(LOGIN_ERROR_REGEX).getMatch(0);
                if (error != null || !loggedinHTML(br)) {
                    return false;
                }
                account.saveCookies(br.getCookies(getHost()), "");
                return true;
            } finally {
                br.setFollowRedirects(followRedirect);
            }
        }
    }

    public boolean loggedinHTML(final Browser br) {
        return br.containsHTML("id=\"headerLinkLogout\"");
    }

    /**
     * @param account
     * @param ai
     * @return
     * @throws PluginException
     * @throws IOException
     */
    private void parseAccountInfo(Account account, AccountInfo ai) throws PluginException, IOException {
        final String[] daysLeft = br.getRegex("<span class=\"daysleft\">(\\d+)</span> Days left</span>").getColumn(0);
        int maxDaysLeft = -1;
        for (final String d : daysLeft) {
            final int left = Integer.parseInt(d);
            maxDaysLeft = Math.max(maxDaysLeft, left);
        }
        final String[] activeNetwork = br.getRegex("infoPanel (\\S+)  active").getColumn(0);
        final StringBuilder sb = new StringBuilder();
        for (final String network : activeNetwork) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(network);
        }
        ai.setStatus(sb.toString());
        if (maxDaysLeft >= 0) {
            ai.setValidUntil(System.currentTimeMillis() + (maxDaysLeft + 1) * (24 * 60 * 60 * 1000l));
        }
        ai.setUnlimitedTraffic();
    }

    @Override
    public List<TwentyOneMembersVariantInfo> getVariantsByLink(final DownloadLink downloadLink) {
        return downloadLink.getVariants(TwentyOneMembersVariantInfo.class);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        final TwentyOneMembersVariantInfo variant = getActiveVariantByLink(downloadLink);
        if (!TwentyOneMembersCom.login(br)) {
            return AvailableStatus.UNCHECKABLE;
        }
        final String id = downloadLink.getStringProperty("id");
        final List<TwentyOneMembersVariantInfo> variants;
        if (variant.isPhoto()) {
            br.getPage("http://21members.com/members/scene/photos/" + id + "?sidi=");
            if (isOffline(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            variants = TwentyOneMembersCom.parsePhotoVariants(br);
        } else {
            br.getPage("https://members." + this.getHost() + "/en/video////" + id);
            if (isOffline(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            variants = TwentyOneMembersCom.parseVideoVariants(br);
        }
        if (variants == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (variants != null) {
            br.setFollowRedirects(true);
            for (final TwentyOneMembersVariantInfo v : variants) {
                if (StringUtils.equals(v._getUniqueId(), variant._getUniqueId())) {
                    final String url = v.getUrl();
                    downloadLink.setProperty("url", url);
                    URLConnectionAdapter con = null;
                    try {
                        con = br.openHeadConnection(v.getUrl());
                        if (con.isOK() && con.isContentDisposition()) {
                            return AvailableStatus.TRUE;
                        }
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } finally {
                        if (con != null) {
                            con.disconnect();
                        }
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }

    @Override
    public TwentyOneMembersVariantInfo getActiveVariantByLink(final DownloadLink downloadLink) {
        return downloadLink.getVariant(TwentyOneMembersVariantInfo.class);
    }

    @Override
    public void setActiveVariantByLink(final DownloadLink downloadLink, final LinkVariant variant) {
        if (variant != null && variant instanceof TwentyOneMembersVariantInfo) {
            TwentyOneMembersCom.setVariant(downloadLink, (TwentyOneMembersVariantInfo) variant);
        }
    }

    /**
     * @param downloadLink
     * @param variant
     * @return
     */
    private static String formatFileName(final DownloadLink downloadLink, final TwentyOneMembersVariantInfo variant) {
        return downloadLink.getStringProperty("title") + "-" + variant._getName(downloadLink) + "." + variant._getExtension();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        final AvailableStatus status = requestFileInformation(link);
        if (AvailableStatus.UNCHECKABLE.equals(status)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getStringProperty("url"), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
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

    public static boolean login(final Browser br) {
        final List<Account> accs = AccountController.getInstance().getValidAccounts("21members.com");
        if (accs != null) {
            for (final Account ac : accs) {
                try {
                    final PluginForHost hostPlugin = JDUtilities.getPluginForHost("21members.com");
                    if (((TwentyOneMembersCom) hostPlugin).login(br, ac, 30000)) {
                        return true;
                    }
                } catch (final Exception e) {
                    LogController.CL().log(e);
                }
            }
        }
        return false;
    }

    public static ArrayList<TwentyOneMembersVariantInfo> parseVideoVariants(final Browser br) throws Exception {
        if (!br.containsHTML("buy it first")) {
            final String downloads = br.getRegex("<div class=\"optionList\">(.*?)</div>\\s*?</div>").getMatch(0);
            final String[] dlinfos = new Regex(downloads, "<p>(.*?)</p>").getColumn(0);
            final ArrayList<TwentyOneMembersVariantInfo> variantInfos = new ArrayList<TwentyOneMembersVariantInfo>();
            for (final String html : dlinfos) {
                String url = new Regex(html, "(/movieaction/download/\\d+/\\d+p/mp4)").getMatch(0);
                final String shorttype = new Regex(html, "(\\d+p)/").getMatch(0);
                final String filesize = new Regex(html, "class=\"movieSize\">([^<>\"]+)<").getMatch(0);
                if (url == null || shorttype == null) {
                    continue;
                }
                url = "https://members." + br.getHost() + url;
                if (filesize != null) {
                    variantInfos.add(new TwentyOneMembersVariantInfo(url, shorttype, filesize));
                } else {
                    variantInfos.add(new TwentyOneMembersVariantInfo(url, shorttype));
                }
            }
            return variantInfos;
        }
        return null;
    }

    public static void setVariant(final DownloadLink link, final TwentyOneMembersVariantInfo v) {
        final String filesize = v.getFilesize();
        link.setVariant(v);
        link.setLinkID(link.getPluginPatternMatcher() + "." + v._getUniqueId());
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        } else {
            link.setVerifiedFileSize(-1);
        }
        final String fileName = formatFileName(link, v);
        link.setFinalFileName(fileName);
    }

    public static ArrayList<TwentyOneMembersVariantInfo> parsePhotoVariants(final Browser br) throws Exception {
        if (!br.containsHTML("buy it first")) {
            final String downloads = br.getRegex("<!-- download -->(.*?)<!-- end download -->").getMatch(0);
            final String[][] downloadInfos = new Regex(downloads, "<a href=\"([^\"]+)\" class=\"([^\"]+)\">").getMatches();
            if (downloadInfos == null) {
                return null;
            }
            final ArrayList<TwentyOneMembersVariantInfo> variantInfos = new ArrayList<TwentyOneMembersVariantInfo>();
            for (final String[] downloadInfo : downloadInfos) {
                variantInfos.add(new TwentyOneMembersVariantInfo(downloadInfo[0], downloadInfo[1]));
            }
            return variantInfos;
        }
        return null;
    }
}
