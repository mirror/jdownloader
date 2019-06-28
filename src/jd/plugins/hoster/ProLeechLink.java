package jd.plugins.hoster;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Locale;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.http.requests.PostRequest;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision: 39245 $", interfaceVersion = 3, names = { "proleech.link" }, urls = { "https?://proleech\\.link/download/[a-zA-Z0-9]+(/.*)?" })
public class ProLeechLink extends antiDDoSForHost {
    public ProLeechLink(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://proleech.link/signup");
    }

    private static MultiHosterManagement mhm = new MultiHosterManagement("proleech.link");

    @Override
    public String getAGBLink() {
        return "https://proleech.link/page/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        final String fileName = new Regex(parameter.getPluginPatternMatcher(), "download/[a-zA-Z0-9]+/([^/\\?]+)").getMatch(0);
        if (fileName != null && !parameter.isNameSet()) {
            parameter.setName(fileName);
        }
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, ai);
        if (!ai.isExpired()) {
            getPage("/page/hostlist");
            final String hosts[] = br.getRegex("<td>\\s*\\d+\\s*</td>\\s*<td>\\s*<img[^<]+/?>\\s*([^<]*?)\\s*</td>\\s*<td>\\s*<span\\s*class\\s*=\\s*\"label\\s*label-success\"\\s*>\\s*Online").getColumn(0);
            if (hosts == null || hosts.length == 0) {
                logger.warning("Failed to find list of supported hosts");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ai.setMultiHostSupport(this, Arrays.asList(hosts));
        }
        return ai;
    }

    private boolean checkCookies(Cookies cookies) throws PluginException {
        if (cookies == null || cookies.get("amember_nr", Cookies.NOTDELETEDPATTERN) == null || cookies.get("amember_ru", Cookies.NOTDELETEDPATTERN) == null || cookies.get("amember_rp", Cookies.NOTDELETEDPATTERN) == null) {
            return false;
        } else {
            return true;
        }
    }

    private void login(Account account, AccountInfo ai) throws Exception {
        synchronized (account) {
            try {
                Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(getHost(), cookies);
                    getPage("https://proleech.link/member");
                    br.followRedirect();
                    cookies = br.getCookies(getHost());
                    if (br.getFormbyAction("/login") != null) {
                        cookies = null;
                    }
                }
                if (!checkCookies(cookies)) {
                    br.clearCookies(getHost());
                    getPage("https://proleech.link");
                    getPage("https://proleech.link/login/index");
                    final Form login = br.getFormbyAction("/login");
                    if (login == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    login.put("amember_login", URLEncoder.encode(account.getUser(), "UTF-8"));
                    login.put("amember_pass", URLEncoder.encode(account.getPass(), "UTF-8"));
                    submitForm(login);
                    br.followRedirect();
                    if (br.getFormbyAction("/login") != null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        getPage("/member");
                        br.followRedirect();
                        if (br.getFormbyAction("/login") != null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        cookies = br.getCookies(getHost());
                    }
                }
                if (!checkCookies(cookies)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final String activeSubscription = br.getRegex("am-list-subscriptions\">\\s*<li[^<]*>(.*?)</li>").getMatch(0);
                if (activeSubscription != null) {
                    final String expireDate = new Regex(activeSubscription, "([a-zA-Z]+\\s*\\d+,\\s*\\d{4})").getMatch(0);
                    if (expireDate != null) {
                        final long validUntil = TimeFormatter.getMilliSeconds(expireDate, "MMM' 'dd', 'yyyy", Locale.ENGLISH);
                        if (ai != null) {
                            ai.setValidUntil(validUntil);
                        }
                        account.setType(AccountType.PREMIUM);
                        account.setConcurrentUsePossible(true);
                        account.setMaxSimultanDownloads(-1);
                        return;
                    }
                } else {
                    account.setType(AccountType.FREE);
                    account.setConcurrentUsePossible(true);
                    account.setMaxSimultanDownloads(0);
                    ai.setTrafficLeft(0);
                }
                account.saveCookies(cookies, "");
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public void handleMultiHost(DownloadLink link, Account account) throws Exception {
        mhm.runCheck(account, link);
        login(account, null);
        String downloadURL = link.getStringProperty(getHost(), null);
        if (downloadURL != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadURL, true, 0);
            if (!dl.getConnection().isContentDisposition()) {
                logger.info("Saved downloadurl did not work");
                link.removeProperty(getHost());
                try {
                    br.followConnection();
                } catch (final IOException e) {
                    logger.log(e);
                }
                downloadURL = null;
            }
        }
        if (downloadURL == null) {
            final PostRequest post = new PostRequest("https://proleech.link/dl/debrid/deb_process.php");
            post.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
            post.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final String url = link.getDefaultPlugin().buildExternalDownloadURL(link, this);
            post.put("urllist", URLEncoder.encode(url, "UTF-8"));
            final String pass = link.getDownloadPassword();
            if (StringUtils.isEmpty(pass)) {
                post.put("pass", "");
            } else {
                post.put("pass", URLEncoder.encode(pass, "UTF-8"));
            }
            post.put("boxlinklist", "0");
            sendRequest(post);
            downloadURL = br.getRegex("class=\"[^\"]*success\".*?<a href\\s*=\\s*\"(https?://.*?)\"").getMatch(0);
            if (downloadURL == null) {
                final String danger = br.getRegex("class=\"[^\"]*danger\".*?<b>\\s*(.*?)\\s*</b>").getMatch(0);
                if (danger != null) {
                    logger.info(danger);
                }
                if (br.containsHTML(">\\s*No link entered\\.?\\s*<")) {
                    mhm.handleErrorGeneric(account, link, "no_link_entered", 50, 2 * 60 * 1000l);
                } else if (br.containsHTML(">\\s*Error getting the link from this account\\.?\\s*<")) {
                    mhm.putError(account, link, 2 * 60 * 1000l, "Error getting the link from this account");
                } else if (br.containsHTML(">\\s*Our account has reached traffic limit\\.?\\s*<")) {
                    mhm.putError(account, link, 2 * 60 * 1000l, "Error getting the link from this account");
                }
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 2 * 60 * 1000l);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadURL, true, 0);
            final boolean isOkay = dl.getConnection().isOK() && (dl.getConnection().isContentDisposition() || StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "application/force-download"));
            if (!isOkay) {
                try {
                    br.followConnection();
                } catch (final IOException e) {
                    logger.log(e);
                }
                mhm.handleErrorGeneric(account, link, "unknowndlerror", 50, 2 * 60 * 1000l);
            }
        }
        link.setProperty(getHost(), downloadURL);
        dl.startDownload();
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        login(account, null);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), true, 0);
        final boolean isOkay = dl.getConnection().isOK() && (dl.getConnection().isContentDisposition() || StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "application/force-download"));
        if (!isOkay) {
            try {
                br.followConnection();
            } catch (final IOException e) {
                logger.log(e);
            }
            if (StringUtils.endsWithCaseInsensitive(br.getURL(), "/downloader")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
