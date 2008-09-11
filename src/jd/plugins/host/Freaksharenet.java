//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.host;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class Freaksharenet extends PluginForHost {

    public Freaksharenet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return "http://freakshare.net/?x=faq";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {

        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        String url = downloadLink.getDownloadURL();

        br.getPage(url);
        if (!br.containsHTML("<span class=\"txtbig\">Fehler</span>")) {
            String[][] filename = new Regex(br, Pattern.compile("colspan=\"2\" class=\"content_head\">(.*?)<b>(.*?)</b>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatches();
            downloadLink.setName(filename[0][1]);
            String[][] filesize = new Regex(br, Pattern.compile("<b>Datei(.*?)</b>(.*?)<td width=\"48%\" height=\"10\" align=\"left\" class=\"content_headcontent\">(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatches();
            downloadLink.setDownloadSize(Regex.getSize(filesize[0][2]));

            return true;
        }

        return false;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        Form form = br.getFormbyValue(null);
        form.put("wait", "Download");

        /* Datei herunterladen */
        HTTPConnection urlConnection = br.openFormConnection(form);

        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        downloadLink.setLocalSpeedLimit(75 * 1024);
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setChunkNum(1);
        dl.setResume(false);
        dl.startDownload();
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        Browser br = new Browser();
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());

        br.setFollowRedirects(true);
        br.getPage("http://freakshare.net/");
        Form form = br.getFormbyValue("LOGIN");
        form.put("fb_username", account.getUser());
        form.put("fb_password", account.getPass());

        br.submitForm(form);

        String[] dat = br.getRegex("Cash: <b>(.*?) .</b><br>.*?Total Points: <b>(\\d*?)</b><br>.*?Files: <b>(\\d*?) <font color=.*?>\\((.*?)\\)</font></b></font><br>").getRow(0);
        if (dat == null) {
            ai.setValid(false);
            ai.setStatus("Logins incorrect");
            return ai;
        }
        ai.setFilesNum(Integer.parseInt(dat[2]));
        ai.setAccountBalance(dat[0]);
        ai.setUsedSpace(dat[3]);
        ai.setPremiumPoints(dat[1]);
        String expire = br.getRegex("<td width=.*? align=.*? height=.*? class=\"content_headcontent\">.*?G.ltig bis.*?class=\"content_headcontent\".*?<font.*?>(.*?)</font>.*?</td>").getMatch(0);
        String freeTraffic = br.getRegex("<td width=.*? align=.*? height=.*? class=\"content_headcontent\">.*?Traffic verbleibend.*?class=\"content_headcontent\".*?<font.*?>(.*?)</font>.*?</td>").getMatch(0);

        ai.setValidUntil(Regex.getMilliSeconds(expire, "dd.MM.yyyy", Locale.GERMAN));
        ai.setTrafficLeft(freeTraffic);

        return ai;
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        Form form = br.getFormbyValue("LOGIN");
        form.put("fb_username", account.getUser());
        form.put("fb_password", account.getPass());

        br.submitForm(form);

        /* Datei herunterladen */
        br.setFollowRedirects(true);
        HTTPConnection urlConnection = br.openGetConnection(downloadLink.getDownloadURL());
        if (urlConnection.getContentType().equals("text/html; charset=ISO-8859-1")) {
            logger.finer("Direct Download disabled");
            br.followConnection();
            form = br.getFormbyValue("Direkt-Download");
            urlConnection = br.openFormConnection(form);
        }
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        downloadLink.setLocalSpeedLimit(-1);
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setChunkNum(1);
        dl.setResume(false);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}