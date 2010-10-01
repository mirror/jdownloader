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
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "save.tv" }, urls = { "http://[\\w\\.]*?(save\\.tv|free\\.save\\.tv)/STV/M/obj/user/usShowVideoArchiveDetail\\.cfm\\?TelecastID=\\d+" }, flags = { 2 })
public class SaveTv extends PluginForHost {

    public SaveTv(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.save.tv/stv/s/obj/registration/RegPage1.cfm");
        setConfigElements();
    }

    private static final String NORANDOMNUMBERS         = "";
    private static final String USEORIGINALFILENAME     = "";
    private static final String PREFERADSFREE           = "";
    private static final String ADSFREEAVAILABLE        = "for=\"archive-layer-adfree\">Schnittliste vor dem Download / Streaming anwenden<";
    private static final String ADSFREEAVAILABLETEXT    = "Video ist ohne Werbung verfügbar";
    private static final String ADSFREEANOTVAILABLETEXT = "Videos ohne Werbung werden bevorzugt, dieses ist aber nur mit Werbung verfügbar";
    private static final String FREEPOSTPAGE            = "https://www.save.tv/STV/M/Index.cfm?sk=freesave";
    private static final String PREMIUMPOSTPAGE         = "https://www.save.tv/STV/M/Index.cfm?sk=PREMIUM";

    private void setConfigElements() {
        ConfigEntry cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), NORANDOMNUMBERS, JDL.L("plugins.hoster.SaveTv.DontModifyFilename", "Keine Zufallszahlen an Dateinamen anhängen (kann Probleme verursachen)")).setDefaultValue(false);
        ConfigEntry cond2 = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USEORIGINALFILENAME, JDL.L("plugins.hoster.SaveTv.UseOriginalFilename", "Original Dateinamen verwenden (erst beim Download sichtbar)")).setDefaultValue(false);
        ConfigEntry cond3 = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFERADSFREE, JDL.L("plugins.hoster.SaveTv.PreferAdFreeVideos", "Geschnittene Videos (Videos ohne Werbung) bevorzugen")).setDefaultValue(false);
        config.addEntry(cond);
        config.addEntry(cond2);
        config.addEntry(cond3);
    }

    @Override
    public String getAGBLink() {
        return "http://free.save.tv/STV/S/misc/miscShowTermsConditionsInMainFrame.cfm";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        for (int i = 0; i <= 1; i++) {
            String acctype = this.getPluginConfig().getStringProperty("premium", null);
            if (acctype != null) {
                extendedLogin("http://www.save.tv/STV/S/misc/home.cfm?", PREMIUMPOSTPAGE, Encoding.urlEncode(account.getUser()), Encoding.urlEncode(account.getPass()));
                break;
            } else {
                extendedLogin("http://free.save.tv/", FREEPOSTPAGE, Encoding.urlEncode(account.getUser()), Encoding.urlEncode(account.getPass()));
                if (br.containsHTML("id=\"loginform\"")) {
                    this.getPluginConfig().setProperty("premium", "1");
                    this.getPluginConfig().save();
                    continue;
                }
                this.getPluginConfig().setProperty("premium", Property.NULL);
                this.getPluginConfig().save();
                break;
            }
        }
        if (!br.containsHTML("<frame") || br.containsHTML("Bitte verifizieren Sie Ihre Logindaten")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String acctype = this.getPluginConfig().getStringProperty("premium", null);
        if (acctype != null) {
            ai.setStatus("Premium save.tv User");
        } else {
            ai.setStatus("Free save.tv User");
        }
        String tic = br.getRegex("tic=(\\d+)").getMatch(0);
        if (tic != null) {
            String ticlink = "http://free.save.tv/STV/M/obj/user/usShowVideoArchive.cfm?&sk=JOE&tic=";
            if (acctype != null) ticlink = "http://save.tv/STV/M/obj/user/usShowVideoArchive.cfm?&sk=JOE&tic=";
            br.getPage(ticlink + tic);
            String minsleft = br.getRegex("Restzeit.*?(\\d+).*?Min").getMatch(0);
            logger.info("The user got " + minsleft + " minutes left to record");
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        String addedlink = downloadLink.getDownloadURL();
        if (this.getPluginConfig().getStringProperty("premium", null) != null) {
            addedlink = addedlink.replace("free.save.tv", "save.tv");
        } else {
            if (!addedlink.contains("free.save.tv")) addedlink = addedlink.replace("save.tv", "free.save.tv");
        }
        logger.info("Added link = " + addedlink);
        br.getPage(addedlink);
        // On their page this step is made by java script but leaving some vars
        // out, it still works :D
        boolean preferAdsFree = getPluginConfig().getBooleanProperty(PREFERADSFREE);
        String downloadWithoutAds = "false";
        if (preferAdsFree) downloadWithoutAds = "true";
        String postThat = "ajax=true&clientAuthenticationKey=&callCount=1&c0-scriptName=null&c0-methodName=GetDownloadUrl&c0-id=&c0-param0=number:" + new Regex(addedlink, "TelecastID=(\\d+)").getMatch(0) + "&c0-param1=number:0&c0-param2=boolean:" + downloadWithoutAds + "&xml=true&";
        br.postPage("http://www.save.tv/STV/M/obj/cRecordOrder/croGetDownloadUrl.cfm?null.GetDownloadUrl", postThat);
        String dllink = br.getRegex("\\[ 'OK','(http://.*?)\\',\\'").getMatch(0);
        if (dllink == null) dllink = br.getRegex("'(http://.*?/\\?m=dl)'").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (dllink) is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Final downloadlink = " + dllink + " starting download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -5);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        boolean useOriginalFilename = getPluginConfig().getBooleanProperty(USEORIGINALFILENAME);
        if (useOriginalFilename)
            downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
        else
            downloadLink.setFinalFileName(downloadLink.getName());
        dl.startDownload();
    }

    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) return false;
        try {
            Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa.toString().contains("false")) throw new PluginException(LinkStatus.ERROR_FATAL, "Kann Links ohne gültigen Account nicht überprüfen");
            br.setFollowRedirects(true);
            String acctype = this.getPluginConfig().getStringProperty("premium", null);
            if (acctype != null) {
                extendedLogin("http://www.save.tv/STV/S/misc/home.cfm?", PREMIUMPOSTPAGE, Encoding.urlEncode(aa.getUser()), Encoding.urlEncode(aa.getPass()));
            } else {
                extendedLogin("http://free.save.tv", FREEPOSTPAGE, Encoding.urlEncode(aa.getUser()), Encoding.urlEncode(aa.getPass()));
            }
            boolean useOriginalFilename = getPluginConfig().getBooleanProperty(USEORIGINALFILENAME);
            boolean dontModifyFilename = getPluginConfig().getBooleanProperty(NORANDOMNUMBERS);
            boolean preferAdsFree = getPluginConfig().getBooleanProperty(PREFERADSFREE);
            for (DownloadLink dl : urls) {
                String addedlink = dl.getDownloadURL();
                if (acctype != null) {
                    addedlink = addedlink.replace("free.save.tv", "save.tv");
                } else {
                    if (!addedlink.contains("free.save.tv")) addedlink = addedlink.replace("save.tv", "free.save.tv");
                }
                br.getPage(addedlink);
                if (br.containsHTML("(Leider ist ein Fehler aufgetreten|Bitte versuchen Sie es später noch einmal)")) {
                    dl.setAvailable(false);
                } else {
                    String filename = br.getRegex("<h2 id=\"archive-detailbox-title\">(.*?)</h2>").getMatch(0);
                    if (filename == null) filename = br.getRegex("id=\"telecast-detail\">.*?<h3>(.*?)</h2>").getMatch(0);
                    if (filename != null) {
                        filename = filename.trim();
                        filename = filename.replaceAll("(\r|\n)", "");
                        String[] unneededSpaces = new Regex(filename, ".*?([ ]{2,}).*?").getColumn(0);
                        if (unneededSpaces != null && unneededSpaces.length != 0) {
                            for (String unneededSpace : unneededSpaces) {
                                filename = filename.replace(unneededSpace, " ");
                            }
                        }
                        if (preferAdsFree && br.containsHTML(ADSFREEAVAILABLE))
                            dl.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SaveTv.AdsFreeAvailable", ADSFREEAVAILABLETEXT));
                        else if (preferAdsFree && !br.containsHTML(ADSFREEAVAILABLE)) dl.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SaveTv.AdsFreeNotAvailable", ADSFREEANOTVAILABLETEXT));
                        if (!dontModifyFilename || useOriginalFilename) filename = filename + new Random().nextInt(1000);
                        dl.setName(filename + ".avi");
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

    public void extendedLogin(String accessSite, String postPage, String user, String password) throws Exception {
        br.getPage(accessSite);
        String postData = "sUsername=" + Encoding.urlEncode_light(user) + "&sPassword=" + Encoding.urlEncode_light(password) + "&image.x=" + new Random().nextInt(100) + "&image.y=" + new Random().nextInt(100) + "&image=Login&bAutoLoginActivate=1";
        br.postPage(postPage, postData);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

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