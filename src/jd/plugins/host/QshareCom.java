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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

public class QshareCom extends PluginForHost {
    public QshareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://s1.qshare.com/index.php?sysm=sys_page&sysf=site&site=buy");
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        Browser br = new Browser();
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
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
            if (br.containsHTML("Flatrate")) {
                ai.setStatus("Account expired");
            } else {
                ai.setStatus("Logins not valid");
            }

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
            e.printStackTrace();
        }
        return ai;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String error = br.getRegex("<SPAN STYLE=\"font\\-size:13px;color:#BB0000;font\\-weight:bold\">(.*?)</SPAN>").getMatch(0);
        if (error != null) throw new PluginException(LinkStatus.ERROR_FATAL, Encoding.UTF8Encode(error));

        String url = br.getRegex(Pattern.compile("<SCRIPT TYPE=\"text/javascript\">.*?function free\\(\\).*?window.location = \"(.*?)\";", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.getPage(url);

        if (br.getRegex("Du hast die maximal zulässige Anzahl").matches()) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);

        String wait = br.getRegex("Dein Frei-Traffic wird in ([\\d]*?) Minuten wieder").getMatch(0);

        if (wait != null && !downloadLink.getBooleanProperty("trywithoutwait", true)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait.trim()) * 60 * 1000l);

        String link = br.getRegex("<DIV ID=\"download_link\"><A HREF=\"(.*?)\"").getMatch(0);

        if (link == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);

        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, link, true, 1);
        if (!dl.getConnection().isContentDisposition() && wait != null) {
            dl.getConnection().disconnect();
            downloadLink.setProperty("trywithoutwait", false);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (dl.startDownload()) {
            downloadLink.setProperty("trywithoutwait", true);
        }

    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        String user = account.getUser();
        String pass = account.getPass();
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
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
            dl = br.openDownload(downloadLink, (String) null, true, 0);
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

            br.setCookiesExclusive(true);
            br.clearCookies(getHost());
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
            url = br.getRegex("(http://\\w{1,5}.qshare.com/\\w{1,10}/\\w{1,50}/\\w{1,50}/\\w{1,50}/\\w{1,50}/" + account.getUser() + "/" + account.getPass() + "/.*?)\"").getMatch(0);
            br.setFollowRedirects(true);
            dl = br.openDownload(downloadLink, url, true, 0);

        }

        dl.startDownload();

    }

    @Override
    public String getAGBLink() {
        return "http://s1.qshare.com/index.php?sysm=sys_page&sysf=site&site=terms";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String[] dat = br.getRegex("<SPAN STYLE=\"font-size:13px;vertical-align:middle\">(.*?) \\((.*?)\\).*?</SPAN>").getRow(0);
        if (dat.length != 2) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(dat[0].trim());
        downloadLink.setDownloadSize((int) Regex.getSize(dat[1].trim()));
        return true;
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
