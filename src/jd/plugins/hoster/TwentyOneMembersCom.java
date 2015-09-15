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

import javax.swing.JComponent;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountError;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.TwntnMmbrsCm.TwentyOneMembersVariantInfo;
import jd.utils.JDUtilities;

import org.appwork.uio.UIOManager;
import org.appwork.utils.Hash;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.HTTPHeader;
import org.jdownloader.controlling.linkcrawler.LinkVariant;

@HostPlugin(revision = "$Revision: 28758 $", interfaceVersion = 3, names = { "21members.com" }, urls = { "http://21members\\.com/dummy/file/\\d+" }, flags = { 0 })
public class TwentyOneMembersCom extends PluginForHost {

    private static final String LOGIN_ERROR_REGEX = "<ul.*?class=\"loginErrors\".*?>.*?<li class=\"warning\">(.+?)</li>";

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
        AccountInfo ai = new AccountInfo();
        login(br, account);
        String error = br.getRegex(LOGIN_ERROR_REGEX).getMatch(0);
        if (error != null) {
            ai.setStatus("Login Failed: " + error);
            account.setError(AccountError.INVALID, "Login Failed: " + error);

        }
        parseAccountInfo(account, ai);

        return ai;
    }

    public boolean login(Browser br, Account account) throws Exception {

        br.setFollowRedirects(false);
        String cookieCacheID = Hash.getSHA256(account.getUser() + "." + account.getPass());

        br.forceDebug(true);
        br.clearCookies("http://21members.com/login");
        Cookies cookies = account.loadCookies(cookieCacheID);
        if (cookies != null) {
            br.setCookies(getHost(), cookies);
            br.getPage("http://21members.com/members/home/");
            String error = br.getRegex(LOGIN_ERROR_REGEX).getMatch(0);
            if (error == null && br.getRedirectLocation() == null) {
                return true;
            }

        }
        br.getPage("http://21members.com/login");

        String relCaptchaUrl = br.getRegex("<span>Auth code\\:</span><img src='([^']+)\' .*?alt='captcha'").getMatch(0);

        Form login = br.getFormBySubmitvalue("Login");
        if (relCaptchaUrl != null) {
            // cookie appears after at least one bad login
            final DownloadLink dummyLink = new DownloadLink(this, "Account", "21members.com", "http://21members.com", true);
            final String code = getCaptchaCode(br.getBaseURL() + relCaptchaUrl, dummyLink);
            login.getInputField("code").setValue(Encoding.urlEncode(code));

        }
        login.setEncoding("application/x-www-form-urlencoded");
        login.getInputField("identifier").setValue(Encoding.urlEncode(account.getUser()));
        login.getInputField("password").setValue(Encoding.urlEncode(account.getPass()));
        login.getInputField("password2").setValue("Password");
        br.getHeaders().put(new HTTPHeader("Origin", "http://21members.com"));

        br.submitForm(login);

        String error = br.getRegex(LOGIN_ERROR_REGEX).getMatch(0);
        String sidiCookie = br.getCookie("21members.com", "sidi");
        String redirect = br.getRedirectLocation();

        if (error != null || sidiCookie == null || redirect == null) {
            return false;
        }

        account.saveCookies(br.getCookies(getHost()), cookieCacheID);
        br.getPage(redirect);
        return true;

    }

    /**
     * @param account
     * @param ai
     * @return
     * @throws PluginException
     * @throws IOException
     */
    private void parseAccountInfo(Account account, AccountInfo ai) throws PluginException, IOException {

        String[] daysLeft = br.getRegex("<span class=\"daysleft\">(\\d+)</span> Days left</span>").getColumn(0);
        int maxDaysLeft = -1;
        for (String d : daysLeft) {
            int left = Integer.parseInt(d);
            maxDaysLeft = Math.max(maxDaysLeft, left);
        }
        if (maxDaysLeft < 0) {
            ai.setExpired(true);
        }

        String[] activeNetwork = br.getRegex("infoPanel (\\S+)  active").getColumn(0);
        ai.setPremiumPoints(br.getRegex("<span class=\"creditNum\">(\\d+)").getMatch(0));
        StringBuilder sb = new StringBuilder();
        for (String network : activeNetwork) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(network);
        }
        ai.setStatus(sb.toString());
        ai.setUnlimitedTraffic();
        ai.setValidUntil(System.currentTimeMillis() + (maxDaysLeft + 1) * 24 * 60 * 60 * 1000l);
    }

    @Override
    public List<TwentyOneMembersVariantInfo> getVariantsByLink(DownloadLink downloadLink) {
        return downloadLink.getVariants(TwentyOneMembersVariantInfo.class);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        TwentyOneMembersVariantInfo variant = getActiveVariantByLink(downloadLink);

        if (!TwentyOneMembersCom.login(br)) {
            UIOManager.I().showErrorMessage("An active 21members.com account is required for " + downloadLink.getName());
        }

        ArrayList<TwentyOneMembersVariantInfo> variants;
        if (variant.isPhoto()) {
            br.getPage("http://21members.com/members/scene/photos/" + downloadLink.getStringProperty("id") + "?sidi=" + br.getCookie(getHost(), "sidi"));
            variants = TwentyOneMembersCom.parsePhotoVariants(br);
        } else {
            br.getPage("http://21members.com/members/scene/info/" + downloadLink.getStringProperty("id") + "?sidi=" + br.getCookie(getHost(), "sidi"));
            variants = TwentyOneMembersCom.parseVideoVariants(br);
        }
        if (variants != null) {
            for (TwentyOneMembersVariantInfo v : variants) {
                if (StringUtils.equals(v._getUniqueId(), variant._getUniqueId())) {
                    downloadLink.setProperty("url", v.getUrl());

                    URLConnectionAdapter con = br.openGetConnection(v.getUrl());
                    try {
                        long length = con.getCompleteContentLength();
                        if (con.isOK() && length > 0) {
                            downloadLink.setDownloadSize(length);
                            return AvailableStatus.TRUE;
                        }
                    } finally {
                        con.disconnect();
                    }

                }
            }
        }
        return AvailableStatus.FALSE;
    }

    @Override
    public TwentyOneMembersVariantInfo getActiveVariantByLink(DownloadLink downloadLink) {
        return downloadLink.getVariant(TwentyOneMembersVariantInfo.class);
    }

    @Override
    public JComponent getVariantPopupComponent(DownloadLink downloadLink) {
        return super.getVariantPopupComponent(downloadLink);
    }

    @Override
    public void setActiveVariantByLink(DownloadLink downloadLink, LinkVariant variant) {
        if (variant != null && variant instanceof TwentyOneMembersVariantInfo) {

            TwentyOneMembersCom.setVariant(downloadLink, (TwentyOneMembersVariantInfo) variant);

        }
    }

    /**
     * @param downloadLink
     * @param variant
     * @return
     */
    private static String formatFileName(DownloadLink downloadLink, TwentyOneMembersVariantInfo variant) {

        return downloadLink.getStringProperty("title") + "-" + variant._getName() + "." + variant._getExtension();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);

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

    public static boolean login(Browser br) {
        final List<Account> accs = AccountController.getInstance().getValidAccounts("21members.com");
        if (accs != null) {
            for (Account ac : accs) {
                try {
                    final PluginForHost hostPlugin = JDUtilities.getPluginForHost("21members.com");
                    if (((TwentyOneMembersCom) hostPlugin).login(br, ac)) {

                        return true;
                    }
                } catch (final Exception e) {
                }
            }
        }
        return false;
    }

    public static ArrayList<TwentyOneMembersVariantInfo> parseVideoVariants(Browser br) {
        if (br.containsHTML("buy it first")) {
            return null;
        }
        String downloads = br.getRegex("<!-- download -->(.*?)<!-- end download -->").getMatch(0);
        String[][] downloadInfo = new Regex(downloads, "<a href=\"([^\"]+)\".*?>.*?<div class=\"([^\"]+)\" data-csst=\"([^\"]+)\">").getMatches();

        ArrayList<TwentyOneMembersVariantInfo> variantInfos = new ArrayList<TwentyOneMembersVariantInfo>();

        for (String[] v : downloadInfo) {
            variantInfos.add(new TwentyOneMembersVariantInfo(v[0], v[2]));
        }
        return variantInfos;
    }

    public static void setVariant(DownloadLink link, TwentyOneMembersVariantInfo v) {

        link.setVariant(v);
        link.setLinkID(link.getPluginPatternMatcher() + "." + v._getUniqueId());
        link.setDownloadSize(-1);
        String fileName = formatFileName(link, v);
        link.setFinalFileName(fileName);
    }

    public static ArrayList<TwentyOneMembersVariantInfo> parsePhotoVariants(Browser br) {
        if (br.containsHTML("buy it first")) {
            return null;
        }
        String downloads = br.getRegex("<!-- download -->(.*?)<!-- end download -->").getMatch(0);
        String[][] downloadInfo = new Regex(downloads, "<a href=\"([^\"]+)\" class=\"([^\"]+)\">").getMatches();

        ArrayList<TwentyOneMembersVariantInfo> variantInfos = new ArrayList<TwentyOneMembersVariantInfo>();

        for (String[] v : downloadInfo) {
            variantInfos.add(new TwentyOneMembersVariantInfo(v[0], v[1]));
        }
        return variantInfos;
    }
}
