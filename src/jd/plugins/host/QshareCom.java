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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class QshareCom extends PluginForHost {
    // static private final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getMatch(0).*= "0.1";
    // static private final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getMatch(0);
    static private final String CODER = "JD-Team";

    static private final String HOST = "qshare.com";
    // http://s1.qshare.com/get/246129/Verknuepfung_mit_JDownloader.exe.lnk.html
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?qshare\\.com\\/get\\/[0-9]{1,20}\\/.*", Pattern.CASE_INSENSITIVE);

    public QshareCom() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
        // setConfigElements();
        this.enablePremium();

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        Browser br = new Browser();
        Browser.clearCookies(HOST);
        br.setAcceptLanguage("en, en-gb;q=0.8");

        br.getPage("http://www.qshare.com");
        br.getPage("http://www.qshare.com/index.php?sysm=user_portal&sysf=login");
        br.setFollowRedirects(false);
        // passt invalid html code an. es fehlt der form-close tag
        if (br.getRequest().getHtmlCode().toLowerCase().contains("<form") && !br.getRequest().getHtmlCode().toLowerCase().contains("</form")) {
            br.getRequest().setHtmlCode(br.getRequest().getHtmlCode() + "</form>");
        }

        Form[] forms = br.getForms();
        Form login = forms[0];
        login.put("username", account.getUser());
        login.put("password", account.getPass());
        login.put("cookie", "1");
        br.submitForm(login);

        String premiumError = br.getRegex("Following error occured: (.*?)[\\.|<]").getMatch(0);
        if (premiumError != null) {
            ai.setValid(false);
            ai.setStatus(premiumError);
            return ai;
        }
        br.getPage("http://qshare.com/index.php?sysm=user_adm&sysf=details");

        String expire = br.getRegex("[Ablauf der Flatrate am|Flatrate expires on]: <SPAN STYLE=.*?>(.*?)</SPAN>").getMatch(0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss", Locale.UK);
        if (expire == null) {
            ai.setStatus("Account expired or logins not valid");
            ai.setValid(false);
            return ai;
        }
        ai.setStatus("Account is ok");
        // 2009-07-19 17:50:29
        logger.info(dateFormat.format(new Date()) + "");
        Date date;
        try {
            date = dateFormat.parse(expire);
            ai.setValidUntil(date.getTime());
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ai;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        String page = null;

        Browser.clearCookies(HOST);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        // String[][] dat = new Regex(page, "<SPAN
        // STYLE=\"font-size\\:13px\\;vertical\\-align\\:middle\">.*<\\!\\-\\-
        // google_ad_section_start \\-\\->(.*?)<\\!\\-\\- google_ad_section_end
        // \\-\\->(.*?)<\\/SPAN>").getMatches();
        //
        // if (dat == null || dat.length == 0) {
        //
        // // step.setStatus(PluginStep.STATUS_ERROR);
        //
        // linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
        // return;
        // }

        if (br.getRedirectLocation() != null) br.getPage((String) null);

        String error = br.getRegex("<SPAN STYLE=\"font\\-size:13px;color:#BB0000;font\\-weight:bold\">(.*?)</SPAN>").getMatch(0);
        if (error != null) {
            linkStatus.setErrorMessage(error);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            return;
        }
        Form[] forms = br.getForms();
        page = br.submitForm(forms[0]);

        if (br.getRegex("Du hast die maximal zulässige Anzahl").matches()) {
            logger.severe("There is already a download running with our ip");
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }

        String wait = new Regex(page, "Dein Frei-Traffic wird in ([\\d]*?) Minuten wieder").getMatch(0);
        if (wait != null) {
            long waitTime = Long.parseLong(wait) * 60 * 1000;
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.setValue(waitTime);
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            return;
        }
        String link = new Regex(page, "<div id=\"download_link\"><a href=\"(.*?)\"").getMatch(0);

        if (link == null) {

            // step.setStatus(PluginStep.STATUS_ERROR);

            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
            return;
        }
        HTTPConnection con = br.openGetConnection(link);
        if (Plugin.getFileNameFormHeader(con) == null || Plugin.getFileNameFormHeader(con).indexOf("?") >= 0) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);

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

        Browser.clearCookies(HOST);
        br.getPage("http://www.qshare.com");
        br.getPage("http://www.qshare.com/index.php?sysm=user_portal&sysf=login");
        br.setFollowRedirects(false);
        // passt invalid html code an. es fehlt der form-close tag
        if (br.getRequest().getHtmlCode().toLowerCase().contains("<form") && !br.getRequest().getHtmlCode().toLowerCase().contains("</form")) {
            br.getRequest().setHtmlCode(br.getRequest().getHtmlCode() + "</form>");
        }

        Form[] forms = br.getForms();
        Form login = forms[0];
        login.put("username", user);
        login.put("password", pass);
        login.put("cookie", "1");
        br.submitForm(login);

        String premiumError = br.getRegex("[Following error occured|Folgender Fehler ist aufgetreten]: (.*?)[\\.|<]").getMatch(0);
        if (premiumError != null) {
            linkStatus.setErrorMessage(Encoding.htmlDecode(premiumError));
            linkStatus.addStatus(LinkStatus.ERROR_PREMIUM);
            linkStatus.setValue(LinkStatus.VALUE_ID_PREMIUM_DISABLE);
            return;
        }
        br.getPage(downloadLink.getDownloadURL());
        String url = br.getRegex("A HREF=\"(.*?)\">").getMatch(0);
        br.openGetConnection(url);

        if (br.getRedirectLocation() != null) {
            logger.info("Direct Download is activ");
            br.openGetConnection(null);
        } else {
            logger.warning("InDirect Download is activ (is much slower... you should active direct downloading in the configs(qshare configs)");
            br.loadConnection(null);
            String error = br.getRegex("<SPAN STYLE=\"font\\-size:13px;color:#BB0000;font\\-weight:bold\">(.*?)</SPAN>").getMatch(0);
            if (error != null) {
                linkStatus.setErrorMessage(error);
                linkStatus.addStatus(LinkStatus.ERROR_FATAL);
                return;
            }
            forms = br.getForms();
            Form premium = forms[forms.length - 1];

            Browser.clearCookies(HOST);
            br.setFollowRedirects(false);
            br.submitForm(premium);
            br.getPage((String) null);
            if (br.getRequest().getHtmlCode().toLowerCase().contains("<form") && !br.getRequest().getHtmlCode().toLowerCase().contains("</form")) {
                br.getRequest().setHtmlCode(br.getRequest().getHtmlCode() + "</form>");
            }
            forms = br.getForms();
            login = forms[forms.length - 1];
            login.put("username", user);
            login.put("password", pass);
            login.put("cookie", "1");
            br.submitForm(login);
            br.getPage((String) null);
            url = br.getRegex("(http://\\w{1,5}.qshare.com/\\w{1,10}/\\w{1,50}/\\w{1,50}/\\w{1,50}/\\w{1,50}/"+account.getUser()+"/"+account.getPass()+"/.*?)\"").getMatch(0);
            br.setFollowRedirects(true);
            br.openGetConnection(url);

        }

        dl = new RAFDownload(this, downloadLink, br.getHttpConnection());
        dl.setResume(true);
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));

        dl.startDownload();
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

        try {
            String page;
            // dateiname, dateihash, dateisize, dateidownloads, zeit bis
            // happyhour
            Browser br = new Browser();
            br.setFollowRedirects(true);
            page = br.getPage(downloadLink.getDownloadURL());
            String[][] dat = new Regex(page, "<SPAN STYLE=\"font-size\\:13px\\;vertical\\-align\\:middle\">.*<\\!\\-\\- google_ad_section_start \\-\\->(.*?)<\\!\\-\\- google_ad_section_end \\-\\->(.*?)<\\/SPAN>").getMatches();

            downloadLink.setName(dat[0][0].trim());
            downloadLink.setDownloadSize((int) Regex.getSize(dat[0][1].trim()));
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {

        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    /*
     * public int getMaxSimultanDownloadNum() { // if //
     * (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, //
     * true) && // this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM,
     * false)) // { // return 20; // } else { return 1; // } }
     * 
     * @Override
     */public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
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
