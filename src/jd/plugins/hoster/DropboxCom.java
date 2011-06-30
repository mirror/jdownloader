package jd.plugins.hoster;

import java.io.IOException;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dropbox.com" }, urls = { "https?://dl-web\\.dropbox\\.com/get/.*?w=[0-9a-f]+" }, flags = { 2 })
public class DropboxCom extends PluginForHost {
    private static final Object             LOCK       = new Object();
    private static HashMap<String, Cookies> accountMap = new HashMap<String, Cookies>();

    public DropboxCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.dropbox.com/pricing");
    }

    @Override
    public String getAGBLink() {
        return "https://www.dropbox.com/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_FATAL, "You can only download files from your own account!");
    }

    private void login(final Account account, boolean refresh) throws IOException, PluginException {
        boolean ok = false;
        synchronized (LOCK) {
            setBrowserExclusive();
            br.setFollowRedirects(true);
            if (refresh == false) {
                Cookies accCookies = accountMap.get(account.getUser());
                if (accCookies != null) {
                    br.getCookies("https://www.dropbox.com").add(accCookies);
                    return;
                }
            }
            try {
                br.getPage("https://www.dropbox.com");
                br.postPage("https://www.dropbox.com/login", "t=&login_email=" + Encoding.urlEncode(account.getUser()) + "&login_password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie("https://www.dropbox.com", "puc") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                ok = true;
            } finally {
                if (ok) {
                    accountMap.put(account.getUser(), br.getCookies("https://www.dropbox.com"));
                } else {
                    accountMap.remove(account.getUser());
                }
            }
        }

    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        login(account, false);
        String dlURL = downloadLink.getDownloadURL();
        if (!dlURL.contains("?dl=1") && !dlURL.contains("&dl=1")) {
            dlURL = dlURL + "&dl=1";
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlURL, true, 0);
        final URLConnectionAdapter con = dl.getConnection();
        if (!con.isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        br.setDebug(true);
        try {
            login(account, true);
        } catch (final PluginException e) {
            ai.setStatus("Account not valid.");
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Account ok");
        account.setValid(true);
        return ai;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
