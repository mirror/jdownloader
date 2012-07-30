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
import java.util.ArrayList;
import java.util.Arrays;

import jd.PluginWrapper;
import jd.config.Property;
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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multishare.cz" }, urls = { "http://[\\w\\.]*?multishare\\.cz/stahnout/[0-9]+/" }, flags = { 2 })
public class MultiShareCz extends PluginForHost {

    public MultiShareCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.multishare.cz/cenik/");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            if (multiHostSupported()) {
                ai.setProperty("multiHostSupport", Property.NULL);
            }
            return ai;
        }
        account.setValid(true);
        try {
            account.setConcurrentUsePossible(true);
            account.setMaxSimultanDownloads(-1);
        } catch (final Throwable e) {
        }
        String space = br.getRegex("Velikost nahraných souborů:</span>.*?<strong>(.*?)</strong>").getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim().replace("&nbsp;", ""));
        String trafficleft = br.getRegex("Kredit:</span>.*?<strong>(.*?)</strong").getMatch(0);
        if (trafficleft == null) trafficleft = br.getRegex("class=\"big\"><strong>Kredit:(.*?)</strong>").getMatch(0);
        if (trafficleft != null) {
            trafficleft = trafficleft.replace("&nbsp;", "");
            trafficleft = trafficleft.replace(" ", "");
            ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
        }
        String hostedFiles = br.getRegex("Počet nahraných souborů:</span>.*?<strong>(\\d+)</strong>").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Integer.parseInt(hostedFiles));
        ai.setStatus("Premium User");
        if (multiHostSupported()) {
            try {
                String hostsSup = br.cloneBrowser().getPage("http://www.multishare.cz/html/mms_support.php");
                String[] hosts = Regex.getLines(hostsSup);
                ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
                /*
                 * set ArrayList<String> with all supported multiHosts of this service
                 */
                ai.setProperty("multiHostSupport", supportedHosts);
            } catch (Throwable e) {
                account.setProperty("multiHostSupport", Property.NULL);
                logger.info("Could not fetch ServerList from Multishare: " + e.toString());
            }
        }
        return ai;
    }

    private boolean multiHostSupported() {
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        int rev = Integer.parseInt(prev);
        if (rev < 16116) return false;
        return true;
    }

    @Override
    public String getAGBLink() {
        return "http://www.multishare.cz/kontakt/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String fileid = new Regex(downloadLink.getDownloadURL(), "/stahnout/(\\d+)/").getMatch(0);
        String dllink = "http://www.multishare.cz/html/download_free.php?ID=" + fileid;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        String fileid = new Regex(link.getDownloadURL(), "/stahnout/(\\d+)/").getMatch(0);
        String dllink = "http://www.multishare.cz/html/download_premium.php?ID=" + fileid;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(false);
        /* login to get u_ID and u_HASH */
        br.getPage("http://www.multishare.cz/");
        br.postPage("http://www.multishare.cz/html/prihlaseni_process.php", "jmeno=" + Encoding.urlEncode(acc.getUser()) + "&heslo=" + Encoding.urlEncode(acc.getPass()) + "&trvale=ano&akce=P%C5%99ihl%C3%A1sit");
        if (br.getCookie("http://www.multishare.cz", "sess_ID") == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        br.getPage("http://www.multishare.cz/");
        String trafficleft = br.getRegex("Kredit:</span>.*?<strong>(.*?)</strong").getMatch(0);
        if (trafficleft == null) trafficleft = br.getRegex("class=\"big\"><strong>Kredit:(.*?)</strong>").getMatch(0);
        if (trafficleft != null) {
            trafficleft = trafficleft.replace("&nbsp;", "");
            trafficleft = trafficleft.replace(" ", "");
            AccountInfo ai = acc.getAccountInfo();
            if (ai != null) ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
        }
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /* parse u_ID and u_HASH */
        String u_ID = form.getVarsMap().get("u_ID");
        String u_HASH = form.getVarsMap().get("u_hash");
        if (u_ID == null || u_HASH == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        showMessage(link, "Phase 1/3: Check Download");
        String url = Encoding.urlEncode(link.getDownloadURL());
        /* request Download */
        String page = br.postPage("http://www.multishare.cz/html/mms_ajax.php", "link=" + url);
        if (page.contains("Vašeho kreditu bude")) {
            showMessage(link, "Phase 2/3: Request Download");
            /* download is possible */
            br.getPage("http://www.multishare.cz/html/mms_process.php?link=" + url + "&u_ID=" + u_ID + "&u_hash=" + u_HASH + "&over=ano");
            if (br.containsHTML("ready")) {
                showMessage(link, "Phase 3/3: Download");
                /* download is ready */
                /* build final URL */
                String rnd = "dl" + Math.round(Math.random() * 10000l * Math.random());
                String fUrl = "http://" + rnd + "mms.multishare.cz/html/mms_process.php?link=" + url + "&u_ID=" + u_ID + "&u_hash=" + u_HASH;
                /*
                 * resume is supported, chunks make no sense and did not work for me either
                 */
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, fUrl, true, 1);
                if (dl.getConnection().isContentDisposition()) {
                    /* contentdisposition, lets download it */
                    dl.startDownload();
                    return;
                } else {
                    /*
                     * download is not contentdisposition, so remove this host from premiumHosts list
                     */
                    br.followConnection();
                }
            } else {
                /* not enough credits */
            }
        }
        /* temp disabled the host */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage("http://www.multishare.cz/");
        br.postPage("http://www.multishare.cz/html/prihlaseni_process.php", "jmeno=" + Encoding.urlEncode(account.getUser()) + "&heslo=" + Encoding.urlEncode(account.getPass()) + "&trvale=ano&akce=P%C5%99ihl%C3%A1sit");
        if (br.getCookie("http://www.multishare.cz", "sess_ID") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(Požadovaný soubor neexistuje|Je možné, že byl již tento soubor vymazán uploaderem nebo porušoval autorská práva)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>MultiShare\\.cz :: Stáhnout soubor \"(.*?)\"</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<li>Název: <strong>(.*?)</strong>").getMatch(0);
        String filesize = br.getRegex("Velikost: <strong>(.*?)</strong").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        filesize = filesize.replace("&nbsp;", "");
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}