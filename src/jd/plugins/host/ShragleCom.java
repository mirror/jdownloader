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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class ShragleCom extends PluginForHost {

    public ShragleCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.shragle.com/index.php?p=accounts");
        setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.shragle.com/index.php?cat=about&p=faq";
    }

    private void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://www.shragle.com/index.php?p=login");
        br.postPage("http://www.shragle.com/index.php?p=login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&cookie=1&submit=Login");
        String Cookie = br.getCookie("http://www.shragle.com", "userID");
        if (Cookie == null) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
        Cookie = br.getCookie("http://www.shragle.com", "username");
        if (Cookie == null) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
        Cookie = br.getCookie("http://www.shragle.com", "password");
        if (Cookie == null) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
        br.getPage("http://www.shragle.com/?cat=user");
        if (br.containsHTML(">Premium-Upgrade<")) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        br.getPage("http://www.shragle.com/?cat=user");
        if (br.containsHTML(">Premium-Upgrade<")) {
            ai.setStatus("This is no Premium Account!");
            ai.setValid(false);
            return ai;
        }
        String premPoints = br.getRegex(Pattern.compile("<td>Premium-Punkte:</td>.*?<td>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (premPoints != null) ai.setPremiumPoints(premPoints);

        String expires = br.getRegex(Pattern.compile("<b>Ihr Premium-Account ist gültig bis zum (.*?) Uhr</b>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (expires == null) {
            ai.setValid(false);
            return ai;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy  hh:mm", Locale.UK);
        try {
            Date date = dateFormat.parse(expires.replaceAll("um", ""));
            ai.setValidUntil(date.getTime());
        } catch (ParseException e) {
        }
        return ai;
    }

    public void correctUrl(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.de/", "\\.com/"));
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        getFileInformation(downloadLink);
        login(account);
        Thread.sleep(500);/* sonst kommen serverfehler */
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        Thread.sleep(500);/* sonst kommen serverfehler */
        br.setDebug(true);
        if (br.getRedirectLocation() != null) {
            br.setFollowRedirects(true);
            dl = br.openDownload(downloadLink, br.getRedirectLocation(), true, 0);
        } else {
            Form form = br.getFormbyName("download");
            br.setFollowRedirects(true);
            dl = br.openDownload(downloadLink, form, true, 0);
        }
        HTTPConnection con = dl.getConnection();
        if (!con.isContentDisposition()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        setBrowserExclusive();
        correctUrl(downloadLink);
        String id=new Regex(downloadLink.getDownloadURL(),"shragle.com/files/(.*?)/").getMatch(0);
        
        
       String[] data = Regex.getLines(br.getPage("http://www.shragle.com/api.php?key=078e5ca290d728fd874121030efb4a0d&action=getStatus&fileID="+id));
        br.getPage(downloadLink.getDownloadURL());
        if(data.length!=4)throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String name=data[0];
        String size=data[1];
        String md5=data[2];
        //status 0: all ok  1: abused
        String status=data[3];
        
        if(!status.equals("0"))throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);     
   
        downloadLink.setName(name.trim());
        downloadLink.setDownloadSize(Long.parseLong(size));
        downloadLink.setMD5Hash(md5.trim());
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        Form form = br.getFormbyName("download");
        sleep(10000l, downloadLink);
        br.setFollowRedirects(true);
        /*
         * zum zeitpunkt der implementation waren nur 3 verbindungen gesamt
         * erlaubt
         */
        dl = br.openDownload(downloadLink, form, true, -3);
        HTTPConnection con = dl.getConnection();
        if (!con.isContentDisposition()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public int getTimegapBetweenConnections() {
        return 1000;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}