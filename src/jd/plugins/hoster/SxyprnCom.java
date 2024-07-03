package jd.plugins.hoster;

import java.io.IOException;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yourporn.sexy", "sxyprn.com" }, urls = { "https?://(?:www\\.)?yourporn\\.sexy/post/([a-fA-F0-9]{13})(?:\\.html)?", "https?://(?:www\\.)?sxyprn\\.(?:com|net)/post/([a-fA-F0-9]{13})(?:\\.html)?" })
public class SxyprnCom extends antiDDoSForHost {
    public SxyprnCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://sxyprn.com/community/0.html");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public String getAGBLink() {
        return "https://sxyprn.com/";
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

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String dllink = null;

    // private String authorid = null;
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    public static String cleanupTitle(Plugin plugin, Browser br, final String title) {
        if (title == null) {
            return null;
        }
        String ret = Encoding.htmlDecode(title);
        ret = ret.replaceAll("(https?://.*?)(\\s+|$)", "");// remove URLs
        ret = ret.replaceAll("(?s)(.+#\\w+)\\s*(.+)", "$1");// remove everything after tags
        ret = ret.replaceAll("(?s)(WATCH FULL VIDEO.+)", "");// remove WATCH FULL VIDEO section
        ret = ret.replaceFirst("(?i)\\s*on SexyPorn$", "");// remove " on SexyPorn"
        ret = ret.trim();
        return ret;
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final String fid = this.getFID(link);
        final String videoExtDefault = ".mp4";
        if (!link.isNameSet()) {
            link.setName(fid + videoExtDefault);
        }
        dllink = null;
        if (account != null) {
            this.login(br, account, false);
        }
        getPage(link.getPluginPatternMatcher());
        /** 2019-07-08: yourporn.sexy now redirects to youporn.com but sxyprn.com still exists. */
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = regexTitle(br);
        if (title != null) {
            title = cleanupTitle(this, br, title);
            link.setFinalFileName(title + videoExtDefault);
        }
        // authorid = br.getRegex("data-authorid='([^']+)'").getMatch(0);
        final String json = br.getRegex("data-vnfo=\\'([^\\']+)\\'").getMatch(0);
        if (json == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (json.length() <= 10) {
            /* Rare case: E.g. empty json object '[]' */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: No video source available");
        }
        final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
        String vnfo = (String) entries.get(fid);
        if (vnfo == null && json != null) {
            final String ids[] = new Regex(json, "\"([a-z0-9]*?)\"").getColumn(0);
            for (final String id : ids) {
                vnfo = PluginJSonUtils.getJsonValue(json, id);
                dllink = getDllink(link, vnfo, isDownload);
                if (dllink != null) {
                    break;
                }
            }
        } else {
            dllink = getDllink(link, vnfo, isDownload);
        }
        return AvailableStatus.TRUE;
    }

    public static final String regexTitle(final Browser br) {
        return br.getRegex("<meta property='og:title' content='(.*?)'/>").getMatch(0);
    }

    public static final boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (br.containsHTML("(?i)class='page_message'[^>]*>\\s*Post Not Found")) {
            return true;
        } else if (br.getHost().equals("youporn.com")) {
            return true;
        } else {
            return false;
        }
    }

    private long ssut51(final String input) {
        final String num = input.replaceAll("[^0-9]", "");
        long ret = 0;
        for (int i = 0; i < num.length(); i++) {
            ret += Long.parseLong(String.valueOf(num.charAt(i)), 10);
        }
        return ret;
    }

    private String getDllink(final DownloadLink link, final String vnfo, final boolean isDownload) throws Exception {
        final String tmp[] = vnfo.split("/");
        if (StringUtils.containsIgnoreCase(br.getHost(), "sxyprn.net")) {
            tmp[1] += "5";
        } else {
            tmp[1] += "8";
        }
        tmp[5] = String.valueOf((Long.parseLong(tmp[5]) - (ssut51(tmp[6]) + ssut51(tmp[7]))));
        final String url = "/" + StringUtils.join(tmp, "/");
        if (isDownload) {
            /* Do not validate */
            return url;
        } else {
            try {
                final URLConnectionAdapter con = basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(url), link, link.getName(), null);
                return con.getURL().toExternalForm();
            } catch (Exception e) {
                logger.log(e);
                return null;
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    @Override
    protected void throwFinalConnectionException(Browser br, URLConnectionAdapter con) throws PluginException, IOException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    public void login(final Browser brlogin, final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                logger.info("Attempting cookie login");
                this.br.setCookies(this.getHost(), cookies);
                if (!validateCookies) {
                    /* Don't validate cookies */
                    return;
                }
                getPage(brlogin, "https://" + this.getHost() + "/");
                if (this.isLoggedin(brlogin)) {
                    logger.info("Cookie login successful");
                    account.saveCookies(br.getCookies(this.getHost()), "");
                    return;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(br.getHost());
                    account.clearCookies("");
                }
            }
            logger.info("Performing full login");
            getPage(brlogin, "https://" + this.getHost() + "/");
            final Form loginform = new Form();
            loginform.setMethod(MethodType.POST);
            loginform.setAction("/php/login.php");
            loginform.put("email", Encoding.urlEncode(account.getUser()));
            loginform.put("password", Encoding.urlEncode(account.getPass()));
            this.submitForm(brlogin, loginform);
            getPage(brlogin, "https://" + this.getHost() + "/");
            if (!isLoggedin(brlogin)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            account.saveCookies(br.getCookies(this.getHost()), "");
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("class=(\\'|\")lout_btn(\\'|\")");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(this.br, account, true);
        ai.setUnlimitedTraffic();
        /*
         * 2020-04-27: They only have free accounts - there is no premium model. Free acccount users can sometimes see content which is
         * otherwise hidden / seemingly offline.
         */
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, null);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 5;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 5;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        /* No captchas at all */
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
