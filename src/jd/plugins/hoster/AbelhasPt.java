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
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

/*Same script for AbelhasPt, LolaBitsEs, CopiapopEs, MinhatecaComBr*/
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "abelhas.pt" }, urls = { "http://(www\\.)?abelhasdecrypted\\.pt/\\d+" }, flags = { 2 })
public class AbelhasPt extends PluginForHost {

    public AbelhasPt(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://abelhas.pt/action/Registration/Create");
    }

    @Override
    public String getAGBLink() {
        return "http://abelhas.pt/TerminosCondiciones.aspx";
    }

    private static boolean pluginloaded = false;
    private String         fileid       = null;
    private String         requesttoken = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        fileid = null;
        requesttoken = null;
        if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!link.getDownloadURL().matches("http://(www\\.)?abelhasdecrypted\\.pt/\\d+")) {
            /* Only accept new links from the decrypter */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getStringProperty("mainlink", null));
        if (br.containsHTML("class=\"noFile\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = link.getStringProperty("plain_filename", null);
        final String filesize = link.getStringProperty("plain_filesize", null);
        link.setFinalFileName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        /* Sometimes free users can only download low quality streams */
        String dllink_video = br.getRegex("\"(http://(www\\.)?abelhas\\.pt/Video\\.ashx\\?e=[^<>\"]*?)\"").getMatch(0);
        String dllink = null;
        try {
            dllink = getDllink_general(downloadLink);
        } catch (final Throwable e) {
        }
        if (dllink == null) {
            dllink = dllink_video;
        }
        if (dllink == null) {
            /* Either plugin broken or only downloadable via account */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        dllink = Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            /* Whatever happens here, always show server error... */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
        }
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://abelhas.pt";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.getPage("http://abelhas.pt/");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage("http://abelhas.pt/action/login/loginWindow", "Redirect=true");
                br.postPage("http://abelhas.pt/action/login/login", "RememberMe=true&__RequestVerificationToken=undefined&RedirectUrl=&Redirect=True&FileId=0&Login=" + Encoding.urlEncode(account.getUser()) + "&Password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "RememberMe") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Registered (free) user");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(true);
        br.getPage(link.getStringProperty("mainlink", null));
        String dllink = getDllink_general(link);
        if (dllink == null) {
            br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
            final String orgfile = br.getRegex("name=\"orgFile\" value=\"([^<>\"]*?)\"").getMatch(0);
            final String userSelection = br.getRegex("name=\"userSelection\" value=\"([^<>\"]*?)\"").getMatch(0);
            if (orgfile == null || userSelection == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.postPage("http://abelhas.pt/action/License/acceptLargeTransfer", "fileId=" + fileid + "&orgFile=" + Encoding.urlEncode(orgfile) + "&userSelection=" + Encoding.urlEncode(userSelection) + "&__RequestVerificationToken=" + Encoding.urlEncode(requesttoken));
            dllink = br.getRegex("\"redirectUrl\":\"(http[^<>\"]*?)\"").getMatch(0);
            if (dllink != null) {
                dllink = unescape(dllink);
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getDllink_general(final DownloadLink dl) throws PluginException, IOException {
        handlePWProtected(dl);
        fileid = br.getRegex("id=\"fileDetails_(\\d+)\"").getMatch(0);
        requesttoken = br.getRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (fileid == null || requesttoken == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://abelhas.pt/action/License/Download", "fileId=" + fileid + "&__RequestVerificationToken=" + Encoding.urlEncode(requesttoken));
        String dllink = br.getRegex("\"redirectUrl\":\"(http[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(http://s\\d+\\.abelhas\\.pt/File\\.aspx[^\\\\\"]+)").getMatch(0);
        }
        if (dllink != null) {
            dllink = unescape(dllink);
        }
        return dllink;
    }

    private static synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (pluginloaded == false) {
            final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
            if (plugin == null) {
                throw new IllegalStateException("youtube plugin not found!");
            }
            pluginloaded = true;
        }
        return jd.nutils.encoding.Encoding.unescapeYoutube(s);
    }

    private void handlePWProtected(final DownloadLink dl) throws PluginException, IOException {
        String passCode = dl.getStringProperty("pass", null);
        if (br.containsHTML("class=\"LoginToFolderForm\"")) {
            final String reqtoken = br.getRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            final String chomikid = br.getRegex("type=\"hidden\" name=\"ChomikId\" value=\"(\\d+)\"").getMatch(0);
            final String folderid = br.getRegex("name=\"FolderId\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
            final String foldername = br.getRegex("id=\"FolderName\" name=\"FolderName\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            if (reqtoken == null || chomikid == null || folderid == null || foldername == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            boolean success = false;
            for (int i = 1; i <= 3; i++) {
                if (passCode == null) {
                    passCode = Plugin.getUserInput("Password?", dl);
                }
                br.postPageRaw("http://" + this.getHost() + "/action/Files/LoginToFolder", "Remember=true&Remember=false&ChomikId=" + chomikid + "&FolderId=" + folderid + "&FolderName=" + Encoding.urlEncode(foldername) + "&Password=" + Encoding.urlEncode(passCode) + "&__RequestVerificationToken=" + Encoding.urlEncode(reqtoken));
                if (br.containsHTML("\"IsSuccess\":false")) {
                    passCode = null;
                    dl.setProperty("pass", Property.NULL);
                    continue;
                }
                success = true;
                break;
            }
            if (!success) {
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            /* We don't want to work with the encoded json bla html response */
            br.getPage(dl.getStringProperty("mainlink", null));
        }
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     * */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals(""))) {
            return true;
        } else {
            return false;
        }
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