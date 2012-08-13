package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 1 $", interfaceVersion = 2, names = { "x-art.com" }, urls = { "https?://x-art\\.com/members/(videos/.+)" }, flags = { 2 })
public class XArtCom extends PluginForHost {

    private String HTTP_Auth = "";

    public String getAuthHeader() {
        return this.HTTP_Auth;
    }

    public void setAuthHeader(String AuthHeader) {
        this.HTTP_Auth = AuthHeader;
    }

    public XArtCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://x-art.com/join/");
    }

    @Override
    public String getAGBLink() {
        return "http://x-art.com/legal/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        String name = null;
        if (parameter.getDownloadURL().contains("/videos/")) {
            name = new Regex(parameter.getDownloadURL(), "/videos/.+/(.+)\\.(mov|mp4|wmv)").getMatch(0);
        }
        if (name == null) name = "Unknown Filename";
        String type = new Regex(parameter.getDownloadURL(), "/videos/.+/(.+)\\.(mov|mp4|wmv)").getMatch(1);
        if (type != null) {
            if ("wmv".equalsIgnoreCase(type)) {
                name = name + ".wmv";
            } else if ("mp4".equalsIgnoreCase(type)) {
                name = name + ".mp4";
            } else if ("mov".equalsIgnoreCase(type)) {
                name = name + ".mov";
            } else {
                name = name + "-" + type + ".mp4";
            }
        }
        parameter.setName(name);

        /*
         * this.setBrowserExclusive(); br.getHeaders().put("Authorization", "Basic " + this.getAuthHeader()); br.setFollowRedirects(true); int res_code =
         * br.openGetConnection(parameter.getDownloadURL()).getResponseCode(); if (res_code == 200) { return AvailableStatus.TRUE; } else if (res_code == 404) {
         * throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); } else if (res_code == 401) { throw new PluginException(LinkStatus.ERROR_PREMIUM); } else
         * {
         */
        return AvailableStatus.UNCHECKABLE;
        /* } */
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) throw (PluginException) e;
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "X-Art members only!");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (account == null) return false;
        return true;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        URLConnectionAdapter con = null;
        try {
            this.setBrowserExclusive();
            br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
            con = br.openGetConnection("http://x-art.com/members/");
            if (con.getResponseCode() == 401) {
                throw new Throwable("Account invalid");
            } else {
                br.followConnection();
            }
            account.setValid(true);
            ai.setStatus("Account valid");
        } catch (final Throwable e) {
            try {
                con.disconnect();
            } catch (final Throwable e2) {
            }
            account.setValid(false);
            ai.setStatus("Account invalid");
        }
        return ai;
    }

    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        this.setAuthHeader(Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
        br.getHeaders().put("Authorization", "Basic " + this.getAuthHeader());
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
        if (dl.getConnection().getResponseCode() == 401) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

}
