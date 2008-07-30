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
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class QshareCom extends PluginForHost {
    // http://s1.qshare.com/get/246129/Verknuepfung_mit_JDownloader.exe.lnk.html
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?qshare\\.com\\/get\\/[0-9]{1,20}\\/.*", Pattern.CASE_INSENSITIVE);

    static private final String HOST = "qshare.com";
    static private final String PLUGIN_NAME = HOST;
    //static private final String new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch().*= "0.1";
    //static private final String PLUGIN_ID =PLUGIN_NAME + "-" + new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    static private final String CODER = "JD-Team";

    public QshareCom() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
        // setConfigElements();

    }

    
    public String getCoder() {
        return CODER;
    }

    
    public String getPluginName() {
        return HOST;
    }

    
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    
    public String getHost() {
        return HOST;
    }

    
    public String getVersion() {
       String ret=new Regex("$Revision$","\\$Revision: ([\\d]*?) \\$").getFirstMatch();return ret==null?"0.0":ret;
    }

    
    
        
   

    public void handle(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        String user = this.getProperties().getStringProperty(PROPERTY_PREMIUM_USER);
        String pass = this.getProperties().getStringProperty(PROPERTY_PREMIUM_PASS);

        if (user != null && pass != null && this.getProperties().getBooleanProperty(PROPERTY_PREMIUM_USER, false)) {

            this.doPremium(downloadLink);

        } else {

            this.doFree(downloadLink);

        }
        return;
    }

    public void doPremium(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        return;

    }

    public void doFree(DownloadLink downloadLink) throws Exception {
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
        int length = con.getContentLength();
        downloadLink.setDownloadMax(length);
        logger.info("Filename: " + getFileNameFormHeader(con));
        if (getFileNameFormHeader(con) == null || getFileNameFormHeader(con).indexOf("?") >= 0) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            this.sleep(20000, downloadLink);
            return;
        }
        downloadLink.setName(getFileNameFormHeader(con));

        dl = new RAFDownload(this, downloadLink, con);
        dl.setResume(false);
        dl.setChunkNum(1);

        dl.startDownload();

    }

    
    public boolean doBotCheck(File file) {
        return false;
    }

    
    public void reset() {

    }

    public String getFileInformationString(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }

    
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

    
    public void resetPluginGlobals() {
       

    }

    
    public String getAGBLink() {
       
        return "http://s1.qshare.com/index.php?sysm=sys_page&sysf=site&site=terms";
    }
}
