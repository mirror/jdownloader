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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.StringFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "badongo.com" }, urls = { "http://[\\w\\.]*?badongo\\.viajd.*/.+" }, flags = { PluginWrapper.LOAD_ON_INIT })
public class BadongoCom extends PluginForHost {

    private static final String FILETEMPLATE = "/ajax/prototype/ajax_api_filetemplate.php";
    private static final String JAVASCRIPT   = "eval(.*?)\n|\r|\rb";
    private static final String MAINPAGE     = "http://www.badongo.com";

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
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        try {
            isPremium();
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
        requestFileInformation(downloadLink);
        br.setCookiesExclusive(true);
        br.setFollowRedirects(false);
        br.setCustomCharset("utf-8");
        if (realURL.contains("/file/") || realURL.contains("/vid/")) {
            /* Get CaptchaCode */
            br.getPage(realURL + "?rs=refreshImage&rst=&rsrnd=" + System.currentTimeMillis());
            final String cid = br.getRegex("cid=(\\d+)").getMatch(0);
            final String fileID = new Regex(realURL, "(file|vid)/(\\d+)").getMatch(1);
            final String filetype = new Regex(realURL, "(file|vid)/(\\d+)").getMatch(0);
            String filepart = new Regex(realURL, "/([a-z]+)$").getMatch(0);
            final String capSecret = br.getRegex("cap_secret value=(.*?)>").getMatch(0);
            final String action = br.getRegex("action=\\\\\"(.*?)\\\\\"").getMatch(0);
            if (cid == null || fileID == null || capSecret == null || action == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            final String code = this.getCaptchaCode(MAINPAGE + "/ccaptcha.php?cid=" + cid, downloadLink);
            String postData = "user_code=" + code + "&cap_id=" + cid + "&cap_secret=" + capSecret;
            br.getHeaders().put("Referer", realURL);
            br.postPage(action, postData);
            if (br.getRequest().getHttpConnection().getResponseCode() != 200) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
            if (filepart == null) {
                filepart = "";
            }
            /* packed JS in ArrayList */
            final ArrayList<String> packedJS = new ArrayList<String>();
            packedJS.add(br.getRegex(JAVASCRIPT).getMatch(0, 2));
            packedJS.add(br.getRegex(JAVASCRIPT).getMatch(0, 7));
            if (packedJS.get(1) == null) {
                packedJS.set(1, "{:'#':'#':'#':'" + br.getRegex("dlUrl \\+ \"(.*?)\"").getMatch(0, 1) + "'");
            }
            String[] plainJS = unpackJS(packedJS.get(0));
            if (plainJS == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            /* DOWNLOAD:INIT */
            postData = "id=" + fileID + "&type=" + filetype + "&ext=" + filepart + "&f=download:init&z=" + plainJS[1] + "&h=" + plainJS[2];
            br.getHeaders().put("Referer", action);
            br.postPage(MAINPAGE + FILETEMPLATE, postData);
            /* DOWNLOAD:CHECK#1 */
            plainJS = unpackJS(br.getRegex(JAVASCRIPT).getMatch(0));
            if (plainJS == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            postData = "id=" + plainJS[4] + "&type=" + plainJS[5] + "&ext=" + plainJS[6] + "&f=download:check" + "&z=" + plainJS[1] + "&h=" + plainJS[2] + "&t=" + plainJS[3];
            /* Timer */
            int waitThis = 59;
            String wait = plainJS[7].toString().replaceAll("\\D", "");
            if (wait != null) {
                waitThis = Integer.parseInt(wait);
            }
            this.sleep((waitThis - 10) * 1001l, downloadLink);
            /* DOWNLOAD:CHECK#2 + additional wait time */
            do {
                br.postPage(MAINPAGE + FILETEMPLATE, postData);
                plainJS = unpackJS(br.getRegex(JAVASCRIPT).getMatch(0));
                if (plainJS[0] != null && new Regex(plainJS[0], "window.[0-9a-f]+").matches()) {
                    wait = new Regex(plainJS[0], "=(\\d+)").getMatch(0);
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
            br.getPage(action + pathData).trim();
            plainJS = unpackJS(packedJS.get(1));
            String link = null;
            final String returnVar = br.getRegex("javascript:\\w+\\(\\\\'(.*?)\\\\'\\)").getMatch(0, 1);
            final String[] alllink = br.getRegex(returnVar + "\\(.'(.*?).'\\)").getColumn(0);
            if (returnVar != null && alllink != null && alllink.length > 0) {
                for (final String tmplink : alllink) {
                    if (tmplink.contains("www.badongo.com")) {
                        link = tmplink;
                        break;
                    }
                }
            }
            if (returnVar != null) {
                link = returnVar;
            }
            if (link == null || plainJS == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            /* Click button. Next Request. Response new packed JS */
            br.getPage(link + plainJS[4] + "?zenc=");
            handleErrors(br);
            packedJS.add(br.getRegex(BadongoCom.JAVASCRIPT).getMatch(0, 4));
            if (packedJS.get(2) == null) {
                packedJS.set(2, "{:'" + br.getRegex("window\\.location\\.href = '(.*?)\\?pr=").getMatch(0) + "?pr='");
            }
            plainJS = unpackJS(packedJS.get(2));
            /* Test */
            if (plainJS[1] == null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 1000l); }
            /* Next Request. Response final link as redirect */
            br.getPage(MAINPAGE + plainJS[1]);
            dl = BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), true, 1);
        } else {
            String dllink = br.getRegex("songFileSrc\",\\s?\"(.*?)\"\\)").getMatch(0);
            if (dllink == null && realURL.contains("/pic/")) {
                br.getPage(realURL + "?size=original");
                dllink = br.getRegex("<img src=\"(.*?)\"\\sborder").getMatch(0);
            } else {
                final String filename = br.getRegex("songFileName\",\\s?\"(.*?)\"\\)").getMatch(0);
                if (dllink == null || filename == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                if (!filename.contains("Unknown")) {
                    downloadLink.setName(filename);
                }
            }
            dllink = Encoding.urlDecode(dllink, true);
            dl = BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        }
        if (!dl.getConnection().isContentDisposition()) {
            final String page = br.loadConnection(dl.getConnection()) + "";
            br.getRequest().setHtmlCode(page);
            dl.getConnection().disconnect();
            handleErrors(br);
        }
        dl.startDownload();
    }

    @Override
    // TODO: Fix & Test Premium
    public void handlePremium(final DownloadLink parameter, final Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        isPremium();
        String link = null;
        br.getPage(parameter.getDownloadURL());
        this.sleep(5000l, parameter);
        if (parameter.getStringProperty("type", "single").equalsIgnoreCase("split")) {
            final String downloadLinks[] = br.getRegex("doDownload\\(.?'(.*?).?'\\)").getColumn(0);
            final int part = parameter.getIntegerProperty("part", 1);
            link = downloadLinks[part - 1];
            link = link + (char) (part + 96);
            br.getPage(link + "/ifr?pr=1&zenc=");
            link = link + "/loc?pr=1";
        } else {
            link = br.getRegex("onclick=\"return doDownload\\('(.*?)'\\)").getMatch(0);
            link = link.replaceFirst("/1$", "/0");
            br.getPage(link + "/ifr?zenc=");
        }
        if (link == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, link, true, 0);
        if (!dl.getConnection().isContentDisposition()) {

            final String page = br.loadConnection(dl.getConnection()) + "";
            br.getRequest().setHtmlCode(page);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public boolean isPremium() throws PluginException, IOException {
        br.getPage(MAINPAGE + "/de/");
        final String type = br.getRegex("Du bist zur Zeit als <b>(.*?)</b> eingeloggt").getMatch(0);
        if (type == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        if (new Regex(type, Pattern.compile("premium", Pattern.CASE_INSENSITIVE)).matches()) { return true; }
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    public void login(final Account account) throws Exception {
        setBrowserExclusive();
        br.setCookie(MAINPAGE, "badongoL", "de");
        // br.getPage("http://www.badongo.com");
        br.getPage(MAINPAGE + "/de/login");
        final Form form = br.getForm(0);
        form.put("username", Encoding.urlEncode(account.getUser()));
        form.put("password", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        if (br.getCookie(MAINPAGE, "badongo_user") == null || br.getCookie(MAINPAGE, "badongo_password") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setCookiesExclusive(true);
        br.setCookie(MAINPAGE, "badongoL", "de");
        br.getPage(downloadLink.getDownloadURL());
        /* File Password */
        if (br.containsHTML("Diese Datei ist zur Zeit : <b>Geschützt</b>")) {
            for (int i = 0; i <= 5; i++) {
                final Form pwForm = br.getFormbyProperty("name", "pwdForm");
                if (pwForm == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                String pass = downloadLink.getDecrypterPassword();
                if (pass == null) {
                    pass = Plugin.getUserInput(null, downloadLink);
                    if (pass == null) {
                        continue;
                    }
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
        final String filesize = br.getRegex(Pattern.compile("<div class=\"ffileinfo\">Ansichten.*?\\| Dateig.*?:(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        final String filename = br.getRegex("<div class=\"finfo\">(.*?)</div>").getMatch(0);
        if (filesize == null || filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (downloadLink.getStringProperty("type", "single").equalsIgnoreCase("single")) {
            downloadLink.setName(filename.trim());
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        } else {
            final String parts = StringFormatter.fillString(downloadLink.getIntegerProperty("part", 1) + "", "0", "", 3);
            downloadLink.setName(filename.trim() + "." + parts);
            if (downloadLink.getIntegerProperty("part", 1) == downloadLink.getIntegerProperty("parts", 1)) {
                final long bytes = SizeFormatter.getSize(filesize);
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
        if (new Regex(fun, "%[0-9a-fA-F]{2}").matches()) {
            final ScriptEngineManager manager = new ScriptEngineManager();
            final ScriptEngine engine = manager.getEngineByName("javascript");
            try {
                result = engine.eval(fun);
            } catch (final ScriptException e) {
                e.printStackTrace();
            }
        }
        String unpacked = result.toString();
        result = null;
        if (!unpacked.contains("'ext': ''")) {
            final String[] InitOpt = new Regex(unpacked, "(getFileLinkInitOpt =|getFileLinkInitOpt\\)) (.*?)\n").getColumn(1);
            if (InitOpt.length > 0) {
                final String z = new Regex(InitOpt[InitOpt.length - 1], "z = '(.*?)'").getMatch(0);
                result = InitOpt[InitOpt.length - 2].replaceAll("z':(.*),'h", "z':'" + z + "','h");
            }
        }
        if (result == null && unpacked.contains("getFileLinkInitOpt")) {
            result = unpacked;
        }
        if (result == null) {
            result = unpacked;
        }
        if (fun.contains("data")) {
            result = fun;
        }
        final String[] row = new String[10];
        unpacked = result.toString();
        int z = 0;
        unpacked = unpacked.replaceAll("\n|\r|\rb|\t| ", "");
        if (unpacked.length() <= 45 && new Regex(unpacked, "window.[0-9a-f]+").matches()) {
            row[0] = unpacked;
            return row;
        }
        final int a = unpacked.lastIndexOf("{");
        unpacked = unpacked.substring(a + 1, unpacked.length());
        final Pattern pattern = Pattern.compile("(:|=)'(.*?)'|window\\['[0-9a-f]+'\\].*?\"(\\d+)\"|\".(.*?)\"");
        final Matcher matcher = pattern.matcher(unpacked);
        while (matcher.find()) {
            z += 1;
            row[z] = matcher.group().toString().replaceAll("'|:|=|\"|window\\['[0-9a-f]+'\\]", "").trim();
        }
        return row;
    }
}
