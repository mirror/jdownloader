package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision: 35284 $", interfaceVersion = 3, names = { "oracle.com" }, urls = { "(https?://updates\\.oracle\\.com/Orion/Services/download/.*?\\?aru=\\d+&patch_file=(.+)|https?://.*?oracle\\.com/.*?download\\?fileName=.*?&token=.+)" })
public class OracleCom extends PluginForHost {

    public OracleCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://login.oracle.com/mysso/signon.jsp");
    }

    @Override
    public String getAGBLink() {
        return "http://www.oracle.com/us/legal/terms/index.html";
    }

    private void login(Account account) throws Exception {
        synchronized (account) {
            br.setFollowRedirects(true);
            final Cookies cookies = account.loadCookies("");
            try {
                boolean login = true;
                if (cookies != null) {
                    br.setCookies(getHost(), cookies);
                    br.getPage("https://www.oracle.com/index.html");
                    if (br.containsHTML(">Sign Out<")) {
                        login = false;
                    } else {
                        br.clearCookies(getHost());
                    }
                }
                if (login) {
                    account.clearCookies("");
                    br.getPage("https://www.oracle.com/index.html");
                    br.getPage("http://www.oracle.com/webapps/redirect/signon?nexturl=https://www.oracle.com/index.html");
                    Form form = br.getForm(0);
                    br.submitForm(form);
                    form = br.getForm(0);
                    form.put("ssousername", Encoding.urlEncode(account.getUser()));
                    form.put("password", Encoding.urlEncode(account.getPass()));
                    br.submitForm(form);
                    if (br.containsHTML("readerpwderrormsg")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if (!br.getURL().matches("https?://www.oracle.com/index.html")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        setBrowserExclusive();
        login(account);
        return new AccountInfo();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        String name = new Regex(parameter.getPluginPatternMatcher(), "patch_file=(.+?)(&|$)").getMatch(0);
        if (name == null) {
            name = new Regex(parameter.getPluginPatternMatcher(), "fileName=(.+?)(&|$)").getMatch(0);
        }
        if (name != null) {
            parameter.setName(name);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        login(account);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), true, 1);
        final URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() == 404) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (con.getResponseCode() == 403) {
            br.followConnection();
            if (br.containsHTML("You do not have the required access privilege")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else if (StringUtils.contains(con.getContentType(), "text")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            return super.canHandle(downloadLink, account);
        }
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
