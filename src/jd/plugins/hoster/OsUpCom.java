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
import jd.config.Property;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "os-up.com" }, urls = { "http://(www\\.)?os\\-up\\.com/filehost/file/[A-Za-z0-9]+/.{1}" }, flags = { 0 })
public class OsUpCom extends PluginForHost {

    public OsUpCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("http://www.os-up.com/home/1/");
    }

    @Override
    public String getAGBLink() {
        return "http://www.os-up.com/home/1/";
    }

    private static final String PASSWORDTEXT = ">Bitte geben sie das Passwort f\\&uuml;r den Download ein";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Der Download wurde Gesperrt|>Datei wurde vom User Gel\\&ouml;scht|>Datei nicht gefunden<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(PASSWORDTEXT)) {
            link.getLinkStatus().setStatusText("This link is password protected");
            return AvailableStatus.TRUE;
        }
        final String filename = br.getRegex("<dt>DateiName:</dt>[\t\n\r ]+<dd>([^<>\"]*?)</dd>").getMatch(0);
        final String filesize = br.getRegex("<dt>Datei Gr\\&ouml;\\&szlig;e:</dt>[\t\n\r ]+<dd>([^<>\"]*?)</dd>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        final String md5 = br.getRegex("<dt>MD5 Checksum:</dt>[\t\n\r ]+<dd>([^<>\"]*?)</dd>").getMatch(0);
        if (md5 != null) link.setMD5Hash(md5);
        final String sha1 = br.getRegex("<dt>SHA1 Checksum:</dt>[\t\n\r ]+<dd>([^<>\"]*?)</dd>").getMatch(0);
        if (sha1 != null) link.setSha1Hash(sha1);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(PASSWORDTEXT)) {
            String passCode = downloadLink.getStringProperty("pass", null);
            if (passCode == null) passCode = Plugin.getUserInput("Password?", downloadLink);
            br.postPage(br.getURL(), "Filehost_File_Kennwort_" + getFileID(downloadLink) + "=" + Encoding.urlEncode(passCode));
            if (br.containsHTML(PASSWORDTEXT)) {
                downloadLink.setProperty("pass", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_FATAL, "Wrong password!");
            }
            downloadLink.setProperty("pass", passCode);
        }
        String result = null;
        final PluginForDecrypt keycplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
        try {
            final jd.plugins.decrypter.LnkCrptWs.KeyCaptcha kc = ((jd.plugins.decrypter.LnkCrptWs) keycplug).getKeyCaptcha(br);
            result = kc.showDialog(downloadLink.getDownloadURL());
        } catch (final Throwable e) {
            result = null;
        }
        if (result == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if ("CANCEL".equals(result)) throw new PluginException(LinkStatus.ERROR_FATAL);
        final String postData = "capcode=" + result + "&fileid=" + getFileID(downloadLink) + "&type=1&type=1";
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, "http://www.os-up.com/index.php?p=filehost&action=getfile", postData, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getFileID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "/file/([A-Za-z0-9]+)/").getMatch(0);
    }

    // private static final String MAINPAGE = "http://os-up.com";
    // private static Object LOCK = new Object();
    //
    // @SuppressWarnings("unchecked")
    // private void login(final Account account, final boolean force) throws Exception {
    // synchronized (LOCK) {
    // try {
    // // Load cookies
    // br.setCookiesExclusive(true);
    // final Object ret = account.getProperty("cookies", null);
    // boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name",
    // Encoding.urlEncode(account.getUser())));
    // if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass",
    // Encoding.urlEncode(account.getPass())));
    // if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
    // final HashMap<String, String> cookies = (HashMap<String, String>) ret;
    // if (account.isValid()) {
    // for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
    // final String key = cookieEntry.getKey();
    // final String value = cookieEntry.getValue();
    // br.setCookie(MAINPAGE, key, value);
    // }
    // return;
    // }
    // }
    // br.postPage("http://www.os-up.com/userlogin/",
    // "staylogged=1&p=userlogin&action=newlogin&area=1&backurl=aHR0cDovL3d3dy5vcy11cC5jb20vaW5kZXgucGhw&login_email=jdownload" +
    // Encoding.urlEncode(account.getUser()) + "&login_pass=" + Encoding.urlEncode(account.getPass()));
    // if (br.getCookie(MAINPAGE, "login_pass") == null) {
    // if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // } else {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // }
    // // Save cookies
    // final HashMap<String, String> cookies = new HashMap<String, String>();
    // final Cookies add = br.getCookies(MAINPAGE);
    // for (final Cookie c : add.getCookies()) {
    // cookies.put(c.getKey(), c.getValue());
    // }
    // account.setProperty("name", Encoding.urlEncode(account.getUser()));
    // account.setProperty("pass", Encoding.urlEncode(account.getPass()));
    // account.setProperty("cookies", cookies);
    // } catch (final PluginException e) {
    // account.setProperty("cookies", Property.NULL);
    // throw e;
    // }
    // }
    // }
    //
    // @Override
    // public AccountInfo fetchAccountInfo(final Account account) throws Exception {
    // AccountInfo ai = new AccountInfo();
    // try {
    // login(account, true);
    // } catch (PluginException e) {
    // account.setValid(false);
    // throw e;
    // }
    // String space = br.getRegex("").getMatch(0);
    // if (space != null) ai.setUsedSpace(space.trim());
    // ai.setUnlimitedTraffic();
    // final String expire = br.getRegex("<td>Premium-Account expire:</td>.*?<td>(.*?)</td>").getMatch(0);
    // if (expire == null) {
    // account.setValid(false);
    // return ai;
    // } else {
    // ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
    // }
    // account.setValid(true);
    // ai.setStatus("Premium User");
    // return ai;
    // }
    //
    // @Override
    // public void handlePremium(final DownloadLink link, final Account account) throws Exception {
    // requestFileInformation(link);
    // login(account, false);
    // br.setFollowRedirects(false);
    // br.getPage(link.getDownloadURL());
    // String dllink = br.getRegex("").getMatch(0);
    // if (dllink == null) {
    // logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
    // if (dl.getConnection().getContentType().contains("html")) {
    // logger.warning("The final dllink seems not to be a file!");
    // br.followConnection();
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // dl.startDownload();
    // }
    //
    // @Override
    // public int getMaxSimultanPremiumDownloadNum() {
    // return -1;
    // }

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