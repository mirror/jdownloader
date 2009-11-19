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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.AccountController;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "save.tv" }, urls = { "http://[\\w\\.]*?free\\.save\\.tv/STV/M/obj/recordOrder/reShowDownload\\.cfm\\?TelecastID=[0-9]+" }, flags = { 2 })
public class SaveTv extends PluginForHost {

    public SaveTv(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.save.tv/stv/s/obj/registration/RegPage1.cfm");
    }

    @Override
    public String getAGBLink() {
        return "http://free.save.tv/STV/S/misc/miscShowTermsConditionsInMainFrame.cfm";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://free.save.tv");
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.put("sUsername", Encoding.urlEncode(account.getUser()));
        form.put("sPassword", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        if (!br.containsHTML("<frame") || br.containsHTML("Bitte verifizieren Sie Ihre Logindaten")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        // Just a little komfortable feature for all save.tv users ;)
        br.getPage("http://free.save.tv/STV/M/obj/user/usShowVideoArchive.cfm?&sk=JOE");
        String[] links = br.getRegex("\"(/STV/M/obj/recordOrder/reShowDownload\\.cfm\\?TelecastID=.*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Didn't find any links");
        } else {
            logger.info("**********VIDEOLINKS START**********");
            for (String link : links) {
                logger.info("***VIDEOLINKS = http://free.save.tv" + link + "***");
            }
            logger.info("**********VIDEOLINKS END**********");
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
        String tic = br.getRegex("tic=(\\d+)").getMatch(0);
        if (tic != null) {
            br.getPage("http://free.save.tv/STV/M/obj/user/usShowVideoArchive.cfm?&sk=JOE&tic=" + tic);
            String minsleft = br.getRegex("Restzeit.*?(\\d+).*?Min").getMatch(0);
            if (minsleft != null) ai.setTrafficLeft(minsleft + "MB");
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        logger.info("Added link = " + downloadLink.getDownloadURL());
        String dllink = br.getRegex("document\\.location\\.href='(http.*?)'").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("'(http://.*?/\\?m=dl)'").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("reShowStreamOnload\\.cfm\\?sURL=(http.*?)'").getMatch(0);
                if (dllink != null) {
                    dllink = Encoding.deepHtmlDecode((dllink));
                    br.getPage(dllink);
                    dllink = br.getRegex("video/divx\" src=\"(.*?)\"").getMatch(0);
                    if (dllink == null) dllink = br.getRegex("\"src\" value=\"(.*?)\"").getMatch(0);
                }
            }
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (dllink) is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Final downloadlink = " + dllink + " starting download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -5);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa.toString().contains("false")) throw new PluginException(LinkStatus.ERROR_FATAL, "Kann Links ohne gültigen Account nicht überprüfen");
            Regex accdata = new Regex(aa.toString(), "(.*?):(.*?)$");
            String username = accdata.getMatch(0);
            String password = accdata.getMatch(1).replace("true", "").trim();
            br.setFollowRedirects(true);
            br.getPage("http://free.save.tv");
            Form loginform = br.getForm(0);
            if (loginform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            loginform.put("sUsername", username);
            loginform.put("sPassword", password);
            br.submitForm(loginform);
            for (DownloadLink dl : urls) {
                String addedlink = dl.getDownloadURL();
                br.getPage(addedlink);
                if (br.containsHTML("(Leider ist ein Fehler aufgetreten|Bitte versuchen Sie es später noch einmal)")) {
                    dl.setAvailable(false);
                } else {
                    String filename = br.getRegex("rand_ueberall\" cellpadding.*?<b>(.*?)</b><br>").getMatch(0);
                    if (filename == null) filename = br.getRegex("rowspan=.*?width=.*?>.*?<b>(.*?)</b><br>").getMatch(0);
                    if (filename != null) {
                        dl.setFinalFileName(filename.trim() + new Random().nextInt(10) + ".avi");
                        dl.setAvailable(true);
                    } else {
                        dl.setAvailable(false);
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    // Old available check, very bad...
    // @Override
    // public AvailableStatus requestFileInformation(DownloadLink link) throws
    // IOException, PluginException {
    // this.setBrowserExclusive();
    // Account aa = AccountController.getInstance().getValidAccount(this);
    // if (aa.toString().contains("false")) throw new
    // PluginException(LinkStatus.ERROR_FATAL,
    // "Kann Links ohne gültigen Account nicht überprüfen");
    // Regex accdata = new Regex(aa.toString(), "(.*?):(.*?)$");
    // String username = accdata.getMatch(0);
    // String password = accdata.getMatch(1).replace("true", "").trim();
    // br.setFollowRedirects(true);
    // br.getPage("http://free.save.tv");
    // Form loginform = br.getForm(0);
    // if (loginform == null) throw new
    // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // loginform.put("sUsername", username);
    // loginform.put("sPassword", password);
    // try {
    // br.submitForm(loginform);
    // } catch (Exception e) {
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // br.getPage(link.getDownloadURL());
    // br.setFollowRedirects(false);
    // if (br.containsHTML("File does not exist")) throw new
    // PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    // if
    // (br.containsHTML("(Leider ist ein Fehler aufgetreten|Bitte versuchen Sie es später noch einmal)"))
    // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    // String filename =
    // br.getRegex("rand_ueberall\" cellpadding.*?<b>(.*?)</b><br>").getMatch(0);
    // if (filename == null) filename =
    // br.getRegex("rowspan=.*?width=.*?>.*?<b>(.*?)</b><br>").getMatch(0);
    // if (filename == null) throw new
    // PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    // link.setFinalFileName(filename.trim() + new Random().nextInt(10) +
    // ".avi");
    // return AvailableStatus.TRUE;
    // }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        }
        return downloadLink.getAvailableStatus();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for registered users");
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