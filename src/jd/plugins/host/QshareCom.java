//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.host;

import java.io.File;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class QshareCom extends PluginForHost {
    // static private final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getFirstMatch().*= "0.1";
    // static private final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    static private final String CODER = "JD-Team";

    static private final String HOST = "qshare.com";
    // http://s1.qshare.com/get/246129/Verknuepfung_mit_JDownloader.exe.lnk.html
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?qshare\\.com\\/get\\/[0-9]{1,20}\\/.*", Pattern.CASE_INSENSITIVE);

    public QshareCom() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
        // setConfigElements();

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        String page = null;

        Browser br = new Browser();

        Browser.clearCookies("qshare.com");

        page = br.getPage(downloadLink.getDownloadURL());
        String[][] dat = new Regex(page, "<SPAN STYLE=\"font-size\\:13px\\;vertical\\-align\\:middle\">.*<\\!\\-\\- google_ad_section_start \\-\\->(.*?)<\\!\\-\\- google_ad_section_end \\-\\->(.*?)<\\/SPAN>").getMatches();

        if (dat == null || dat.length == 0) {

            // step.setStatus(PluginStep.STATUS_ERROR);

            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }
        Form[] forms = br.getForms();
        page = br.submitForm(forms[0]);

        String wait = new Regex(page, "Dein Frei-Traffic wird in ([\\d]*?) Minuten wieder").getFirstMatch();
        if (wait != null) {
            long waitTime = Long.parseLong(wait) * 60 * 1000;
            // step.setStatus(PluginStep.STATUS_ERROR);
            // step.setParameter(waitTime);
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            return;
        }
        String link = new Regex(page, "<div id=\"download_link\"><a href=\"(.*?)\"").getFirstMatch();

        if (link == null) {

            // step.setStatus(PluginStep.STATUS_ERROR);

            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }
        HTTPConnection con = br.openGetConnection(link);
        if (Plugin.getFileNameFormHeader(con) == null || Plugin.getFileNameFormHeader(con).indexOf("?") >= 0) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            sleep(20000, downloadLink);
            return;
        }
        dl = new RAFDownload(this, downloadLink, con);
        dl.setResume(false);
        dl.setChunkNum(1);

        dl.startDownload();

    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        String user = account.getUser();
        String pass = account.getPass();
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        return;

    }

    @Override
    public String getAGBLink() {

        return "http://s1.qshare.com/index.php?sysm=sys_page&sysf=site&site=terms";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        try {
            String page;
            // dateiname, dateihash, dateisize, dateidownloads, zeit bis
            // happyhour
            Browser br = new Browser();
            page = br.getPage(downloadLink.getDownloadURL());
            String[][] dat = new Regex(page, "<SPAN STYLE=\"font-size\\:13px\\;vertical\\-align\\:middle\">.*<\\!\\-\\- google_ad_section_start \\-\\->(.*?)<\\!\\-\\- google_ad_section_end \\-\\->(.*?)<\\/SPAN>").getMatches();

            downloadLink.setName(dat[0][0].trim());
            downloadLink.setDownloadMax((int) Regex.getSize(dat[0][1].trim()));
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        // if
        // (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM,
        // true) &&
        // this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false))
        // {
        // return 20;
        // } else {
        return 1;
        // }
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    // private void setConfigElements() {
    // ConfigEntry cfg;
    // config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD,
    // getProperties(), PROPERTY_PREMIUM_USER,
    // JDLocale.L("plugins.hoster.rapidshare.de.premiumUser", "Premium User")));
    // cfg.setDefaultValue("Kundennummer");
    // config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD,
    // getProperties(), PROPERTY_PREMIUM_PASS,
    // JDLocale.L("plugins.hoster.rapidshare.de.premiumPass", "Premium Pass")));
    // cfg.setDefaultValue("Passwort");
    // config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
    // getProperties(), PROPERTY_USE_PREMIUM,
    // JDLocale.L("plugins.hoster.rapidshare.de.usePremium", "Premium Account
    // verwenden")));
    // cfg.setDefaultValue(false);
    //
    // }

    @Override
    public void reset() {

    }

    @Override
    public void resetPluginGlobals() {

    }
}
