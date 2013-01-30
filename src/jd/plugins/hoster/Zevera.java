//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.controlling.AccountController;
import jd.gui.UserIO;
import jd.http.Browser;
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
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zevera.com" }, urls = { "https?://[\\w\\.]*?zevera\\.com/.+" }, flags = { 0 })
public class Zevera extends PluginForHost {
    // TODO REMOVE WITH JD2 == stable

    static public Object LOGINLOCK = new Object();

    private static String getID(String link) {
        String id = new Regex(link, ".*fid=(.+)&.*").getMatch(0);
        return id;
    }

    private boolean showDialog = false;

    public Zevera(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(1000l);
        this.enablePremium("http://www.zevera.com/Prices.aspx");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        showDialog = true;
        try {
            setBrowserExclusive();
            loginAPI(account, ai);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://zevera.com/TermsOfUse.aspx";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 800;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        throw new PluginException(LinkStatus.ERROR_FATAL, "You need a premium account to use this hoster");
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        setBrowserExclusive();
        workAroundTimeOut(br);
        showDialog = false;
        br.setDebug(true);
        String user = Encoding.urlEncode(account.getUser());
        String pw = Encoding.urlEncode(account.getPass());
        String link = downloadLink.getDownloadURL();
        if (link.contains("getFiles.aspx")) {
            showMessage(downloadLink, "Downloading file");
            user = account.getUser();
            pw = account.getPass();
            String basicauth = "Basic " + Encoding.Base64Encode(user + ":" + pw);
            br.getHeaders().put("Authorization", basicauth);
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 0);
        } else {
            showMessage(downloadLink, "Phase 1/2: Get FileID");
            String FileID = Zevera.getID(link);
            if (FileID == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Error adding file, FileID null"); }
            while (true) {
                showMessage(downloadLink, "Phase 2/3: Check download:");
                br.getPage("http://www.zevera.com/jDownloader.ashx?cmd=fileinfo&login=" + user + "&pass=" + pw + "&FileID=" + FileID);
                String infos[] = br.getRegex("(.*?)(,|$)").getColumn(0);
                if (infos[0].contains("FileID:0")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Error adding file, FileID doesnt exist"); }
                if (infos[6].contains("Status:Downloaded")) {
                    String DownloadURL = br.getRegex("DownloadURL:(Http.*?),").getMatch(0);
                    if (DownloadURL == null) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE); }
                    showMessage(downloadLink, "Phase 3/3: OK Download starting");
                    dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DownloadURL, true, 0);
                    break;
                } else
                    this.sleep(15 * 1000l, downloadLink, "Waiting for download to finish on Zevera");
            }
        }
        if (dl.getConnection().isContentDisposition()) {
            long filesize = dl.getConnection().getLongContentLength();
            if (filesize == 0) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 10 * 60 * 1000l);
            dl.startDownload();
        } else {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
    }

    private void loginAPI(Account account, AccountInfo ai) throws IOException, PluginException {
        synchronized (LOGINLOCK) {
            workAroundTimeOut(br);
            if (ai == null) {
                ai = account.getAccountInfo();
                if (ai == null) {
                    ai = new AccountInfo();
                    account.setAccountInfo(ai);
                }
            }
            br.getPage("http://www.zevera.com/");
            String res = br.getPage("http://www.zevera.com/jDownloader.ashx?cmd=accountinfo&login=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
            if ("No trafic".equals(res) || res == null || res.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            res = res.trim();
            account.setValid(true);
            if ("Login Error".equalsIgnoreCase(res)) {
                ai.setStatus("Unknown user");
                if (showDialog) UserIO.getInstance().requestMessageDialog(0, "Zevera Premium Error", "The username '" + account.getUser() + "' is unknown.\r\nPlease check your Username!");

                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                /* normal premium */
                // 0 DayTrafficLimit:5120,
                // 1 EndSubscriptionDate:2012/6/29 0:0:0,
                // 2 TrafficUsedToday:0,
                // 3 AvailableTodayTraffic5120,
                // 4 OronDayTrafficLimit:5120
                ai.setStatus("Premium");
                String infos[] = br.getRegex("(.*?)(,|$)").getColumn(0);
                if (infos == null || infos.length != 6) {
                    logger.info(br.toString());
                }
                String EndSubscriptionDate = new Regex(infos[1], "EndSubscriptionDate:(.+)").getMatch(0);
                ai.setValidUntil(TimeFormatter.getMilliSeconds(EndSubscriptionDate, "yyyy/MM/dd HH:mm:ss", null));

                /* Traffic balance not working in Zevera JD API */
                // Integer DayTraffic = Integer.parseInt(new
                // Regex(infos[0],"DayTrafficLimit:(.+)").getMatch(0).trim());
                // Integer TrafficUsedToday = Integer.parseInt(new
                // Regex(infos[0],"TrafficUsedToday:(.+)").getMatch(0).trim());
                // Integer Balance = DayTraffic - TrafficUsedToday;
                // ai.setAccountBalance(Balance * 1024);
                String AvailableTodayTraffic = new Regex(infos[3], "AvailableTodayTraffic:(\\d+)").getMatch(0);
                logger.info("Zevera: AvailableTodayTraffic=" + AvailableTodayTraffic);
                ai.setTrafficLeft(SizeFormatter.getSize(AvailableTodayTraffic + "mb"));
                if (ai.isExpired()) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setDebug(true);
        // We need to log in to get the file status^M
        Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null) throw new PluginException(LinkStatus.ERROR_FATAL, "Links are only checkable if a registered account is entered!");
        String link = downloadLink.getDownloadURL();
        String user = Encoding.urlEncode(aa.getUser());
        String pw = Encoding.urlEncode(aa.getPass());

        if (link.contains("getFiles.aspx")) {
            String ourl = new Regex(link, ".*ourl=(.+)").getMatch(0);
            if (ourl == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            String res = br.getPage("http://www.zevera.com/jDownloader.ashx?cmd=checklink&login=" + user + "&pass=" + pw + "&olink=" + ourl);
            String filename = new Regex(link, ".*/(.+)").getMatch(0);
            downloadLink.setName(filename.trim());
            if (res.contains("Alive")) {
                return AvailableStatus.TRUE;
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }

        String FileID = Zevera.getID(link);
        String res = br.getPage("http://www.zevera.com/jDownloader.ashx?cmd=fileinfo&login=" + user + "&pass=" + pw + "&FileID=" + FileID);

        if (res.contains("Conversion")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Error adding file, bad FileID format"); }
        String infos[] = br.getRegex("(.*?)(,|$)").getColumn(0);
        if (infos[0].contains("FileID:0")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Error adding file, FileID doesnt exist"); }

        String filename = new Regex(infos[1], "FileName:(.+)").getMatch(0);
        String filesize = new Regex(infos[4], "FileSizeInBytes:(.+)").getMatch(0);

        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));

        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {

    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    /* TODO: remove me after 0.9xx public */
    private void workAroundTimeOut(Browser br) {
        try {
            if (br != null) {
                br.setConnectTimeout(30000);
                br.setReadTimeout(120000);
            }
        } catch (Throwable e) {
        }
    }
}