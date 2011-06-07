//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datei.to", "sharebase.to" }, urls = { "http://(www\\.)?(sharebase\\.(de|to)/(files/|1,)|datei\\.to/datei/)[\\w]+\\.html", "blablablaInvalid_regex" }, flags = { 2, 2 })
public class DateiTo extends PluginForHost {

    public DateiTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://datei.to/premium");
    }

    private static final String APIPAGE          = "http://api.datei.to/";
    private static final String FILEIDREGEX      = "datei\\.to/datei/(.*?)\\.html";
    private static final String DOWNLOADPOSTPAGE = "http://datei.to/ajax/download.php";
    private static final String RECAPTCHATEXT    = "(Versuche es erneut, indem du|klickst und das Captcha erneut eingibst|>Deine Eingabe war leider falsch)";

    @Override
    public String getAGBLink() {
        return "http://datei.to/agb";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws MalformedURLException {
        if ("sharebase.to".equals(link.getHost())) {
            /* README: this is how to change hostname in 09581 stable */
            try {
                final Field pidField = link.getClass().getDeclaredField("host");
                pidField.setAccessible(true);
                pidField.set(link, "datei.to");
            } catch (Throwable e) {
                logger.severe("could not rewrite host: " + e.getMessage());
            }
        }
        String id = new Regex(link.getDownloadURL(), "(/files/|/1,)([\\w]+\\.html)").getMatch(1);
        if (id != null) link.setUrlDownload("http://datei.to/datei/" + id);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.postPage(APIPAGE, "key=YYMHGBR9SFQA0ZWA&info=COMPLETE&datei=" + new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0));
        if (!br.containsHTML("online") || br.containsHTML("offline")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex info = br.getRegex(";(.*?);(\\d+);");
        downloadLink.setFinalFileName(info.getMatch(0));
        downloadLink.setDownloadSize(SizeFormatter.getSize(info.getMatch(1)));
        return AvailableStatus.TRUE;
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://api.datei.to/", "info=jdLogin&Username=" + Encoding.urlEncode(account.getUser()) + "&Password=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("free")) {
            logger.info("Free account found->Not support->Disable");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (!br.containsHTML("premium") || br.containsHTML("false")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String expireDate = br.getRegex("premium;(\\d+)").getMatch(0);
        if (expireDate != null)
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate));
        else
            ai.setExpired(true);
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        br.postPage("http://api.datei.to/", "info=jdPremDown&Username=" + Encoding.urlEncode(account.getUser()) + "&Password=" + Encoding.urlEncode(account.getPass()) + "&datei=" + new Regex(downloadLink.getDownloadURL(), "datei\\.to/datei/(.*?)\\.html").getMatch(0));
        String dlUrl = br.toString();
        if (dlUrl == null || !dlUrl.startsWith("http") || dlUrl.length() > 500 || dlUrl.contains("no file")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        dlUrl = dlUrl.trim();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlUrl, true, 0);
        br.setFollowRedirects(true);
        if (dl.getConnection() == null || dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.severe("PremiumError: " + br.toString());
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage(DOWNLOADPOSTPAGE, "P=I&ID=" + new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0));
        if (br.containsHTML("(Aktuell lädst du bereits eine Datei herunter|Als Free-User kannst du nur 1 Datei gleichzeitig downloaden.)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.dateito.toomanysimultandownloads", "Too many simultan downloads"), 10 * 60 * 1000l);
        String reconnectWaittime = br.getRegex("Du musst <span style=\"font-weight:bold; color:#DD0000;\">(\\d+) Minuten</span>").getMatch(0);
        if (reconnectWaittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWaittime) * 60 * 1001l);
        String postData = br.getRegex("data: \"(.*?)\"").getMatch(0);
        if (postData == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        int wait = 30;
        String waittimeRegexed = br.getRegex("id=\"CDD\">(\\d+)</span> Sekunden").getMatch(0);
        if (waittimeRegexed != null) wait = Integer.parseInt(waittimeRegexed);
        sleep(wait * 1001l, downloadLink);
        br.postPage(DOWNLOADPOSTPAGE, postData);
        if (br.containsHTML("(Bitte versuche es in ein paar Minuten erneut|>Dies kann verschiedene Ursachen haben)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        String iD = br.getRegex("\"\\&ID=(.*?)\"").getMatch(0);
        if (iD == null) {
            logger.warning("ID is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!br.containsHTML("P=III")) {
            boolean failed = true;
            for (int i = 0; i <= 5; i++) {
                if (br.containsHTML("(Da hat etwas nicht geklappt|>Hast du etwa versucht, die Wartezeit zu umgehen)")) {
                    logger.info("Countdown or server-error");
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                String reCaptchaId = br.getRegex("Recaptcha\\.create\\(\"(.*?)\"").getMatch(0);
                if (reCaptchaId == null) {
                    logger.warning("reCaptchaId is null...");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                Form reCaptchaForm = new Form();
                reCaptchaForm.setMethod(Form.MethodType.POST);
                reCaptchaForm.setAction("http://datei.to/ajax/recaptcha.php");
                reCaptchaForm.put("Doing", "CheckCaptcha");
                reCaptchaForm.put("ID", iD);
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.setForm(reCaptchaForm);
                rc.setId(reCaptchaId);
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                rc.setCode(c);
                if (!br.containsHTML(RECAPTCHATEXT)) {
                    failed = false;
                    break;
                }
                br.postPage(DOWNLOADPOSTPAGE, postData);
            }
            if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        br.postPage(DOWNLOADPOSTPAGE, "P=IV&ID=" + iD);
        String dllink = br.toString();
        if (!dllink.startsWith("http") || dllink.length() > 500) {
            logger.warning("Invalid dllink...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.trim();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection() == null || dl.getConnection().getContentType().contains("html")) {
            logger.warning("The dllink doesn't seem to be a file...");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
