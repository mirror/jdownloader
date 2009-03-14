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

package jd.plugins.host;

import java.io.IOException;

import utils.Property;
//import java.util.Locale;

import jd.PluginWrapper;
//import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
//import jd.plugins.Account;
//import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
//import jd.utils.JDLocale;

public class UpMusicIn extends PluginForHost {

    //private int simultanpremium = 50;

    public UpMusicIn(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.upmusic.in/?op=registration");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        String linkurl = null;
        String previousLink = null;
        Object previousLinkProperty = new Property();
        previousLinkProperty = downloadLink.getProperty("directLink");
        if (previousLinkProperty==null) previousLink = null;
        else previousLink = previousLinkProperty.toString();
        if (previousLink == null) {
            getFileInformation(downloadLink);
            Form form1;
            form1 = br.getFormbyProperty("name", "F1");
            if (form1==null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            String waittime = br.getRegex("Wait\\s+<span[^>]*>(.*?)</span>").getMatch(0);
            int waittimen = 0;
            waittimen = Integer.valueOf(waittime).intValue();
            if (waittimen==0) waittimen = 40;
            this.sleep(waittimen*1001, downloadLink);
            br.submitForm(form1);
            linkurl = br.getRegex("This\\s+direct\\s+link.*href=\"(http://www\\.upmusic\\.in/files/.*?)\"").getMatch(0);
            if (linkurl == null) 
                {
                    if (br.containsHTML("err\">"))
                    {
                        String currentError = br.getRegex("err\">(.*?)</font>").getMatch(0);
                        throw new PluginException(LinkStatus.ERROR_FATAL, currentError);
                    }
                    else
                    {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
                    }
                }
            linkurl = linkurl.replaceAll(" " , "%20");
            downloadLink.setProperty("directLink", linkurl);
        }
        else 
        {
           
           linkurl = previousLink;
        }
        dl = br.openDownload(downloadLink, linkurl, true, -7);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isOK())
            {
            if (previousLink != null)
                {
                downloadLink.setProperty("directLink", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            else
                {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
           
            }
        if (con.getContentType().contains("text")) {
            br.getPage(linkurl);
            if (br.containsHTML("Premiums\\s+Plans")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10000); 
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 7;
    }
/* 
 * Support for Premium accounts will be added on users' request
 * atm no need to support premium, hoster give nice results for free users
 * 
    public void handleFree0(DownloadLink downloadLink) throws Exception {
        br.getPage(downloadLink.getDownloadURL());
        String linkurl = br.getRegex("downloadurl'\\);\">(.*?)</textarea>").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        // this.sleep(15000, downloadLink); // uncomment when they find a better
        // way to force wait time
        dl = br.openDownload(downloadLink, linkurl);
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.clearCookies("uploader.pl");
        br.getPage("http://uploader.pl/en/login.php");
        Form login = br.getForm(0);
        login.put("user", Encoding.urlEncode(account.getUser()));
        login.put("pass", Encoding.urlEncode(account.getPass()));
        login.put("autologin", "0");
        br.submitForm(login);
        String cookie1 = br.getCookie("http://uploader.pl/", "yab_uid");
        if (cookie1 == null || cookie1.equalsIgnoreCase("0")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    private boolean isPremium() throws IOException {
        br.getPage("http://uploader.pl/en/members.php?overview=1");
        if (br.containsHTML("package_info'\\)\"><b>Zareje")) return false;
        return true;
    }

    @Override
    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        if (!isPremium()) {
            ai.setStatus(JDLocale.L("plugins.hoster.UploaderPl.freememberacc", "Free registered user account"));
            ai.setValid(true);
            return ai;
        }
        String expired = br.getRegex("<b>Expired\\?</b></td>\\s*<td[^>]*>(.*?) <a href").getMatch(0);
        if (!expired.equalsIgnoreCase("No")) {
            ai.setValid(false);
            ai.setStatus(JDLocale.L("plugins.hoster.UploaderPl.accountexpired", "Account expired"));
            return ai;
        }
        String expires = br.getRegex("<b>Package Expire Date</b></td>\\s*<td[^>]*>(.*?)</td>").getMatch(0);
        expires = expires.trim();
        if (!expires.equalsIgnoreCase("Never")) ai.setValidUntil(Regex.getMilliSeconds(expires, "mm/dd/yy", Locale.UK));
        return ai;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        getFileInformation(downloadLink);
        login(account);
        if (!this.isPremium()) {
            simultanpremium = 10;
            handleFree0(downloadLink);
            return;
        } else {
            if (simultanpremium + 1 > 20) {
                simultanpremium = 20;
            } else {
                simultanpremium++;
            }
        }
        String linkurl = br.getRegex("downloadurl'\\);\">(.*?)</textarea>").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        // this.sleep(30000, downloadLink); // uncomment when they find a better
        // way to force wait time
        dl = br.openDownload(downloadLink, linkurl);
        dl.startDownload();
    }

*/
    @Override
    public String getAGBLink() {
        return "http://www.upmusic.in/tos.html";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRegex("<html>(.*?)</html>").getMatch(0) == null) throw new PluginException(LinkStatus.ERROR_RETRY);
        if (br.containsHTML("No such file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("File Name:</b></td><td[^>]*>(.+?)</td></tr>").getMatch(0);
        if (filename.endsWith("</b>")) filename = filename.substring(0, filename.length()-4);
        String filesize = br.getRegex("Size:</b></td><td>.*<small>\\((.+?)ytes\\)</small>").getMatch(0);
        //System.out.println(br.getRegex("<html>(.*?)</html>").getMatch(0));
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision: 4712 $");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}