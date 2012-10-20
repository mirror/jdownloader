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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "met-art.com" }, urls = { "https?://members\\.met-art\\.com/members/(media/.+|movie\\.php.+|movie\\.mp4.+|zip\\.php\\?zip=[A-Z0-9]+\\&type=(high|med|low))" }, flags = { 2 })
public class MetArtCom extends PluginForHost {

    public MetArtCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://signup.met-art.com/model.htm?from=homepage");
    }

    @Override
    public String getAGBLink() {
        return "http://guests.met-art.com/faq/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        String name = null;
        if (parameter.getDownloadURL().contains("/media/")) {
            name = new Regex(parameter.getDownloadURL(), "/media/.*?/[A-F0-9]+/(.+)").getMatch(0);
        } else if (parameter.getDownloadURL().contains("movie.php")) {
            name = new Regex(parameter.getDownloadURL(), "movie\\.php.+?file=(.*?)($|&)").getMatch(0);
        } else if (parameter.getDownloadURL().contains("movie.mp4")) {
            name = new Regex(parameter.getDownloadURL(), "movie\\.mp4.+?file=(.*?)($|&)").getMatch(0);
        } else if (parameter.getDownloadURL().contains("zip.php")) {
            name = new Regex(parameter.getDownloadURL(), "zip\\.php\\?zip=([A-Z0-9]+)\\&").getMatch(0);
        }
        if (name == null) name = "Unknown Filename";
        String type = new Regex(parameter.getDownloadURL(), "movie\\.(php|mp4).*?type=(.*?)&").getMatch(1);
        if (parameter.getDownloadURL().contains("zip.php")) type = ".zip";
        if (type != null) {
            if ("avi".equalsIgnoreCase(type)) {
                name = name + ".avi";
            } else if ("wmv".equalsIgnoreCase(type)) {
                name = name + ".wmv";
            } else if ("mpg".equalsIgnoreCase(type)) {
                name = name + ".mpg";
            } else if (".zip".equalsIgnoreCase(type)) {
                name = name + type;
            } else {
                name = name + "-" + type + ".mp4";
            }
        }
        parameter.setName(name);
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) throw (PluginException) e;
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "Met-Art members only!");
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
            con = br.openGetConnection("http://members.met-art.com/members/");
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
        br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
        if (dl.getConnection().getResponseCode() == 401) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }
}
