package jd.plugins.hoster;

import java.io.IOException;

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
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yourporn.sexy", "sxyprn.com" }, urls = { "https?://(?:www\\.)?yourporn\\.sexy/post/([a-fA-F0-9]{13})(?:\\.html)?", "https?://(?:www\\.)?sxyprn\\.(?:com|net)/post/([a-fA-F0-9]{13})(?:\\.html)?" })
public class SxyprnCom extends antiDDoSForHost {
    public SxyprnCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://sxyprn.com/community/0.html");
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

    private String  json         = null;
    private String  dllink       = null;
    private boolean server_issue = false;
    private String  authorid     = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, Account account) throws Exception {
        json = null;
        dllink = null;
        br.setFollowRedirects(true);
        if (account == null) {
            account = AccountController.getInstance().getValidAccount(this.getHost());
        }
        if (account != null) {
            this.login(this.br, account, false);
        }
        getPage(link.getPluginPatternMatcher());
        final String title = br.getRegex("name\" content=\"(.*?)\"").getMatch(0);
        /** 2019-07-08: yourporn.sexy now redirects to youporn.com but sxyprn.com still exists. */
        if (title == null || br.getHost().equals("youporn.com") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (link.getFinalFileName() == null) {
            link.setFinalFileName(title.trim() + ".mp4");
        }
        final String fid = this.getFID(link);
        authorid = br.getRegex("data-authorid='([^']+)'").getMatch(0);
        json = br.getRegex("data-vnfo=\\'([^\\']+)\\'").getMatch(0);
        String vnfo = PluginJSonUtils.getJsonValue(json, fid);
        if (vnfo == null && json != null) {
            final String ids[] = new Regex(json, "\"([a-z0-9]*?)\"").getColumn(0);
            for (final String id : ids) {
                vnfo = PluginJSonUtils.getJsonValue(json, id);
                dllink = getDllink(link, vnfo);
                if (dllink != null) {
                    break;
                }
            }
        } else {
            dllink = getDllink(link, vnfo);
        }
        return AvailableStatus.TRUE;
    }

    private long ssut51(final String input) {
        final String num = input.replaceAll("[^0-9]", "");
        long ret = 0;
        for (int i = 0; i < num.length(); i++) {
            ret += Long.parseLong(String.valueOf(num.charAt(i)), 10);
        }
        return ret;
    }

    private String getDllink(final DownloadLink link, final String vnfo) throws Exception {
        final String tmp[] = vnfo.split("/");
        if (StringUtils.containsIgnoreCase(br.getHost(), "sxyprn.net")) {
            tmp[1] += "5";
        } else {
            tmp[1] += "8";
        }
        tmp[5] = String.valueOf((Long.parseLong(tmp[5]) - (ssut51(tmp[6]) + ssut51(tmp[7]))));
        final String url = "/" + StringUtils.join(tmp, "/");
        Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(false);
        getPage(brc, url);
        final String redirect = brc.getRedirectLocation();
        if (redirect != null && link.getVerifiedFileSize() == -1) {
            brc = br.cloneBrowser();
            final URLConnectionAdapter con = openAntiDDoSRequestConnection(brc, brc.createHeadRequest(redirect));
            try {
                if (con.isOK() && StringUtils.containsIgnoreCase(con.getContentType(), "video")) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                    return redirect;
                }
            } finally {
                con.disconnect();
            }
        }
        if (redirect == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return redirect;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 5;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, null);
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        if (dllink == null) {
            if (json != null && json.length() <= 10) {
                /* Rare case: E.g. empty json object '[]' */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: No video source available");
            } else if (server_issue) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: All video sources are broken");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection();
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                /*
                 * 2019-09-24: E.g. serverside broken content, videos will not even play via browser. This may also happen when a user opens
                 * up a lot of connections to this host!
                 */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    public void login(final Browser brlogin, final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                boolean isLoggedin = false;
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    this.br.setCookies(this.getHost(), cookies);
                    if (!validateCookies && System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 5 * 60 * 1000l) {
                        logger.info("Trust cookies without check");
                        return;
                    }
                    getPage(brlogin, "https://" + this.getHost() + "/");
                    if (this.isLoggedin(brlogin)) {
                        logger.info("Cookie login successful");
                        isLoggedin = true;
                    } else {
                        logger.info("Cookie login failed");
                        isLoggedin = false;
                    }
                }
                if (!isLoggedin) {
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
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("class=(\\'|\")lout_btn(\\'|\")");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account, true);
        } catch (final PluginException e) {
            throw e;
        }
        ai.setUnlimitedTraffic();
        /*
         * 2020-04-27: They only have free accounts - there is no premium model. Free acccount users can sometimes see content which is
         * otherwise hidden / seemingly offline.
         */
        account.setType(AccountType.FREE);
        ai.setStatus("Registered (free) user");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account);
        handleDownload(link, null);
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
