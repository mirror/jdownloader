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
import java.net.URL;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Filer extends PluginForHost {

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?filer.net/(file[\\d]+|get)/.*", Pattern.CASE_INSENSITIVE);
    // static private final Pattern GETID =
    // Pattern.compile("http://[\\w\\.]*?filer.net/file([\\d]+)/.*?",
    // Pattern.CASE_INSENSITIVE);
    static private final String HOST = "filer.net";
    static private final String PLUGIN_NAME = HOST;
    //static private final String new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch().*= "0.5";
    //static private final String PLUGIN_ID =PLUGIN_NAME + "-" + new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    static private final String CODER = "JD-Team";
    // static private final String FILE_NOT_FOUND = "Konnte Download nicht
    // finden";
    // static private final String FILE_NOT_FOUND2 = "Oops! We couldn't find
    // this page for you.";
    // static private final String FREE_USER_LIMIT = "Momentan sind die Limits
    // f&uuml;r Free-Downloads erreicht";
    // static private final String CAPTCHAADRESS =
    // "http://www.filer.net/captcha.png";
    // <form method="post" action="/get/e14eaf55d686a2b.html"><p class="center">
    // static private final Pattern DOWNLOAD = Pattern.compile("<form
    // method=\"post\" action=\"(\\/dl\\/.*?)\">", Pattern.CASE_INSENSITIVE);
    // static private final Pattern WAITTIME = Pattern.compile("Bitte warten Sie
    // ([\\d]+)", Pattern.CASE_INSENSITIVE);
    // static private final Pattern INFO = Pattern.compile("(?s)<td><a
    // href=\"(\\/get\\/.*?.html)\">(.*?)</a></td>.*?<td>([0-9\\.]+) .*?</td>",
    // Pattern.CASE_INSENSITIVE);
    // static private final String WRONG_CAPTCHACODE = "<img
    // src=\"/captcha.png\"";
    private static final Pattern PATTERN_MATCHER_ERROR = Pattern.compile("errors", Pattern.CASE_INSENSITIVE);

    // private String cookie;
    // private String dlink = null;
    // private String url;
    // private int waitTime = 500;

    public Filer() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
        setConfigElements();

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
        return new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    }

    
    
        
    

    public void handle(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        String user = this.getProperties().getStringProperty(PROPERTY_PREMIUM_USER);
        String pass = this.getProperties().getStringProperty(PROPERTY_PREMIUM_PASS);

        if (user != null && pass != null && this.getProperties().getBooleanProperty(PROPERTY_PREMIUM_USER, false) && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) {
            try {
                this.doPremium(downloadLink);
            } catch (Exception e) {

                e.printStackTrace();
            }
        } else {
            try {
                this.doFree(downloadLink);
            } catch (Exception e) {

                e.printStackTrace();
            }
        }
        return;
    }

    public void doPremium(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        String user = this.getProperties().getStringProperty(PROPERTY_PREMIUM_USER);
        String pass = this.getProperties().getStringProperty(PROPERTY_PREMIUM_PASS);

        String page = null;
        Browser br = new Browser();
        br.postPage("http://www.filer.net/login", "username=" + JDUtilities.urlEncode(user) + "&password=" + JDUtilities.urlEncode(pass) + "&commit=Einloggen");
        page = br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(false);
        String id = new Regex(page, "<a href=\"\\/dl\\/(.*?)\">.*?<\\/a>").getFirstMatch();
        br.getPage("http://www.filer.net/dl/" + id);

        HTTPConnection con = br.openGetConnection(br.getRedirectLocation());
        int length = con.getContentLength();
        downloadLink.setDownloadMax(length);
        logger.info("Filename: " + getFileNameFormHeader(con));
        if (getFileNameFormHeader(con) == null || getFileNameFormHeader(con).indexOf("?") >= 0) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setParameter(20000l);
            return;
        }
        downloadLink.setName(getFileNameFormHeader(con));

        dl = new RAFDownload(this, downloadLink, con);
        dl.setResume(true);
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));

        dl.startDownload();

    }

    public void doFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        int maxCaptchaTries = 5;
        String code;
        String page = null;

        Browser br = new Browser();
        Browser.clearCookies("filer.net");
        br.getPage(downloadLink.getDownloadURL());
        int tries = 0;
        while (tries < maxCaptchaTries) {
            File captchaFile = getLocalCaptchaFile(this, ".png");
            JDUtilities.download(captchaFile, br.openGetConnection("http://www.filer.net/captcha.png"));
            code = Plugin.getCaptchaCode(captchaFile, this);
            page = br.postPage(downloadLink.getDownloadURL(), "captcha=" + code);
            tries++;
            if (!page.contains("captcha.png")) break;
        }
        if (page.contains("captcha.png")) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }

        if (Regex.matches(page, PATTERN_MATCHER_ERROR)) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            String error = new Regex(page, "folgende Fehler und versuchen sie es erneut.*?<ul>.*?<li>(.*?)<\\/li>").getFirstMatch();
            logger.severe("Error: " + error);
            // step.setParameter(JDLocale.L("plugin.host.filernet.error." +
            // JDUtilities.getMD5(error), error));
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;

        }

        br.setFollowRedirects(false);
        String wait = new Regex(br, "Bitte warten Sie ([\\d]*?) Min bis zum").getFirstMatch();
        if (wait != null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            // step.setParameter(Long.parseLong(wait)*60*1000);
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            return;

        }
        Form[] forms = br.getForms();
        if (forms.length < 2) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            // step.setParameter(Long.parseLong("1")*60*1000);
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            return;

        }
        page = br.submitForm(forms[1]);
        //        
        // if (requestInfo.containsHTML(FREE_USER_LIMIT)) {
        // logger.severe("Free User Limit reached");
        // linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_SPECIFIC);
        // //step.setParameter("Free User Limit");
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // return;
        // }
        // if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND)) {
        // logger.severe("Die Datei existiert nicht");
        // linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // return;
        // }
        //
        // String strWaitTime =
        // SimpleMatches.getFirstMatch(requestInfo.getHtmlCode(), WAITTIME,
        // 1);
        // if (strWaitTime != null) {
        // logger.severe("wait " + strWaitTime + " minutes");
        // waitTime = Integer.parseInt(strWaitTime) * 60 * 1000;
        // linkStatus.addStatus(LinkStatus.ERROR_TRAFFIC_LIMIT);
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // logger.info(" WARTEZEIT SETZEN IN " + step + " : " + waitTime);
        // //step.setParameter((long) waitTime);
        // return;
        // }

        sleep(61000, downloadLink);

        HTTPConnection con = br.openGetConnection(br.getRedirectLocation());
        int length = con.getContentLength();
        downloadLink.setDownloadMax(length);
        logger.info("Filename: " + getFileNameFormHeader(con));
        if (getFileNameFormHeader(con) == null || getFileNameFormHeader(con).indexOf("?") >= 0) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setParameter(20000l);
            return;
        }
        downloadLink.setName(getFileNameFormHeader(con));

        dl = new RAFDownload(this, downloadLink, con);
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
        String page;
        File captchaFile;
        String code;
        int bytes;
        int maxCaptchaTries = 5;
        int tries = 0;
        while (maxCaptchaTries > tries) {
            try {

                Browser br = new Browser();
                br.getPage(downloadLink.getDownloadURL());
                captchaFile = getLocalCaptchaFile(this, ".png");
                JDUtilities.download(captchaFile, br.openGetConnection("http://www.filer.net/captcha.png"));
                code = Plugin.getCaptchaCode(captchaFile, this);
                page = br.postPage(downloadLink.getDownloadURL(), "captcha=" + code);
                if (Regex.matches(page, PATTERN_MATCHER_ERROR)) return false;
                bytes = (int) Regex.getSize(new Regex(page, "<tr class=\"even\">.*?<th>DateigrÃ¶ÃŸe</th>.*?<td>(.*?)</td>").getFirstMatch());
                downloadLink.setDownloadMax(bytes);
                br.setFollowRedirects(false);
                Form[] forms = br.getForms();
                if (forms.length < 2) return true;
                br.submitForm(forms[1]);
                downloadLink.setName(getFileNameFormURL(new URL(br.getRedirectLocation())));
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            tries++;
        }
        return false;
    }

    
    public int getMaxSimultanDownloadNum() {
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true) && this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)) {
            return 20;
        } else {
            return 1;
        }
    }

    private void setConfigElements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_USER, JDLocale.L("plugins.hoster.rapidshare.de.premiumUser", "Premium User")));
        cfg.setDefaultValue("Kundennummer");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getProperties(), PROPERTY_PREMIUM_PASS, JDLocale.L("plugins.hoster.rapidshare.de.premiumPass", "Premium Pass")));
        cfg.setDefaultValue("Passwort");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PREMIUM, JDLocale.L("plugins.hoster.rapidshare.de.usePremium", "Premium Account verwenden")));
        cfg.setDefaultValue(false);

    }

    
    public void resetPluginGlobals() {
       

    }

    
    public String getAGBLink() {
       
        return "http://www.filer.net/faq";
    }
}
