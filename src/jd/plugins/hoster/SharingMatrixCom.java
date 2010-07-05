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

import java.io.File;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharingmatrix.com" }, urls = { "http://[\\w\\.]*?sharingmatrix\\.com/file/[0-9]+" }, flags = { 2 })
public class SharingMatrixCom extends PluginForHost {

    public SharingMatrixCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://sharingmatrix.com/premium");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://sharingmatrix.com/contact";
    }

    private static final String WAIT1 = "WAIT1_1";

    private void setConfigElements() {
        ConfigEntry cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), WAIT1, JDL.L("plugins.hoster.SharingMatrixCom.waitInsteadOfReconnect", "Wait 15 minutes instead of reconnecting")).setDefaultValue(false);
        config.addEntry(cond);
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        /*
         * set language for session correct
         */
        br.getPage("http://sharingmatrix.com/en/");
        br.getPage("http://sharingmatrix.com/login");
        br.getPage("http://sharingmatrix.com/ajax_scripts/login.php?email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember_me=true");
        String validornot = br.toString();
        String number = new Regex(validornot, "(\\d+)").getMatch(0);
        if (number != null) validornot = number;
        if (!validornot.equals("1")) {
            logger.info("Login failed");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
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
        br.getPage("http://sharingmatrix.com/personal#page-settings_a");
        boolean isPremium = !br.containsHTML("class=\"lUpgradePremium");
        br.getPage("http://sharingmatrix.com/ajax_scripts/personal.php?query=homepage");
        String expiredate = br.getRegex("([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2})").getMatch(0);
        String daysleft = br.getRegex(",.*?([0-9]{1,3}).*?day\\(s\\) left").getMatch(0);
        if (expiredate != null) {
            ai.setStatus("Premium User");
            ai.setValidUntil(Regex.getMilliSeconds(expiredate, "yyyy-MM-dd HH:mm:ss", null));
            account.setValid(true);
            return ai;
        }
        if (daysleft != null) {
            ai.setValidUntil(System.currentTimeMillis() + (Long.parseLong(daysleft) * 24 * 60 * 60 * 1000));
            account.setValid(true);
            return ai;
        }
        if (isPremium) {
            account.setValid(true);
        } else {
            logger.info("Both expire-date regexes failed, this account seems not to be a premium account...");
            account.setValid(false);
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        String passCode = null;
        requestFileInformation(downloadLink);
        login(account);
        String dllink = downloadLink.getDownloadURL();
        br.getPage(dllink);
        String url = br.getRedirectLocation();
        boolean direct = true;
        if (url == null) {
            if (br.containsHTML("<p>Upgrade to a premium account and")) {
                logger.info("This account doesn't seem to be a premium account...disabling it");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (br.containsHTML("download limit of 10Gb is over")) {
                logger.info(JDL.L("plugins.hoster.sharingmatrixcom.limit", "Deposit: We are sorry, but your daily Premium user's download limit of 10Gb is over."));
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (br.containsHTML("no available free download slots left")) {
                logger.info(JDL.L("plugins.hoster.sharingmatrixcom.buggy", "Buggy Server: enable DirectDownload as workaround"));
                br.getPage("http://sharingmatrix.com/ajax_scripts/personal.php?query=settings");
                Form form = br.getForm(0);
                Form newform = new Form();
                newform.setAction("http://sharingmatrix.com/ajax_scripts/personal/settings.php");
                newform.setMethod(Form.MethodType.GET);
                newform.put("id", br.getCookie("http://sharingmatrix.com", "user_id"));
                if (br.getCookie("http://sharingmatrix.com", "user_id") == null || br.getCookie("http://sharingmatrix.com", "user_id").length() == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                newform.put("type_membership", "premium");
                newform.put("nickname", form.getVarsMap().get("nickname") != null ? form.getVarsMap().get("nickname") : "");
                newform.put("email", form.getVarsMap().get("email") != null ? form.getVarsMap().get("email") : "");
                newform.put("password", form.getVarsMap().get("password") != null ? form.getVarsMap().get("password") : "");
                newform.put("fast_link", "true");
                br.getPage("http://sharingmatrix.com/personal");
                br.submitForm(newform);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            direct = false;
            if (br.containsHTML("Enter password:<") && !br.containsHTML("enter_password\" style=\"display:none\"")) {
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput(JDL.L("plugins.hoster.sharingmatrixcom.password", "Password?"), downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
            }
            String linkid = br.getRegex("link_id = '(\\d+)';").getMatch(0);
            String link_name = br.getRegex("link_name = '([^']*')").getMatch(0);
            if (linkid == null || link_name == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            Browser brc = br.cloneBrowser();
            brc.getPage("http://www.sharingmatrix.com/ajax_scripts/_get.php?link_id=" + linkid + "&link_name=" + link_name + "&dl_id=0&prem=1" + (passCode == null ? "" : "&password=" + Encoding.urlEncode(passCode)));
            if (brc.containsHTML("server_down")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.sharingmatrixcom.failure", "Serverfailure, Please try again later!"), 30 * 60 * 1000l);
            String server = brc.getRegex("serv:\"(http://.*?)\"").getMatch(0);
            String hash = brc.getRegex("hash:\"(.*?)\"").getMatch(0);
            if (server == null || hash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            url = server + "/download/" + hash + "/0/" + (passCode == null ? "" : Encoding.urlEncode(passCode));
        }
        br.setFollowRedirects(true);
        if (direct) {
            passCode = downloadLink.getStringProperty("pass", null);
            if (passCode != null) url = url + passCode;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
        if (dl.getConnection() != null && dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("We are sorry but")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.sharingmatrixcom.maintenance", "Server maintenance"), 60 * 60 * 1000l);
            if (br.containsHTML("download limit of 10Gb is over")) {
                logger.info(JDL.L("plugins.hoster.sharingmatrixcom.limit", "Deposit: We are sorry, but your daily Premium user's download limit of 10Gb is over."));
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (br.containsHTML("Incorrect password for this file.")) {
                logger.info(JDL.L("plugins.hoster.sharingmatrixcom.wrongpass", "Password wrong"));
                downloadLink.setProperty("pass", null);
                if (direct) {
                    passCode = Plugin.getUserInput(JDL.L("plugins.hoster.sharingmatrixcom.password", "Password?"), downloadLink);
                    downloadLink.setProperty("pass", passCode);
                }
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    // The hoster allows only 10 simultan connections so eighter 10 chunks or 10
    // simultan downloads. If the controller is extented one time we could make
    // this dynamically but right now i just set it to 10 max downloads because
    // many people might now know what chunks are and then they wonder that only
    // 1 download starts^^
    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        // Seems not to work if you access sharingmatrix in russia for example
        // so we just access the page t get the right language
        // br.setCookie("http://sharingmatrix.com", "lang", "en");
        br.getPage("http://sharingmatrix.com/en/");
        br.getPage(parameter.getDownloadURL());
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">Filename: <strong>(.*?)</strong>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("link_name = '(.*?)';").getMatch(0);
        }
        String filesize = br.getRegex(">File size: <strong>(.*?)</strong>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("fsize = '(.*?)';").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filesize = filesize.replace("&nbsp;", "");
        if (br.containsHTML("Only premium users are entitled to download files larger than")) parameter.getLinkStatus().setStatusText(JDL.L("plugins.hoster.sharingmatrixcom.only4premium", "This file is only downloadable for premium users!"));
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.forceDebug(true);
        requestFileInformation(downloadLink);
        String passCode = null;
        if (br.containsHTML("Only premium users are entitled to download files larger than")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.sharingmatrixcom.only4premium", "This file is only downloadable for premium users!"));
        if (br.containsHTML("You are already downloading file. Only premium")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
        if (br.containsHTML("no available free download slots left")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.sharingmatrixcom.nofreeslots", "No free slots available for this file"));
        if (br.containsHTML("daily download limit is over")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.sharingmatrixcom.dailylimit", "Daily limit reached"), 60 * 60 * 1000l);
        String linkid = br.getRegex("link_id = '(\\d+)';").getMatch(0);
        if (linkid == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        String freepage = "http://sharingmatrix.com/ajax_scripts/download.php?type_membership=free&link_id=" + linkid;
        br.getPage(freepage);
        String link_name = br.getRegex("link_name = '([^']+)'").getMatch(0);
        String ctjv = br.getRegex("ctjv = '([^']+)'").getMatch(0);
        int ctjvv = Integer.parseInt(ctjv);

        String captchalink = "http://sharingmatrix.com/include/crypt/cryptographp.inc.php?cfg=0&sn=PHPSESSID&";

        File captcha = getLocalCaptchaFile();

        Browser brc = br.cloneBrowser();
        brc.setCookiesExclusive(true);
        brc.setCookie("sharingmatrix.com", "cryptcookietest", "1");
        brc.getDownload(captcha, captchalink);
        Browser br2 = br.cloneBrowser();
        if (captcha.length() > 200) {
            // I'm not sure if there are still captchas

            String code = getCaptchaCode(captcha, downloadLink);

            if (br.containsHTML("Enter password:<") && !br.containsHTML("enter_password\" style=\"display:none\"")) {
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput(JDL.L("plugins.hoster.sharingmatrixcom.password", "Password?"), downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
            }
            br2.postPage("http://sharingmatrix.com/ajax_scripts/verifier.php", "?&code=" + code);
            if (br2.containsHTML("We are sorry but")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.sharingmatrixcom.maintenance", "Server maintenance"), 60 * 60 * 1000l);
            if (Integer.parseInt(br2.toString().trim()) != 1) throw new PluginException(LinkStatus.ERROR_CAPTCHA);

        }
        handleWaittime(downloadLink, ctjvv);
        String dl_id = br2.getPage("/ajax_scripts/dl.php").trim();
        br2.getPage("/ajax_scripts/_get2.php?link_id=" + linkid + "&link_name=" + link_name + "&dl_id=" + dl_id + (passCode == null ? "" : "&password=" + Encoding.urlEncode(passCode)));
        if (br2.containsHTML("server_down")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.sharingmatrixcom.failure", "Serverfailure, Please try again later!"), 30 * 60 * 1000l);
        String linkurl = br2.getRegex("serv:\"([^\"]+)\"").getMatch(0) + "/download/" + br2.getRegex("hash:\"([^\"]+)\"").getMatch(0) + "/" + dl_id.trim() + "/" + (passCode == null ? "" : "&password=" + Encoding.urlEncode(passCode));
        dl = jd.plugins.BrowserAdapter.openDownload(br2, downloadLink, linkurl, true, 1);
        if (dl.getConnection() != null && dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Incorrect password for this file.")) {
                logger.info(JDL.L("plugins.hoster.sharingmatrixcom.wrongpass", "Password wrong"));
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("Prove you are a human!")) {
                /* strange because captcha should be okay */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
            }
            if (br.containsHTML("You are already downloading file. Only premium")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    public void handleWaittime(DownloadLink downloadLink, int ctjvv) throws Exception {
        // Please leave this is, could be useful later...
        // logger.info("Checking for the first waittime...");
        // doWait2(downloadLink, ctjvv);
        // See if we can find a long waittime, if so we HAVE TO reconnect as it
        // is very big!
        logger.info("Checking if a 2nd waittime does exist...");
        Browser br3 = br.cloneBrowser();
        br3.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br3.getPage("http://sharingmatrix.com/ajax_scripts/check_timer.php?tmp=" + Math.round(Math.random() * 1000) + 1);
        String longwait = br3.getRegex(".*?(\\d+).*?").getMatch(0);
        if (longwait != null && Integer.parseInt(longwait) > 0) {
            logger.info("2nd waittime does exist, wait = " + longwait + " seconds.");
            ctjvv = Integer.parseInt(longwait);
            doWait2(downloadLink, ctjvv);
        } else {
            logger.info("No 2nd waittime found...");
        }
    }

    public void doWait2(DownloadLink downloadLink, int ctjvv) throws Exception {
        boolean waitReconnecttime = getPluginConfig().getBooleanProperty(WAIT1, false);
        /* we will wait up to 15 mins */
        if (ctjvv > 80) {
            if (waitReconnecttime && ctjvv < 910)
                sleep(ctjvv * 1001l, downloadLink);
            else
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, ctjvv * 1001l);
        } else {
            this.sleep(ctjvv * 1000, downloadLink);
        }
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