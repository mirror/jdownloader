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

import jd.PluginWrapper;
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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "warserver.cz" }, urls = { "http://(www\\.)?warserver\\.cz/stahnout/\\d+" }, flags = { 2 })
public class WarServerCz extends PluginForHost {

    public WarServerCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://warserver.cz/platby/cenik");
    }

    @Override
    public String getAGBLink() {
        return "http://warserver.cz/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage("http://www.warserver.cz/api/?action=file-info&fid=" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
        if (br.containsHTML("ERROR: File not found\\.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = parseJson("filename");
        final String filesize = parseJson("size");
        final String md5Sum = parseJson("md5sum");
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        if (md5Sum != null) link.setMD5Hash(md5Sum);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String dllink = parseJson("free-link");
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Ve free modu muzete stahovat pouze jeden soubor z jedne IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        br.setFollowRedirects(false);
        br.getPage("http://www.warserver.cz/api/?action=user-info&username=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("(ERROR: Invalid password\\.|ERROR: User doesn\\'t exists\\.)")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        final String availabletraffic = parseJson("credit");
        if (availabletraffic == null) {
            account.setValid(false);
            return ai;
        }
        ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic));
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage("http://www.warserver.cz/api/?action=generate-premium-link&username=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&fid=" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0) + "&redirect=1");
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String parseJson(final String input) {
        return br.getRegex("\"" + input + "\":\"([^<>\"]*?)\"").getMatch(0);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}