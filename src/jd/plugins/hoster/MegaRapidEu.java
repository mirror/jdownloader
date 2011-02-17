//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megarapid.eu" }, urls = { "(http://[\\w\\.]*?megarapid\\.eu/files/\\d+/.+)|(http://[\\w\\.]*?megarapid\\.eu/\\?e=403\\&m=captcha\\&file=\\d+/.+)" }, flags = { 2 })
public class MegaRapidEu extends PluginForHost {

    public MegaRapidEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return "http://www.megarapid.eu/static/?vypsatCo=podminky";
    }

    private boolean             DIRECT     = false;
    public String               DIRECTLINK = null;
    private static final String MAINPAGE   = "http://www.megarapid.eu";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Please click here to continue")) {
            String continuePage = br.getRegex("<p><a href=\"(.*?)\"").getMatch(0);
            if (continuePage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage(continuePage);
        }
        if (br.getRedirectLocation() == null) {
            String filename = new Regex(link.getDownloadURL(), "captcha\\&file=\\d+/(.+)\\&s=").getMatch(0);
            if (filename == null) {
                filename = new Regex(link.getDownloadURL(), "megarapid\\.eu/files/\\d+/(.+)").getMatch(0);
                if (filename == null) filename = new Regex(link.getDownloadURL(), "\\?fileId=\\d+\\&fileName=(.+)").getMatch(0);
            }
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setName(filename.trim());
        } else {
            DIRECTLINK = br.getRedirectLocation();
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String dllink = DIRECTLINK;
        if (DIRECT && DIRECTLINK == null) {
            dllink = downloadLink.getDownloadURL();
            br.getPage(downloadLink.getDownloadURL());
            dllink = br.getRedirectLocation();
        } else if (dllink == null) {
            br.setFollowRedirects(true);
            String fileid = new Regex(downloadLink.getDownloadURL(), "(files/|file=)(\\d+)").getMatch(1);
            if (fileid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String downLink = "http://www.megarapid.eu/files/" + fileid + "/" + downloadLink.getName();
            String getData = "http://megarapid.eu/remote/?action=captcha&file=" + fileid + "/" + downloadLink.getName();
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            boolean valid = false;
            for (int i = 0; i <= 5; i++) {
                br.getPage(getData);
                if (!br.containsHTML("\"status\":true")) {
                    if (br.containsHTML("Try again in one hour")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
                    logger.warning("Actual link = " + downloadLink.getDownloadURL());
                    logger.warning(br.toString());
                    String error = br.getRegex("msg\":\"(.*?)\"").getMatch(0);
                    if (error != null) {
                        error = "Unsupported error occured: " + error;
                    } else {
                        error = "Unsupported error occured!";
                    }
                    throw new PluginException(LinkStatus.ERROR_FATAL, error);
                }
                String hash = br.getRegex("\"hash\":\"(.*?)\"").getMatch(0);
                String captchaurl = br.getRegex("\"imgsrc\":\"(http:.*?)\"").getMatch(0);
                if (hash == null || captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                captchaurl = captchaurl.replace("\\", "");
                String code = getCaptchaCode(captchaurl, downloadLink);
                String postData = "action=captcha&hash=" + hash + "&code=" + code + "&odkaz=" + Encoding.urlEncode(downLink) + "&nazev=" + Encoding.urlEncode(downloadLink.getName());
                br.postPage("http://megarapid.eu/remote/", postData);
                if (!br.containsHTML("status\":true,\"msg\":\"OK\"") && br.containsHTML("msg\":\"Kod se neshoduje")) {
                } else if (!br.containsHTML("status\":true,\"msg\":\"OK\"")) {
                    logger.warning("Unknown error in captchahandling for link: " + downloadLink.getDownloadURL());
                    logger.warning(br.toString());
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    valid = true;
                    break;
                }
            }
            if (!valid) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            Browser brLoad = br.cloneBrowser();
            brLoad.getPage(downLink);
            if (brLoad.containsHTML("(<h1>Not Found</h1>|<p>The requested URL was not found on this server\\.</p>)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            dllink = brLoad.getRedirectLocation();
        }
        if (dllink == null) {
            logger.warning("Couldn't get the final link for link: " + downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains("free_traffic_exceeded")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        if (dllink.contains("server_busy")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, JDL.L("plugins.hoster.MegaRapidEu.busyoroffline", "File is offline or server is busy"));
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Lost connection to MySQL server")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.getHeaders().put("Referer", "");
        br.setCustomCharset("utf-8");
        br.getPage(MAINPAGE);
        br.postPage("http://www.megarapid.eu/?do=login", "presenter=Stahuj&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie(MAINPAGE, "user") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String availabletraffic = br.getRegex("<p>Kredity: <strong>([0-9,]+)</strong>").getMatch(0);
        if (availabletraffic != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic.replace(",", ".") + "GB"));
        } else {
            account.setValid(false);
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        // Tested with a "megarapid.eu/files/" link
        br.getPage(link.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
        }
        String dllink = br.getRegex("<p><a href=\"(http://.*?)\">").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = Encoding.htmlDecode(dllink);
        br.getPage(dllink);
        dllink = br.getRegex("<p><a href=\"(http://.*?)\">").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = Encoding.htmlDecode(dllink);
        br.getPage(dllink);
        dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}