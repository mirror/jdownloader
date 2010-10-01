//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "badongo.com" }, urls = { "http://[\\w\\.]*?badongo\\.viajd.*/.*(file|vid)/[0-9]+/?\\d?/?\\w?\\w?" }, flags = { PluginWrapper.LOAD_ON_INIT })
public class BadongoCom extends PluginForHost {

    public BadongoCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.badongo.com/compare");
    }

    @Override
    public String getAGBLink() {
        return "http://www.badongo.com/toc/";
    }
    
    private static final String FILETEMPLATE = "http://www.badongo.com/ajax/prototype/ajax_api_filetemplate.php";
    private static final String JAVASCRIPT = "eval(.*?)\n|\r|\rb";

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.viajd", ".com"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setCookiesExclusive(true);
        br.setCookie("http://www.badongo.com", "badongoL", "de");
        br.getPage(downloadLink.getDownloadURL());
        /* File Password */
        if (br.containsHTML("Diese Datei ist zur Zeit : <b>Geschützt</b>")) {
            for (int i = 0; i <= 5; i++) {
                Form pwForm = br.getFormbyProperty("name", "pwdForm");
                if (pwForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                String pass = downloadLink.getDecrypterPassword();
                if (pass == null) {
                    pass = getUserInput(null, downloadLink);
                    if (pass == null) continue;
                }
                pwForm.put("pwd", pass);
                br.submitForm(pwForm);
                if (!br.containsHTML("Falsches Passwort!")) {
                    downloadLink.setDecrypterPassword(pass);
                    break;
                }
            }
            if (downloadLink.getDecrypterPassword() == null) {
                logger.severe(JDL.L("plugins.errors.wrongpassword", "Password wrong"));
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        String filesize = br.getRegex(Pattern.compile("<div class=\"ffileinfo\">Ansichten.*?\\| Dateig.*?:(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        String filename = br.getRegex("<div class=\"finfo\">(.*?)</div>").getMatch(0);

        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        if (downloadLink.getStringProperty("type", "single").equalsIgnoreCase("single")) {
            downloadLink.setName(filename.trim());
            downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        } else {
            String parts = Formatter.fillString(downloadLink.getIntegerProperty("part", 1) + "", "0", "", 3);
            downloadLink.setName(filename.trim() + "." + parts);
            if (downloadLink.getIntegerProperty("part", 1) == downloadLink.getIntegerProperty("parts", 1)) {
                long bytes = Regex.getSize(filesize);
                downloadLink.setDownloadSize(bytes - (downloadLink.getIntegerProperty("parts", 1) - 1) * 102400000l);
            } else {
                downloadLink.setDownloadSize(102400000);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    // TODO: Fix & Test Premium
    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        isPremium();
        String link = null;
        br.getPage(parameter.getDownloadURL());
        sleep(5000l, parameter);
        if (parameter.getStringProperty("type", "single").equalsIgnoreCase("split")) {
            String downloadLinks[] = br.getRegex("doDownload\\(.?'(.*?).?'\\)").getColumn(0);
            int part = parameter.getIntegerProperty("part", 1);
            link = downloadLinks[part - 1];
            link = link + (char) (part + 96);
            br.getPage(link + "/ifr?pr=1&zenc=");
            link = link + "/loc?pr=1";
        } else {
            link = br.getRegex("onclick=\"return doDownload\\('(.*?)'\\)").getMatch(0);
            link = link.replaceFirst("/1$", "/0");
            br.getPage(link + "/ifr?zenc=");
        }
        if (link == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, link, true, 0);
        if (!dl.getConnection().isContentDisposition()) {

            String page = br.loadConnection(dl.getConnection()) + "";// +"" due
                                                                     // to
                                                                     // refaktor
                                                                     // compatibilities.
                                                                     // old
                                                                     // <ref10000
                                                                     // returns
                                                                     // String.
                                                                     // else
                                                                     // Request
                                                                     // INstance
            br.getRequest().setHtmlCode(page);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        String link = null;
        String realURL = downloadLink.getDownloadURL().replaceAll("\\.viajd", ".com");
        requestFileInformation(downloadLink);
        Browser ajax = br.cloneBrowser();
        ajax.setCookiesExclusive(true);
        ajax.setFollowRedirects(false);
        /* Get CaptchaCode */
        String action = null;
        String postData = null;
        String fileID = null;
        String filetype = null;
        String filepart = "";
        do {
            ajax.getPage(realURL + "?rs=refreshImage&rst=&rsrnd=" + System.currentTimeMillis());
            String cid = ajax.getRegex("cid=(\\d+)").getMatch(0);
            fileID = new Regex(realURL, "(file|vid)/(\\d+)").getMatch(1);
            filetype = new Regex(realURL, "(file|vid)/(\\d+)").getMatch(0);
            filepart = new Regex(realURL, "/([a-z]+)$").getMatch(0);
            String capSecret = ajax.getRegex("cap_secret value=(.*?)>").getMatch(0);
            action = ajax.getRegex("action=\\\\\"(.*?)\\\\\"").getMatch(0);
            if (cid == null || fileID == null || capSecret == null || action == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code = getCaptchaCode("http://www.badongo.com/ccaptcha.php?cid=" + cid, downloadLink);
            postData = "user_code=" + code + "&cap_id=" + cid + "&cap_secret=" + capSecret;
            ajax.setHeader("Referer", realURL);
            ajax.postPage(action, postData);
        } while (ajax.getRequest().getHttpConnection().getResponseCode() != 200);
        if ( filepart == null) filepart = "";
        ArrayList<String> packedJS = new ArrayList<String>();
        /* packed Javascript in ArrayList */
        packedJS.add(ajax.getRegex(JAVASCRIPT).getMatch(0, 2));
        packedJS.add(ajax.getRegex(JAVASCRIPT).getMatch(0, 7));
        /* String an unpackJS(String) schicken und geregextes Array zurück bekommen */
        String[] plainJS = unpackJS(packedJS.get(0));           
        /* DOWNLOAD:INIT */
        postData = "id=" + fileID + "&type=" + filetype + "&ext=" + filepart + "&f=download:init&z=" + plainJS[1] + "&h=" + plainJS[2];
        ajax.setHeader("Referer", action);       
        /* DOWNLOAD:CHECK#1 */
        ajax.postPage(FILETEMPLATE, postData);
        plainJS = unpackJS(ajax.getRegex(JAVASCRIPT).getMatch(0));            
        postData = "id=" + plainJS[4] + "&type=" + plainJS[5] + "&ext=" + plainJS[6] + "&f=download:check" + "&z=" + plainJS[1] + "&h=" + plainJS[2] + "&t=" + plainJS[3];
        /* Timer */
        int waitThis = 59;
        String wait = plainJS[7].toString().replaceAll("\\D", "");
        if (wait != null) waitThis = Integer.parseInt(wait);
        sleep(waitThis * 1001l, downloadLink);            
        /* DOWNLOAD:CHECK#2 + additional waittime */
        do {
            ajax.postPage(FILETEMPLATE, postData);
            plainJS = unpackJS(ajax.getRegex(JAVASCRIPT).getMatch(0));
            if (plainJS[0] != null && plainJS[0].contains("check_n")) {
                wait = plainJS[0].replaceAll("\\D", "");
                if (wait != null) waitThis = Integer.parseInt(wait);
                sleep(waitThis * 1001l, downloadLink, "Waiting for host: ");
              }
        } while (plainJS[0] != null);            
        /* File or Video Link */
        String fileOrVid = "";
        if (realURL.contains("file/"))
            fileOrVid = "getFileLink";
        else
            fileOrVid = "getVidLink";
        /* Next Request. Response part of dl-Link */
        String pathData = "?rs=" + fileOrVid + "&rst=&rsrnd=" + System.currentTimeMillis() + "&rsargs[]=0&rsargs[]=yellow&rsargs[]="
                          + plainJS[1] + "&rsargs[]=" + plainJS[2] + "&rsargs[]=" + plainJS[3] + "&rsargs[]=" + plainJS[5] + "&rsargs[]="
                          + plainJS[4] + "&rsargs[]=" + filepart;
        ajax.getPage(action + pathData).trim();
        plainJS = unpackJS(packedJS.get(1));
        link = ajax.getRegex("doDownload\\(.'(.*?).'\\)").getMatch(0);
        if (link == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /* Click Downloadbutton. Next Request. Response new packed JS */
        ajax.getPage(link + plainJS[4] + "?zenc=");
        handleErrors(ajax);
        packedJS.add(ajax.getRegex(JAVASCRIPT).getMatch(0, 4));
        plainJS = unpackJS(packedJS.get(2));
        /* Testfix */
        if (plainJS[1] == null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 1000l);
        /* Next Request. Response Finallink as redirect */
        ajax.getPage("http://www.badongo.com" + plainJS[1]); 
        dl = BrowserAdapter.openDownload(ajax, downloadLink, ajax.getRedirectLocation(), true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            String page = ajax.loadConnection(dl.getConnection()) + "";// +""
                                                                       // due
                                                                       // to
                                                                       // refaktor
                                                                       // compatibilities.
                                                                       // old
                                                                       // <ref10000
                                                                       // returns
                                                                       // String.
                                                                       // else
                                                                       // Request
                                                                       // INstance
            ajax.getRequest().setHtmlCode(page);
            dl.getConnection().disconnect();
            handleErrors(ajax);
        }
        dl.startDownload();
    }          

    private void handleErrors(Browser br) throws PluginException {
        if (br.containsHTML("Gratis Mitglied Wartezeit")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 1000l);
        if (br.containsHTML("Du hast Deine Download Quote überschritten")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
    }

    public boolean isPremium() throws PluginException, IOException {
        br.getPage("http://www.badongo.com/de/");
        String type = br.getRegex("Du bist zur Zeit als <b>(.*?)</b> eingeloggt").getMatch(0);
        if (type == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (new Regex(type, Pattern.compile("premium", Pattern.CASE_INSENSITIVE)).matches()) return true;
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    public void login(Account account) throws Exception {
        setBrowserExclusive();
        br.setCookie("http://www.badongo.com", "badongoL", "de");
        // br.getPage("http://www.badongo.com");
        br.getPage("http://www.badongo.com/de/login");
        Form form = br.getForm(0);
        form.put("username", Encoding.urlEncode(account.getUser()));
        form.put("password", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        if (br.getCookie("http://www.badongo.com", "badongo_user") == null || br.getCookie("http://www.badongo.com", "badongo_password") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        try {
            isPremium();
        } catch (PluginException e) {
            ai.setStatus("Not Premium Membership");
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Account ok");
        account.setValid(true);
        return ai;
    }
    
    public String[] unpackJS(String fun) throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
        Object result = new Object();
        try {
            result = engine.eval(fun);
        } catch(ScriptException e) {
            e.printStackTrace();
        }
        String[] row = new String[10];
        int z = 0;
        String unpacked = result.toString();
        unpacked = unpacked.replaceAll("\n|\t| ", "");
        if (unpacked.length() <= 20 && unpacked.contains("check_n")) {
            row[0] = unpacked;
            return row;
            }
        int a = unpacked.lastIndexOf("{" );
        unpacked = unpacked.substring(a + 1, unpacked.length());
        Pattern pattern = Pattern.compile("(:|=)'(.*?)'|'check_n'.*?\"(\\d+)\"|\".(.*?)\""); 
        Matcher matcher = pattern.matcher(unpacked); 
        while (matcher.find()){             
            z+=1;
            row[z] = matcher.group().toString().replaceAll("'|:|=|\"", "").trim();
        }
        return row;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
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
