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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share.cx" }, urls = { "http://[\\w\\.]*?share\\.cx/files/\\d+" }, flags = { 2 })
public class ShareCx extends PluginForHost {

    public ShareCx(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.share.cx/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://www.share.cx/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        String filename = br.getRegex("alt=\"Download\" /></span>(.*?)</h3>").getMatch(0);
        if (filename == null) filename = br.getRegex("name=\"fname\" value=\"(.*?)\"").getMatch(0);
        String filesize = br.getRegex("\">Dateigröße:(.*?)</div>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (filename.trim().equals("") || filesize.trim().equals("")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        Form dlform0 = br.getForm(0);
        if (dlform0 == null) {
            logger.warning("dlform0 could not be found!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dlform0.put("method_free", "Datei+herunterladen");
        br.submitForm(dlform0);
        String reconTime = br.getRegex("startTimer\\((\\d+)\\)").getMatch(0);
        if (reconTime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconTime) * 1001l);
        if (br.containsHTML("Sie haben Ihr Download-Limit von")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        Form dlform1 = br.getForm(0);
        if (dlform1 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Ticket Time
        String ttt = new Regex(br.toString(), "countdown\">.*?(\\d+).*?</span>").getMatch(0);
        if (ttt == null) ttt = new Regex(br.toString(), "id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span").getMatch(0);
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
            int tt = Integer.parseInt(ttt);
            sleep(tt * 1001, downloadLink);
        }
        if (br.containsHTML("/captchas/")) {
            logger.info("Detected captcha method \"Standard captcha\" for this host");
            String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
            String captchaurl = null;
            if (sitelinks == null || sitelinks.length == 0) {
                logger.warning("Standard captcha captchahandling broken!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (String link : sitelinks) {
                if (link.contains("/captchas/")) {
                    captchaurl = link;
                    break;
                }
            }
            if (captchaurl == null) {
                logger.warning("Standard captcha captchahandling broken!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String code = getCaptchaCode(captchaurl, downloadLink);
            dlform1.put("code", code);
            logger.info("Put captchacode " + code + " obtained by captcha metod \"Standard captcha\" in the form.");
        } else {
            logger.info("Couldn't find a captcha, continuing without captcha...");
        }
        br.submitForm(dlform1);
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            String waittime = br.getRegex("startTimer\\((\\d+)\\);").getMatch(0);
            if (waittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 1001l);
            logger.warning("dllink equals null, stopping...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage("http://www.share.cx");
        Form form = br.getFormbyProperty("name", "FL");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.put("login", Encoding.urlEncode(account.getUser()));
        form.put("password", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        if (br.getCookie("http://www.share.cx", "login") == null || br.getCookie("http://www.share.cx", "xfss") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("http://www.share.cx/myaccount");
        String accType = br.getRegex(">Account</TD><TD><b><a href=\"http://www\\.share\\.cx/premium\">(Premium)</a>").getMatch(0);
        if (accType == null) {
            logger.info("This account is no premium account!");
            account.setValid(false);
            return ai;
        }
        String usedSpace = br.getRegex("Belegter Speicherplatz</TD><TD>(.*?)</TD>").getMatch(0);
        if (usedSpace != null) ai.setUsedSpace(usedSpace);
        String expireDate = br.getRegex("ltig bis</TD><TD>(\\d+\\.\\d+\\.\\d+)</TD>").getMatch(0);
        if (expireDate != null) {
            ai.setValidUntil(Regex.getMilliSeconds(expireDate, "dd.MM.yyyy", null));
        } else {
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Premium User");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        String dllink = null;
        if (br.getRedirectLocation() != null) {
            dllink = br.getRedirectLocation();
        } else {
            Form dlform = br.getFormbyProperty("name", "F1");
            br.submitForm(dlform);
            dllink = br.getRegex("wenigen Sekunden automatisch... <a href=\"(http://.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("window\\.location\\.href='(http://file\\d+\\.share\\.cx.*?)'\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("(\"|')(http://file\\d+\\.share\\.cx:\\d+/d/[a-z0-9]+/.*?)(\"|')").getMatch(1);
                }
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -10);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void handleErrors() throws Exception {
        logger.info("Checking for errors...");
        // This error shows up if you try to download multiple files at the
        // same time
        if (br.containsHTML("<br>oder kaufen Sie sich jetzt einen <a")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        if (br.containsHTML("No File")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}