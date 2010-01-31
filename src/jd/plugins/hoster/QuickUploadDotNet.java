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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "quickupload.net" }, urls = { "http://[\\w\\.]*?(quickupload|ezyfile)\\.net/[a-z0-9]{12}" }, flags = { 2 })
public class QuickUploadDotNet extends PluginForHost {

    public QuickUploadDotNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(COOKIE_HOST + "/premium.html");
    }

    private static final String COOKIE_HOST = "http://www.quickupload.net";

    @Override
    public String getAGBLink() {
        return "http://quickupload.net/tos.html";
    }

    // ezyfile went down so the userswill see that ezyfile is now quickupload
    // een if all old ezyfile links are down
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("ezyfile.net", "quickupload.net"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie(COOKIE_HOST, "lang", "english");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("No such file with this filename")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("<input type=\"hidden\" name=\"fname\" value=\"(.*?)\">").getMatch(0));
        String filesize = br.getRegex("You have requested <font color=\"red\">http://quickupload.net/.*?/.*?</font> \\((.*?)\\)</font>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setFinalFileName(filename.replace("-quickupload.net", ""));
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        // Form um auf free zu "klicken"
        Form DLForm0 = br.getForm(0);
        if (DLForm0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLForm0.remove("method_premium");
        br.submitForm(DLForm0);
        // Form um auf "Datei herunterladen" zu klicken
        Form DLForm = br.getFormbyProperty("name", "F1");
        if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String passCode = null;
        if (br.containsHTML("<br><b>Password:</b>")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            DLForm.put("password", passCode);
        }
        String[][] letters = br.getRegex("<span style='position:absolute;padding-left:(\\d+)px;padding-top:\\d+px;'>(\\d)</span>").getMatches();
        if (letters.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        SortedMap<Integer, String> capMap = new TreeMap<Integer, String>();
        for (String[] letter : letters) {
            capMap.put(Integer.parseInt(letter[0]), letter[1]);
        }
        StringBuilder code = new StringBuilder();
        for (String value : capMap.values()) {
            code.append(value);
        }
        DLForm.put("code", code.toString());
        int tt = Integer.parseInt(br.getRegex("countdown\">(\\d+)</span>").getMatch(0));
        sleep(tt * 1001, downloadLink);
        br.submitForm(DLForm);
        String dllink = br.getRedirectLocation();
        URLConnectionAdapter con2 = br.getHttpConnection();
        if (con2.getContentType().contains("html")) {
            String error = br.getRegex("class=\"err\">(.*?)</font>").getMatch(0);
            if (error != null) {
                logger.warning(error);
                con2.disconnect();
                if (error.equalsIgnoreCase("Wrong captcha") || error.equalsIgnoreCase("Expired session")) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, error, 10000);
                }
            }
            if (br.containsHTML("Wrong password")) {
                logger.warning("Wrong password!");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("Wrong captcha")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (dllink == null) {
                dllink = br.getRegex("dotted #bbb;padding.*?<a href=\"(.*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("This direct link will be available for your IP.*?href=\"(http.*?)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("Download: <a href=\"(.*?)\"").getMatch(0);
                    }
                }
            }
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie(COOKIE_HOST, "lang", "english");
        br.setDebug(true);
        br.getPage(COOKIE_HOST + "/login.html");
        Form loginform = br.getForm(0);
        if (loginform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        loginform.put("login", Encoding.urlEncode(account.getUser()));
        loginform.put("password", Encoding.urlEncode(account.getPass()));
        br.submitForm(loginform);
        br.getPage(COOKIE_HOST + "/?op=my_account");
        if (!br.containsHTML("Premium-Account expire")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (br.getCookie(COOKIE_HOST, "login") == null || br.getCookie(COOKIE_HOST, "xfss") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String space = br.getRegex(Pattern.compile("<td>Used space:</td>.*?<td.*?b>(.*?)of.*?Mb</b>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim() + " Mb");
        String points = br.getRegex(Pattern.compile("<td>You have collected:</td.*?b>(.*?)premium points", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (points != null) {
            // Who needs half points ? If we have a dot in the points, just
            // remove it
            if (points.contains(".")) {
                String dot = new Regex(points, ".*?(\\.(\\d+))").getMatch(0);
                points = points.replace(dot, "");
            }
            ai.setPremiumPoints(Long.parseLong(points.trim()));
        }
        account.setValid(true);
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("<td>Premium-Account expire:</td>.*?<td>(.*?)</td>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            expire = expire.replaceAll("(<b>|</b>)", "");
            ai.setValidUntil(Regex.getMilliSeconds(expire, "dd MMMM yyyy", null));
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        String passCode = null;
        requestFileInformation(link);
        login(account);
        br.setCookie(COOKIE_HOST, "lang", "english");
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            Form DLForm = br.getFormbyProperty("name", "F1");
            if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (br.containsHTML("<b>Passwort:</b>|<b>Password:</b>")) {
                if (link.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", link);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = link.getStringProperty("pass", null);
                }
                DLForm.put("password", passCode);
                logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
            }
            br.submitForm(DLForm);
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                if (br.containsHTML("(<b>Passwort:</b>|<b>Password:</b>|Wrong password)")) {
                    logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                    link.setProperty("pass", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                if (dllink == null) {
                    dllink = br.getRegex("dotted #bbb;padding.*?<a href=\"(.*?)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("This direct link will be available for your IP.*?href=\"(http.*?)\"").getMatch(0);
                        if (dllink == null) {
                            // This was for fileop.com, maybe also works for
                            // others!
                            dllink = br.getRegex("Download: <a href=\"(.*?)\"").getMatch(0);
                        }
                    }
                }
            }
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Final downloadlink = " + dllink + " starting the download...");
        jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        boolean error = false;
        try {
            if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
                error = true;
            }
        } catch (Exception e) {
            error = true;
        }
        if (error == true) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML("File Not Found")) {
                logger.warning("Server says link offline, please recheck that!");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
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