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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "letitbit.net" }, urls = { "http://[\\w\\.]*?letitbit\\.net/d?download/(.*?\\.html|[0-9a-zA-z/.-]+)" }, flags = { 2 })
public class LetitBitNet extends PluginForHost {

    public LetitBitNet(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        this.setStartIntervall(90 * 1000l);
        enablePremium("http://letitbit.net/page/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "http://letitbit.net/page/terms.php";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        /* convert directdownload links to normal links */
        link.setUrlDownload(link.getDownloadURL().replaceAll("/ddownload", "/download"));
    }

    private void login(Account account) throws IOException, PluginException {
        setBrowserExclusive();
        br.getPage("http://letitbit.net/");
        br.postPage("http://letitbit.net/", "England.x=10&England.y=9&vote_cr=en");
        br.postPage("http://letitbit.net/iframe/iframe_git.php?action=login", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&txtcheck=login&txtlogin=");
        String check = br.getCookie("http://letitbit.net/", "pzddlk");
        if (check == null) check = br.getCookie("http://letitbit.net/", "pas");
        if (check == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.setFollowRedirects(true);
        br.getPage("http://premium.letitbit.net/index.php");
        br.getPage("http://premium.letitbit.net/mydata.php");
        if (!br.containsHTML("<strong>Premium</strong>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        if (account.getUser() == null || account.getUser().trim().length() == 0) {
            account.setValid(true);
            ai.setStatus("No Validation Check possible,PasswordAccount");
            return ai;
        }
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String validUntil = br.getRegex("Up to (\\d+-\\d+-\\d+ \\d+:\\d+:\\d+)").getMatch(0);
        account.setValid(true);
        if (validUntil != null) ai.setValidUntil(Regex.getMilliSeconds(validUntil, "dd-MM-yyyy HH:mm:ss", null));
        return ai;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://letitbit.net/", "lang", "en");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(<br>File not found<br />|Запрашиваемый файл не найден<br>|Request file .*? Deleted)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // /* set english language */
        // br.postPage(downloadLink.getDownloadURL(),
        // "en.x=10&en.y=8&vote_cr=en");
        String filename = br.getRegex("\"file-info\">File:: <span>(.*?)</span>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("name=\"realname\" value=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("class=\"first\">File:: <span>(.*?)</span></li>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("title>(.*?) download for free").getMatch(0);
                }
            }
        }
        String filesize = br.getRegex("name=\"sssize\" value=\"(\\d+)\"").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("<li>Size of file:: <span>(.*?)</span></li>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("\\[<span>(.*?)</span>\\]</h1>").getMatch(0);
            }
        }
        if (filename == null || filesize == null) {
            if (br.containsHTML("Request file.*?Deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Their names often differ from other file hosting services. I noticed
        // that when in the filenames from other hosting services there are
        // "-"'s, letitbit uses "_"'s so let's correct this here ;)
        downloadLink.setFinalFileName(filename.trim().replace("_", "-"));
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        String dlUrl = null;
        requestFileInformation(downloadLink);
        br.setDebug(true);
        if (account.getUser() == null || account.getUser().trim().length() == 0) {
            // Get to the premium zone
            br.postPage(downloadLink.getDownloadURL(), "if_not_ru=1");
            /* normal account with only a password */
            logger.info("Premium with pw only");
            Form premiumform = null;
            Form[] allforms = br.getForms();
            if (allforms == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            for (Form singleform : allforms) {
                if (singleform.containsHTML("pass") && singleform.containsHTML("uid5") && singleform.containsHTML("uid") && singleform.containsHTML("name") && singleform.containsHTML("pin") && singleform.containsHTML("realuid") && singleform.containsHTML("realname") && singleform.containsHTML("host") && singleform.containsHTML("ssserver") && singleform.containsHTML("sssize") && singleform.containsHTML("optiondir")) {
                    premiumform = singleform;
                    break;
                }
            }
            if (premiumform == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            premiumform.put("pass", Encoding.urlEncode(account.getPass()));
            br.submitForm(premiumform);
            dlUrl = getUrl(account);
        } else {
            /* account login */
            login(account);
            br.getPage(downloadLink.getDownloadURL());
            dlUrl = getUrl(account);
            if (dlUrl == null) {
                logger.info("Premium with indirectDL, enabling directDL first");
                br.postPage("http://premium.letitbit.net/ajax.php?action=setddlstate", "state=2");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else {
                logger.info("Premium with directDL");
            }
        }
        /* because there can be another link to a downlodmanager first */
        if (dlUrl == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        /* we have to wait little because server too buggy */
        sleep(5000, downloadLink);
        br.setDebug(true);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlUrl, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("Error")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 2 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        Form freeForm = br.getFormbyProperty("id", "ifree_form");
        if (freeForm == null) {
            logger.info("Found did not found freeForm!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.submitForm(freeForm);
        String url = null;
        Form down = null;
        Form[] allforms = br.getForms();
        if (allforms == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (Form singleform : allforms) {
            if (singleform.containsHTML("md5crypt")) {
                down = singleform;
                break;
            }
        }
        String captchaId = down.getVarsMap().get("uid");
        String captchaurl = null;
        if (down == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        URLConnectionAdapter con = null;
        if (br.containsHTML("cap\\.php")) {
            if (captchaId == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            captchaurl = "http://letitbit.net/cap.php?jpg=" + captchaId + ".jpg";
            con = br.openGetConnection(captchaurl);
            File file = this.getLocalCaptchaFile();
            Browser.download(file, con);
            con.disconnect();
            String code = getCaptchaCode(file, downloadLink);
            down.put("cap", code);
        }
        down.setMethod(Form.MethodType.POST);
        down.put("uid2", captchaId);
        down.setAction("http://letitbit.net/download3.php");
        br.submitForm(down);
        url = br.getRegex("<frame src=\"http://[a-z0-9A-Z\\.]*?letitbit.net/tmpl/tmpl_frame_top.php\\?link=(.*?)\"").getMatch(0);
        if (url == null || url.equals("")) {
            logger.info("Getting nextpage + ?link=");
            // Ticket Time
            int waitThat = 60;
            String time = br.getRegex("seconds = (\\d+);").getMatch(0);
            if (time == null) time = br.getRegex("Wait for Your turn: <span id=\"seconds\" style=\"font-size:18px\">(\\d+)</span> seconds").getMatch(0);
            if (time != null) {
                logger.info("Waittime found, waittime is " + time + " seconds.");
                waitThat = Integer.parseInt(time);
            }
            sleep((waitThat + 5) * 1001, downloadLink);
            prepareBrowser(br);
            /*
             * this causes issues in 09580 stable, no workaround known, please
             * update to latest jd version
             */
            br.postPage("http://letitbit.net/ajax/download3.php", "");
            /* letitbit and vipfile share same hosting server ;) */
            /* because there can be another link to a downlodmanager first */
            url = br.toString();
        }
        if (url == null || url.equals("") || !url.startsWith("http://") || url.length() > 1000) {
            logger.warning("url couldn't be found!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* we have to wait little because server too buggy */
        sleep(2000, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 1);
        con = dl.getConnection();
        if (con.getResponseCode() == 404) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, null, 5 * 60 * 1001);
        }
        if (!con.isOK()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
        if (con.getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("<title>Error</title>")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void prepareBrowser(final Browser br) {
        /*
         * last time they did not block the useragent, we just need this stuff
         * below ;)
         */
        if (br == null) { return; }
        br.getHeaders().put("Pragma", "no-cache");
        br.getHeaders().put("Cache-Control", "no-cache");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Content-Length", "0");
    }

    private String getUrl(Account account) throws IOException {
        // This information can only be found before each download so lets set
        // it here
        String points = br.getRegex("\">Points:</acronym>(.*?)</li>").getMatch(0);
        String expireDate = br.getRegex("\">Expire date:</acronym> ([0-9-]+) \\[<acronym class").getMatch(0);
        if (expireDate == null) expireDate = br.getRegex("\">Period of validity:</acronym> ([0-9-]+) \\[<acronym class").getMatch(0);
        if (expireDate != null || points != null) {
            AccountInfo accInfo = new AccountInfo();
            // 1 point = 1 GB
            if (points != null) accInfo.setTrafficLeft(Regex.getSize(points.trim() + "GB"));
            if (expireDate != null) {
                accInfo.setValidUntil(Regex.getMilliSeconds(expireDate, "yyyy-MM-dd", null));
            } else {
                expireDate = br.getRegex("\"Total days remaining\">(\\d+)</acronym>").getMatch(0);
                if (expireDate == null) expireDate = br.getRegex("\"Days remaining in Your account\">(\\d+)</acronym>").getMatch(0);
                if (expireDate != null) accInfo.setValidUntil(System.currentTimeMillis() + (Long.parseLong(expireDate) * 24 * 60 * 60 * 1000));
            }
            account.setAccountInfo(accInfo);
        }
        String iFrame = br.getRegex("<iframe src=\"(/sms/.*?)\"").getMatch(0);
        if (iFrame == null) iFrame = br.getRegex("\"(/sms/check2_iframe\\.php\\?ids=[0-9_]+\\&ids_emerg=[0-9_]+\\&emergency_mode=)\"").getMatch(0);
        if (iFrame != null) br.getPage("http://letitbit.net" + iFrame);
        String url = br.getRegex("(http://[^/;(images) ]*?/download.*?/[^/; ]+?)(\"|')[^(Download Master)]*?(http://[^/; ]*?/download[^; ]*?/[^; ]*?)(\"|')").getMatch(2);
        if (url == null) {
            url = br.getRegex("(http://[^/;(images) ]*?/download[^; ]*?/[^; ]*?)(\"|')").getMatch(0);
            if (url == null) url = br.getRegex("\"(http://[0-9]{2,3}\\.[0-9]{2,3}\\.[0-9]{2,3}\\.[0-9]{2,3}/download\\d+/[^; ]*?)\"").getMatch(0);
        }
        return url;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
