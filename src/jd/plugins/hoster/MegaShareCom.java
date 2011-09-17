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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megashare.com" }, urls = { "http://[\\w\\.]*?megashare\\.com/[0-9]+" }, flags = { 2 })
public class MegaShareCom extends PluginForHost {

    private static final String UA     = RandomUserAgent.generate();
    private Form                DLFORM = null;

    public MegaShareCom(final PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2000l);
        this.enablePremium("http://www.megashare.com/premium.php");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.megashare.com/tos.php";
    }

    private void getForm(final List<String> remove) {
        String f1 = br.toString().replaceAll("<input type=\"submit\".*?>", "");
        if (remove.contains("image")) {
            final String[] klickForm = new Regex(f1, "<input type=\"image\".*?name=\"(.*?)\"").getColumn(0);
            Arrays.sort(klickForm);
            for (int i = 0; i < klickForm.length - 1; i++) {
                if (klickForm[i].equals(klickForm[i + 1])) {
                    remove.add(klickForm[i]);
                }
            }
            f1 = f1.replaceAll("<input type=\"image\".*?>", "");
        }
        final Form[] getAll = Form.getForms(f1);
        for (final Form element : getAll) {
            if (element.getAction() == null) {
                DLFORM = element;
                break;
            }
        }
        if (remove.contains("image")) {
            DLFORM.put(remove.get(remove.size() - 1) + ".x", String.valueOf(Math.round(Math.random() * 100)));
            DLFORM.put(remove.get(remove.size() - 1) + ".y", String.valueOf(Math.round(Math.random() * 100)));
        }
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
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.forceDebug(true);
        final String reconnectWaittime = br.getRegex("var c = (\\d+);").getMatch(0);
        final String cDelay = br.getRegex("(var )?cDly ?= ?(\\d+);").getMatch(1);
        if (reconnectWaittime != null) {
            if (Integer.parseInt(reconnectWaittime) >= 299) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWaittime) * 1001l); }
        }

        /* FORM_POST_1 */
        final List<String> remove = new ArrayList<String>();
        remove.add("submit");
        getForm(remove);
        if (DLFORM == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        DLFORM.put("wComp", "1");
        /* waittime */
        int wait = 10;
        long cDly = 1001;
        if (reconnectWaittime != null) {
            wait = Integer.parseInt(reconnectWaittime);
        }
        if (cDelay != null) {
            cDly = Long.parseLong(cDelay);
            cDly = cDly + cDly / 1000;
        }
        sleep(wait * cDly, downloadLink);
        br.submitForm(DLFORM);

        /* FORM_POST_2 */
        remove.add("image");
        getForm(remove);
        if (DLFORM == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        /* captcha */
        final File captchaFile = getLocalCaptchaFile();
        int i = 15;
        while (i-- > 0) {
            if (!br.containsHTML("security\\.php\\?i=")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            String captchaimg = br.getRegex("id=\"cimg\" src=\"(.*?)\"").getMatch(0);
            if (captchaimg == null) {
                captchaimg = br.getRegex("src=\"(security\\.php.*?)\"></div>").getMatch(0);
            }
            if (captchaimg == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            captchaimg = "http://www.megashare.com/" + captchaimg;
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaimg));
            final String hash = JDHash.getMD5(captchaFile);
            // Seems to be a captchaerror (captcahs without any letters)
            if (hash.equals("eb92a5ddf69784ee2de24bca0c6299d4") || hash.equals("d054cfcd69daca6fe8b8d84f3ece9be3")) {
                continue;
            } else {
                break;
            }
        }
        String captchaCode = null;
        for (int o = 0; o <= 3; o++) {
            captchaCode = getCaptchaCode(captchaFile, downloadLink);
            if (captchaCode.length() == 5) {
                break;
            }
        }
        if (captchaCode.length() != 5) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        DLFORM.put("captcha_code", captchaCode);
        String passCode = null;
        if (br.containsHTML("This file is password protected.")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            DLFORM.put("auth_nm", passCode);
        }
        br.submitForm(DLFORM);

        /* FORM_POST_3 */
        remove.clear();
        getForm(remove);

        final String dllink = Encoding.htmlDecode(DLFORM.getRegex("name=\"link\".*?value=\"(.*?)\"").getMatch(0));
        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        // Unlimited chunks are possible but cause servererrors
        // ("DOWNLOAD_IMCOMPLETE")
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("Invalid Captcha Value")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
            if (br.containsHTML("This file is password protected.")) {
                logger.warning("Wrong password!!");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("get premium access")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        br.postPage(br.getURL(), "PremDz.x=" + new Random().nextInt(100) + "&PremDz.y=" + new Random().nextInt(100) + "&PremDz=PREMIUM");
        if (br.containsHTML("This File has been DELETED")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final Form form = br.getFormbyProperty("name", "downloader");
        if (form == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        final String id = form.getVarsMap().get("id");
        final String timeDiff = form.getVarsMap().get("time_diff");
        if (id == null || timeDiff == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        String post = "yesss.x=" + new Random().nextInt(100) + "&yesss.y=" + new Random().nextInt(100) + "&yesss=Download&id=" + id + "&time_diff=" + timeDiff + "&req_auth=n";
        String passCode = null;
        // This password handling is probably broken
        if (br.containsHTML("This file is password protected.")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            post += "&auth_nm=" + passCode;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getURL(), post, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Invalid Captcha Value")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
            if (br.containsHTML("This file is password protected.")) {
                logger.warning("Wrong password!");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    public void login(final Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        final String post = "loginid=" + Encoding.urlEncode(account.getUser()) + "&passwd=" + Encoding.urlEncode(account.getPass()) + "&yes=submit";
        br.postPage("http://www.megashare.com/login.php", post);
        br.setFollowRedirects(false);
        if (br.getCookie("http://www.megashare.com", "username") == null || br.getCookie("http://www.megashare.com", "password") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.getHeaders().put("User-Agent", UA);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Not Found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final Form freeForm = br.getForm(1);
        final String crap = br.getRegex("src=\"images/dwn\\-btn3\\.gif\" name=\"(.*?)\"").getMatch(0);
        if (freeForm == null || crap == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        freeForm.put(Encoding.urlEncode(crap) + ".x", Integer.toString(new Random().nextInt(100)));
        freeForm.put(Encoding.urlEncode(crap) + ".y", Integer.toString(new Random().nextInt(100)));
        br.submitForm(freeForm);
        if (br.containsHTML("This File has been DELETED")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String filename = br.getRegex("addthis_open\\(this, \\'\\', \\'http://(www\\.)?MegaShare\\.com\\d+\\', \\'(.*?)\\'\\)").getMatch(1);
        if (filename != null) {
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {

    }

    @Override
    public void resetPluginGlobals() {
    }
}
