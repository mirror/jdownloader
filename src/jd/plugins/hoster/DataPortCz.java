//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dataport.cz" }, urls = { "http://(www\\.)?dataport\\.cz/file/[\\w\\-]+" }, flags = { 2 })
public class DataPortCz extends PluginForHost {

    private static final String MAINPAGE = "http://dataport.cz/";

    public DataPortCz(PluginWrapper wrapper) {
        super(wrapper);
        // Didn't find a premiumlink
        this.enablePremium();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        // Maybe old case
        if (br.containsHTML(">Please click here to continue<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Link offline
        if (br.containsHTML("alert\\(\"Tento soubor neexistuje\"\\)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">Název</td>[\t\n\r ]+<td><span itemprop=\"name\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null) filename = br.getRegex("<h2>([^<>\"]*?)</h2>").getMatch(0);
        final String filesize = br.getRegex("<td class=\"fil\">Velikost</td>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Set final filename here because server sends us bad filenames
        link.setFinalFileName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
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
        br.getPage(MAINPAGE);
        String availabletraffic = br.getRegex("/credit/buy\">(.*?)</").getMatch(0);
        if (availabletraffic != null) {
            account.setValid(true);
            ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic.replace(" ", "")));
        } else {
            account.setValid(false);
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://dataport.cz/pravidla-pouziti/";
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
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Počet volných slotů: <span class=\"darkblue\">0</span>")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.dataportcz.nofreeslots", "No free slots available, wait or buy premium"), 10 * 60 * 1000l);
        final String captchaLink = br.getRegex("\"(/captcha/\\d+\\.png)\"").getMatch(0);
        final Form capForm = br.getFormbyProperty("id", "free_download_form");
        if (captchaLink == null || capForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String code = getCaptchaCode("http://dataport.cz" + captchaLink, downloadLink);
        capForm.put("captchaCode", code);
        br.setFollowRedirects(false);
        br.submitForm(capForm);
        final String dllink = br.getRedirectLocation();
        if (dllink == null) {
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains("?_fid=")) {
            br.getPage(dllink);
            if (br.containsHTML("Počet volných slotů: <span class=\"darkblue\">0</span>")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available", 5 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        String dllink = br.getRegex("><strong>Stažení ZDARMA</strong></a> <a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://dataport\\.cz/stahuj\\-soubor/.*?)\"").getMatch(0);
        if (dllink == null) {
            Form form = br.getForm(2);
            br.submitForm(form);
            dllink = br.getRedirectLocation();
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.postPage("http://www.dataport.cz/?do=loginForm-submit", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&loginFormSubmit=");
        if (br.getCookie(MAINPAGE, "PHPSESSID") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (br.getCookie(MAINPAGE, "nette-browser") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}