//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import java.util.Arrays;
import java.util.List;

import jd.PluginWrapper;
import jd.config.Property;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rpnet.biz" }, urls = { "http://(www\\.)?dl[^\\.]*.rpnet\\.biz/download/.*/([^/\\s]+)?" }, flags = { 0 })
public class RPNetBiz extends PluginForHost {

    private static final String mName    = "rpnet.biz";
    private static final String mProt    = "http://";
    private static final String mPremium = "https://premium.rpnet.biz/";
    private static final Object LOCK     = new Object();

    public RPNetBiz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName + "/");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("http://www.", "http://"));
    }

    @Override
    public String getAGBLink() {
        return mPremium + "tos.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        // tested with 20 seems fine.
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        // requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, 0);
        URLConnectionAdapter con = dl.getConnection();
        List<Integer> allowedResponseCodes = Arrays.asList(200, 206);
        if (!allowedResponseCodes.contains(con.getResponseCode()) || con.getContentType().contains("html") || con.getResponseMessage().contains("Download doesn't exist for given Hash/ID/Key")) {
            try {
                br.followConnection();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getDownloadURL());
            List<Integer> allowedResponseCodes = Arrays.asList(200, 206);
            if (!allowedResponseCodes.contains(con.getResponseCode()) || con.getContentType().contains("html") || con.getResponseMessage().contains("Download doesn't exist for given Hash/ID/Key")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            link.setName(getFileNameFromHeader(con));
            link.setDownloadSize(con.getLongContentLength());
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        br.getPage(mPremium + "client_api.php?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&action=generate");
        if (br.containsHTML("Invalid authentication")) {
            account.setValid(false);
            ai.setProperty("multiHostSupport", Property.NULL);
            return ai;
        }
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(-1);
        ai.setStatus("Premium User");
        ArrayList<String> hosts = new ArrayList<String>(Arrays.asList("rapidshare.com", "netload.in", "hotfile.com", "megashares.com", "uploaded.to", "filefactory.com", "bitshare.com", "freakshare.net", "crocko.com", "filepost.com", "turboit.net", "extabit.com", "ifile.it", "uploading.com", "jumbofiles.com", "letitbit.net", "ryushare.com", "share-online.biz", "slingfile.com"));
        ai.setProperty("multiHostSupport", hosts);
        return ai;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        showMessage(link, "Task 1: Generating Link");
        /* request Download */
        br.getPage(mPremium + "client_api.php?username=" + Encoding.urlEncode(acc.getUser()) + "&password=" + Encoding.urlEncode(acc.getPass()) + "&action=generate&links=" + Encoding.urlEncode(link.getDownloadURL()));
        String generatedLink = br.getRegex("\"generated\":\"([^\"]*?)\"").getMatch(0);
        if (generatedLink == null || generatedLink.isEmpty()) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        showMessage(link, "Task 2: Download begins!");
        generatedLink = generatedLink.replaceAll("\\\\/", "/");
        try {
            handleDL(link, generatedLink);
            return;
        } catch (PluginException e1) {
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(DownloadLink link, String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().isContentDisposition()) {
            /* contentdisposition, lets download it */
            dl.startDownload();
            return;
        } else {
            /*
             * download is not contentdisposition, so remove this host from
             * premiumHosts list
             */
            br.followConnection();
        }

        /* temp disabled the host */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }
}