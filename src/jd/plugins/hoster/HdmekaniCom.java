package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hdmekani.com" }, urls = { "ftp://.*?hdmekani\\.com/[^& \"\r\n]+" }, flags = { 2 })
public class HdmekaniCom extends PluginForHost {

    public HdmekaniCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.hdmekani.com/");
    }

    @Override
    public String getAGBLink() {
        return "http://www.hdmekani.com/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_FATAL, "Premium only");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        ai.setStatus("Only checkable during Download");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        String url = new Regex(downloadLink.getDownloadURL(), "ftp://(.*?@)?(.+)").getMatch(1);
        String dllink = "ftp://" + account.getUser() + ":" + account.getPass() + "@" + url;
        try {
            ((Ftp) JDUtilities.getNewPluginForHostInstance("ftp")).download(dllink, downloadLink, true);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("530")) {
                downloadLink.getLinkStatus().setErrorMessage("Login incorrect");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            throw e;
        }
    }

}
