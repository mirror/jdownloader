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
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "data.hu" }, urls = { "http://[\\w\\.]*?data.hu/get/.+/.+" }, flags = { 2 })
public class DataHu extends PluginForHost {

    private static final Object LOCK = new Object();

    public DataHu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://data.hu/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "http://data.hu/adatvedelem.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String[] dat = br.getRegex("<div class=\"download_filename\">(.*?)<\\/div>.*\\:(.*?)<div class=\"download_not_start\">").getRow(0);
        if (dat.length != 2) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setDownloadSize(Regex.getSize(dat[1].trim()));
        downloadLink.setName(dat[0].trim());
        return AvailableStatus.TRUE;
    }

    public void login(Account account) throws Exception {
        synchronized (LOCK) {
            this.setBrowserExclusive();
            br.forceDebug(true);
            br.getPage("http://data.hu/");
            Form form = br.getForm(0);
            form.put("username", Encoding.urlEncode(account.getUser()));
            form.put(form.getInputFieldByName("login_passfield").getValue(), Encoding.urlEncode(account.getPass()));
            form.put("remember", "on");
            br.submitForm(form);
            br.getPage("http://data.hu/index.php");
            if (br.getCookie("http://data.hu/", "datapremiumseccode") == null) {
                logger.warning("Cookie error!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (!isPremium()) {
                logger.warning("This account is no a premium account!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    public boolean isPremium() throws IOException {
        br.getPage("http://data.hu/user.php");
        if (br.getRedirectLocation() != null) {
            /* try to workaround server issue */
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
            }
            br.getPage("http://data.hu/user.php");
        }
        if (br.getRegex("logged_user_prem_date\">(.*?)<").matches()) return true;
        return false;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }

        String days = br.getRegex("logged_user_prem_date\">(.*?)<").getMatch(0);
        if (days != null && !days.equals("0")) {
            ai.setValidUntil(Regex.getMilliSeconds(days, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
        } else {
            logger.warning("Couldn't get the expire date, stopping premium!");
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        }
        String points = br.getRegex(Pattern.compile("leftpanel_datapont_pont\">(\\d+)</span>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (points != null) ai.setPremiumPoints(Long.parseLong(points.trim().replaceAll(",|\\.", "")));
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.forceDebug(true);
        br.getPage(downloadLink.getDownloadURL());
        String link = br.getRegex("window.location.href='(.*?)';").getMatch(0);
        if (link == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 0);
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        requestFileInformation(downloadLink);

        if (br.containsHTML("A let.*?shez v.*?rnod kell:")) {
            long wait = (Long.parseLong(br.getRegex(Pattern.compile("<div id=\"counter\" class=\"countdown\">([0-9]+)</div>")).getMatch(0)) * 1000);
            sleep(wait, downloadLink);
        }
        br.getPage(downloadLink.getDownloadURL());
        String link = br.getRegex(Pattern.compile("download_it\"><a href=\"(http://.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 1).startDownload();

    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public int getMaxConnections() {
        return 1;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
