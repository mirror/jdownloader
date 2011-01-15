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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "badongo.com" }, urls = { "http://[\\w\\.]*?badongo\\.viajd.*/.*(audio|file|vid)/[0-9]+/?\\d?/?\\w?\\w?" }, flags = { PluginWrapper.LOAD_ON_INIT })
public class BadongoCom extends PluginForHost {

    private static final String FILETEMPLATE = "http://www.badongo.com/ajax/prototype/ajax_api_filetemplate.php";

    private static final String JAVASCRIPT   = "eval(.*?)\n|\r|\rb";

    public BadongoCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.badongo.com/compare");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.viajd", ".com"));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            this.login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        try {
            this.isPremium();
        } catch (final PluginException e) {
            ai.setStatus("Not Premium Membership");
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Account ok");
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.badongo.com/toc/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private void handleErrors(final Browser br) throws PluginException {
        if (br.containsHTML("Gratis Mitglied Wartezeit")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 1000l); }
        if (br.containsHTML("Du hast Deine Download Quote überschritten")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l); }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        final String realURL = downloadLink.getDownloadURL().replaceAll("\\.viajd", ".com");
        this.requestFileInformation(downloadLink);
        this.br.setCookiesExclusive(true);
        this.br.setFollowRedirects(false);
        this.br.setCustomCharset("utf-8");
        this.br.setDebug(true);
        if (!realURL.contains("/audio/")) {
            /* Get CaptchaCode */
            this.br.getPage(realURL + "?rs=refreshImage&rst=&rsrnd=" + System.currentTimeMillis());
            final String cid = this.br.getRegex("cid=(\\d+)").getMatch(0);
            final String fileID = new Regex(realURL, "(file|vid)/(\\d+)").getMatch(1);
            final String filetype = new Regex(realURL, "(file|vid)/(\\d+)").getMatch(0);
            String filepart = new Regex(realURL, "/([a-z]+)$").getMatch(0);
            final String capSecret = this.br.getRegex("cap_secret value=(.*?)>").getMatch(0);
            final String action = this.br.getRegex("action=\\\\\"(.*?)\\\\\"").getMatch(0);
            if ((cid == null) || (fileID == null) || (capSecret == null) || (action == null)) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            final String code = this.getCaptchaCode("http://www.badongo.com/ccaptcha.php?cid=" + cid, downloadLink);
            String postData = "user_code=" + code + "&cap_id=" + cid + "&cap_secret=" + capSecret;
            this.br.getHeaders().put("Referer", realURL);
            this.br.postPage(action, postData);
            if (this.br.getRequest().getHttpConnection().getResponseCode() != 200) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
            if (filepart == null) {
                filepart = "";
            }
            /* packed JS in ArrayList */
            final ArrayList<String> packedJS = new ArrayList<String>();
            packedJS.add(this.br.getRegex(BadongoCom.JAVASCRIPT).getMatch(0, 2));
            packedJS.add(this.br.getRegex(BadongoCom.JAVASCRIPT).getMatch(0, 7));
            if (packedJS.get(1) == null) {
                packedJS.set(1, "{:'#':'#':'#':'" + this.br.getRegex("dlUrl \\+ \"(.*?)\"").getMatch(0, 1) + "'");
            }
            String[] plainJS = this.unpackJS(packedJS.get(0));
            if (plainJS == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            /* DOWNLOAD:INIT */
            postData = "id=" + fileID + "&type=" + filetype + "&ext=" + filepart + "&f=download:init&z=" + plainJS[1] + "&h=" + plainJS[2];
            this.br.getHeaders().put("Referer", action);
            this.br.postPage(BadongoCom.FILETEMPLATE, postData);
            /* DOWNLOAD:CHECK#1 */
            plainJS = this.unpackJS(this.br.getRegex(BadongoCom.JAVASCRIPT).getMatch(0));
            if (plainJS == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            postData = "id=" + plainJS[4] + "&type=" + plainJS[5] + "&ext=" + plainJS[6] + "&f=download:check" + "&z=" + plainJS[1] + "&h=" + plainJS[2] + "&t=" + plainJS[3];
            /* Timer */
            int waitThis = 59;
            String wait = plainJS[7].toString().replaceAll("\\D", "");
            if (wait != null) {
                waitThis = Integer.parseInt(wait);
            }
            this.sleep(waitThis * 1001l, downloadLink);
            /* DOWNLOAD:CHECK#2 + additional wait time */
            do {
                this.br.postPage(BadongoCom.FILETEMPLATE, postData);
                plainJS = this.unpackJS(this.br.getRegex(BadongoCom.JAVASCRIPT).getMatch(0));
                if ((plainJS[0] != null) && plainJS[0].contains("ck_[0-9a-f]+")) {
                    wait = plainJS[0].replaceAll("\\D", "");
                    if (wait != null) {
                        waitThis = Integer.parseInt(wait);
                    }
                    this.sleep(waitThis * 1001l, downloadLink, "Waiting for host: ");
                }
            } while (plainJS[0] != null);
            /* File or Video Link */
            String fileOrVid = "";
            if (realURL.contains("file/")) {
                fileOrVid = "getFileLink";
            } else {
                fileOrVid = "getVidLink";
            }
            /* Next Request. Response part of final link */
            final String pathData = "?rs=" + fileOrVid + "&rst=&rsrnd=" + System.currentTimeMillis() + "&rsargs[]=0&rsargs[]=yellow&rsargs[]=" + plainJS[1] + "&rsargs[]=" + plainJS[2] + "&rsargs[]=" + plainJS[3] + "&rsargs[]=" + plainJS[5] + "&rsargs[]=" + plainJS[4] + "&rsargs[]=" + filepart;
            this.br.getPage(action + pathData).trim();
            plainJS = this.unpackJS(packedJS.get(1));
            String link = null;
            final String returnVar = this.br.getRegex("return (.*?)\\(").getMatch(0);
            final String[] alllink = this.br.getRegex(returnVar + "\\(.'(.*?).'\\)").getColumn(0);
            if (((returnVar != null) && (alllink != null)) || (alllink.length > 0)) {
                for (final String tmplink : alllink) {
                    if (tmplink.contains("www.badongo.com")) {
                        link = tmplink;
                        break;
                    }
                }
            }
            if ((link == null) || (packedJS.get(1) == null)) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            /* Click button. Next Request. Response new packed JS */
            this.br.getPage(link + plainJS[4] + "?zenc=");
            this.handleErrors(this.br);
            packedJS.add(this.br.getRegex(BadongoCom.JAVASCRIPT).getMatch(0, 4));
            if (packedJS.get(2) == null) {
                packedJS.set(2, "{:'" + this.br.getRegex("window\\.location\\.href = '(.*?)\\?pr=").getMatch(0) + "?pr='");
            }
            plainJS = this.unpackJS(packedJS.get(2));
            /* Test */
            if (plainJS[1] == null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 1000l); }
            /* Next Request. Response final link as redirect */
            this.br.getPage("http://www.badongo.com" + plainJS[1]);
            this.dl = BrowserAdapter.openDownload(this.br, downloadLink, this.br.getRedirectLocation(), true, 1);
        } else {
            // this.br.getPage(realURL);
            String dllink = this.br.getRegex("songFileSrc\",\"(.*?)\"\\)").getMatch(0);
            final String filename = this.br.getRegex("songFileName\",\"(.*?)\"\\)").getMatch(0);
            if ((dllink == null) || (filename == null)) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            if (!filename.contains("Unknown")) {
                downloadLink.setName(filename);
            }
            dllink = Encoding.urlDecode(dllink, true);
            this.dl = BrowserAdapter.openDownload(this.br, downloadLink, dllink, true, 1);
        }
        if (!this.dl.getConnection().isContentDisposition()) {
            final String page = this.br.loadConnection(this.dl.getConnection()) + "";
            this.br.getRequest().setHtmlCode(page);
            this.dl.getConnection().disconnect();
            this.handleErrors(this.br);
        }
        this.dl.startDownload();
    }

    @Override
    // TODO: Fix & Test Premium
    public void handlePremium(final DownloadLink parameter, final Account account) throws Exception {
        this.requestFileInformation(parameter);
        this.login(account);
        this.isPremium();
        String link = null;
        this.br.getPage(parameter.getDownloadURL());
        this.sleep(5000l, parameter);
        if (parameter.getStringProperty("type", "single").equalsIgnoreCase("split")) {
            final String downloadLinks[] = this.br.getRegex("doDownload\\(.?'(.*?).?'\\)").getColumn(0);
            final int part = parameter.getIntegerProperty("part", 1);
            link = downloadLinks[part - 1];
            link = link + (char) (part + 96);
            this.br.getPage(link + "/ifr?pr=1&zenc=");
            link = link + "/loc?pr=1";
        } else {
            link = this.br.getRegex("onclick=\"return doDownload\\('(.*?)'\\)").getMatch(0);
            link = link.replaceFirst("/1$", "/0");
            this.br.getPage(link + "/ifr?zenc=");
        }
        if (link == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, parameter, link, true, 0);
        if (!this.dl.getConnection().isContentDisposition()) {

            final String page = this.br.loadConnection(this.dl.getConnection()) + "";
            this.br.getRequest().setHtmlCode(page);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    public boolean isPremium() throws PluginException, IOException {
        this.br.getPage("http://www.badongo.com/de/");
        final String type = this.br.getRegex("Du bist zur Zeit als <b>(.*?)</b> eingeloggt").getMatch(0);
        if (type == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        if (new Regex(type, Pattern.compile("premium", Pattern.CASE_INSENSITIVE)).matches()) { return true; }
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    public void login(final Account account) throws Exception {
        this.setBrowserExclusive();
        this.br.setCookie("http://www.badongo.com", "badongoL", "de");
        // br.getPage("http://www.badongo.com");
        this.br.getPage("http://www.badongo.com/de/login");
        final Form form = this.br.getForm(0);
        form.put("username", Encoding.urlEncode(account.getUser()));
        form.put("password", Encoding.urlEncode(account.getPass()));
        this.br.submitForm(form);
        if ((this.br.getCookie("http://www.badongo.com", "badongo_user") == null) || (this.br.getCookie("http://www.badongo.com", "badongo_password") == null)) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        this.br.setCookiesExclusive(true);
        this.br.setCookie("http://www.badongo.com", "badongoL", "de");
        this.br.getPage(downloadLink.getDownloadURL());
        /* File Password */
        if (this.br.containsHTML("Diese Datei ist zur Zeit : <b>Geschützt</b>")) {
            for (int i = 0; i <= 5; i++) {
                final Form pwForm = this.br.getFormbyProperty("name", "pwdForm");
                if (pwForm == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                String pass = downloadLink.getDecrypterPassword();
                if (pass == null) {
                    pass = Plugin.getUserInput(null, downloadLink);
                    if (pass == null) {
                        continue;
                    }
                }
                pwForm.put("pwd", pass);
                this.br.submitForm(pwForm);
                if (!this.br.containsHTML("Falsches Passwort!")) {
                    downloadLink.setDecrypterPassword(pass);
                    break;
                }
            }
            if (downloadLink.getDecrypterPassword() == null) {
                this.logger.severe(JDL.L("plugins.errors.wrongpassword", "Password wrong"));
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        final String filesize = this.br.getRegex(Pattern.compile("<div class=\"ffileinfo\">Ansichten.*?\\| Dateig.*?:(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        final String filename = this.br.getRegex("<div class=\"finfo\">(.*?)</div>").getMatch(0);
        if ((filesize == null) || (filename == null)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (downloadLink.getStringProperty("type", "single").equalsIgnoreCase("single")) {
            downloadLink.setName(filename.trim());
            downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        } else {
            final String parts = Formatter.fillString(downloadLink.getIntegerProperty("part", 1) + "", "0", "", 3);
            downloadLink.setName(filename.trim() + "." + parts);
            if (downloadLink.getIntegerProperty("part", 1) == downloadLink.getIntegerProperty("parts", 1)) {
                final long bytes = Regex.getSize(filesize);
                downloadLink.setDownloadSize(bytes - (downloadLink.getIntegerProperty("parts", 1) - 1) * 102400000l);
            } else {
                downloadLink.setDownloadSize(102400000);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    public String[] unpackJS(String fun) throws Exception {
        Object result = new Object();
        if (fun == null) {
            fun = "nodata";
        }
        if (fun.contains("%[0-9A-F]{2}")) {
            final ScriptEngineManager manager = new ScriptEngineManager();
            final ScriptEngine engine = manager.getEngineByName("javascript");
            try {
                result = engine.eval(fun);
            } catch (final ScriptException e) {
                e.printStackTrace();
            }
        } else {
            result = null;
            if (!this.br.containsHTML("'ext': ''")) {
                final String[] InitOpt = this.br.getRegex("getFileLinkInitOpt(\\.z)? = (.*?)\n|\r\rb").getColumn(1);
                if (InitOpt.length > 0) {
                    final String z = new Regex(InitOpt[InitOpt.length - 1], "'(.*?)'").getMatch(-1);
                    result = InitOpt[InitOpt.length - 2].replaceAll("z':(.*),'h", "z':" + z + ",'h");
                }
            }
            if ((result == null) && this.br.containsHTML("getFileLinkInitOpt")) {
                result = this.br.toString();
            }
            if ((result == null) && this.br.containsHTML("ck_[0-9a-f]+")) {
                result = this.br.toString();
            }
            if (result == null) {
                result = "nodata";
            }
            if (!fun.contains("data")) {
                result = fun;
            }
        }
        final String[] row = new String[10];
        int z = 0;
        String unpacked = result.toString();
        unpacked = unpacked.replaceAll("\n|\r|\rb|\t| ", "");
        if ((unpacked.length() <= 20) && unpacked.contains("check_n")) {
            row[0] = unpacked;
            return row;
        }
        final int a = unpacked.lastIndexOf("{");
        unpacked = unpacked.substring(a + 1, unpacked.length());
        final Pattern pattern = Pattern.compile("(:|=)'(.*?)'|'check_n'.*?\"(\\d+)\"|\".(.*?)\"");
        final Matcher matcher = pattern.matcher(unpacked);
        while (matcher.find()) {
            z += 1;
            row[z] = matcher.group().toString().replaceAll("'|:|=|\"", "").trim();
        }
        return row;
    }

}
