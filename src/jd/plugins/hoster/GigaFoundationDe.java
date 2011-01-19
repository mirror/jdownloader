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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gigafoundation.de" }, urls = { "http://[\\w\\.]*?gigafoundationdecrypted\\.de/features/downloads/details/\\d+" }, flags = { 2 })
public class GigaFoundationDe extends PluginForHost {

    public GigaFoundationDe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://gigafoundation.de/features/donate/");
    }

    private static final String GIGAMAINPAGE = "http://www.gigafoundation.de/";

    @Override
    public String getAGBLink() {
        return "http://www.gigafoundation.de/impressum/";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("gigafoundationdecrypted.de", "gigafoundation.de"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">01\\.01\\.70 01:00 Uhr<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("align=\"right\">Dateiname:</div>[\t\n\r ]+<div class=\"small\" align=\"right\">(.*?)</div").getMatch(0);
        String filesize = br.getRegex("align=\"right\">Dateigröße:</div>[\t\n\r ]+<div class=\"small\" align=\"right\">(.*?)</div").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        String md5 = br.getRegex("align=\"right\">MD5 Checksumme <a href=\"/features/downloads/faq/#md5sum\">\\[\\?\\]</a>:</div>[\t\n\r ]+<div class=\"small\" align=\"right\">(.*?)</div>").getMatch(0);
        if (md5 != null) link.setMD5Hash(md5.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        boolean freePluginBroken = true;
        if (freePluginBroken) throw new PluginException(LinkStatus.ERROR_FATAL, "Leider funktioniert dieses Plugin noch nicht für Freeuser");
        handleDownload(downloadLink);
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.postPage(GIGAMAINPAGE, "x=0&y=0&login%5Buser%5D=" + Encoding.urlEncode(account.getUser()) + "&login%5Bpass%5D=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie(GIGAMAINPAGE, "wbb2_userid") == null || br.getCookie(GIGAMAINPAGE, "wbb2_userpassword") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        account.setValid(true);
        ai.setUnlimitedTraffic();
        ai.setStatus("Premium User");
        return ai;
    }

    public void handleDownload(DownloadLink link) throws Exception {
        br.getPage("http://gigafoundation.de/features/downloads/getTicket.php?" + new Regex(link.getDownloadURL(), "gigafoundation\\.de/features/downloads/details/(\\d+)").getMatch(0));
        String dllink = br.getRegex("\"(http:.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        handleDownload(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 3;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}