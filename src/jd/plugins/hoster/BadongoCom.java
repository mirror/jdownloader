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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
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
                    String passDlgMsg = JDL.L("plugins.hoster.general.enterpassword", "Enter password:");
                    if (i > 0) passDlgMsg = JDL.L("plugins.hoster.general.reenterpassword", "Wrong password. Please re-enter:");
                    pass = getUserInput(passDlgMsg, downloadLink);
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
        br.getPage(downloadLink.getDownloadURL());
        System.out.println(Encoding.UTF8Decode(br.toString()));
        // /* Nochmals das File überprüfen */
        // String link = null;
        // String realURL = downloadLink.getDownloadURL().replaceAll("\\.viajd",
        // ".com");
        // requestFileInformation(downloadLink);
        // if (downloadLink.getStringProperty("type",
        // "single").equalsIgnoreCase("split")) {
        // /* Get CaptchaCode */
        // br.getPage(realURL + "?rs=displayCaptcha&rst=&rsrnd=" +
        // System.currentTimeMillis() + "&rsargs[]=yellow");
        // Form form = br.getForm(0);
        // String cid = br.getRegex("cid=(\\d+)").getMatch(0);
        // String code =
        // getCaptchaCode("http://www.badongo.com/ccaptcha.php?cid=" + cid,
        // downloadLink);
        // form.setAction(br.getRegex("action=.\"(.+?).\"").getMatch(0));
        // form.put("user_code", code);
        // form.put("cap_id",
        // br.getRegex("cap_id.\"\\svalue=.\"(\\d+).\"").getMatch(0));
        // form.put("cap_secret",
        // br.getRegex("cap_secret.\"\\svalue=.\"([a-z0-9]+).\"").getMatch(0));
        // br.submitForm(form);
        // /* Errorhandling */
        // if (br.getRedirectLocation() != null) throw new
        // PluginException(LinkStatus.ERROR_CAPTCHA);
        // handleErrors(br);
        // /* Waittime */
        // String ttt = br.getRegex("var check_n = (\\d+)").getMatch(0);
        // int tt = 60;
        // if (ttt != null) {
        // logger.info("Waittime found by rexes is " + ttt + " seconds.");
        // tt = Integer.parseInt(ttt);
        // }
        // sleep(tt * 1001, downloadLink);
        // /* File or Video Link */
        // String fileOrVid = "";
        // if (realURL.contains("file/"))
        // fileOrVid = "getFileLink";
        // else
        // fileOrVid = "getVidLink";
        // br.getPage(realURL + "?rs=" + fileOrVid + "&rst=&rsrnd=" +
        // System.currentTimeMillis() + "&rsargs[]=yellow");
        // link = br.getRegex("doDownload\\(.'(.*?).'").getMatch(0);
        // if (link == null) throw new
        // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // br.getPage(link + "/ifr?pr=1&zenc=");
        // handleErrors(br);
        // br.getPage(link + "/loc?pr=1");
        // if (br.getRedirectLocation() == null) throw new
        // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink,
        // br.getRedirectLocation(), true, 1);
        // if (!dl.getConnection().isContentDisposition()) {
        // /*
        // * +"" due to refaktor compatibilities. old <ref10000 returns
        // * String. else Request INstance
        // */
        // br.loadConnection(dl.getConnection());
        // dl.getConnection().disconnect();
        // handleErrors(br);
        // }
        // dl.startDownload();
        // } else {
        // boolean pluginBroken = true;
        // if (pluginBroken) throw new
        // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // /* Single File */
        // Browser ajax = br.cloneBrowser();
        // ajax.setCookiesExclusive(true);
        // ajax.setFollowRedirects(false);
        // /* Get CaptchaCode */
        // ajax.getPage(realURL + "?rs=refreshImage&rst=&rsrnd=" +
        // System.currentTimeMillis());
        // String cid = ajax.getRegex("cid=(\\d+)").getMatch(0);
        // String fileID = new Regex(realURL, "(file|vid)/(\\d+)/").getMatch(1);
        // String capSecret =
        // ajax.getRegex("cap_secret value=(.*?)>").getMatch(0);
        // String action =
        // ajax.getRegex("post action=\\\"(.*?)\\\"").getMatch(0);
        // if (cid == null || fileID == null || capSecret == null || action ==
        // null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // String code =
        // getCaptchaCode("http://www.badongo.com/ccaptcha.php?cid=" + cid,
        // downloadLink);
        // String postData = "user_code=" + code + "&cap_id=" + cid +
        // "&cap_secret=" + capSecret;
        // ajax.postPage(action, postData);
        // /* Errorhandling */
        // if (ajax.getRedirectLocation() != null) throw new
        // PluginException(LinkStatus.ERROR_CAPTCHA);
        // ajax.getPage(ajax.getRedirectLocation());
        // handleErrors(ajax);
        // // Ab hier gibts packed java script, die aktuelle Wartezeit steht
        // // auch im JS d.h. da sollte sie geregexed werden und unten
        // // entsprechend lange gewartet werden
        // /* Waittime */
        // sleep(45500, downloadLink);
        // /* File or Video Link */
        // String fileOrVid = "";
        // if (realURL.contains("file/"))
        // fileOrVid = "getFileLink";
        // else
        // fileOrVid = "getVidLink";
        // /* Possibly wait for host */
        // for (int i = 0; i <= 20; i++) {
        // ajax.getPage(realURL + "?rs=" + fileOrVid + "&rst=&rsrnd=" +
        // System.currentTimeMillis() + "&rsargs[]=yellow");
        // if (!ajax.containsHTML("WAITING"))
        // break;
        // else if (i == 20) throw new
        // PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        // sleep(1000, downloadLink, "Waiting for host");
        // }
        // link = ajax.getRegex("doDownload\\(.'(.*?).'\\)").getMatch(0);
        // if (link == null) throw new
        // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // ajax.getPage((link + "/ifr?pr=1&zenc=").replace("/1/", "/0/"));
        // handleErrors(ajax);
        // ajax.getPage((link + "/loc?pr=1").replace("/1/", "/0/"));
        //
        // dl = BrowserAdapter.openDownload(ajax, downloadLink,
        // ajax.getRedirectLocation(), true, 1);
        // if (!dl.getConnection().isContentDisposition()) {
        // String page = ajax.loadConnection(dl.getConnection()) + "";// +""
        // // due
        // // to
        // // refaktor
        // // compatibilities.
        // // old
        // // <ref10000
        // // returns
        // // String.
        // // else
        // // Request
        // // INstance
        // ajax.getRequest().setHtmlCode(page);
        // dl.getConnection().disconnect();
        // handleErrors(ajax);
        // }
        // dl.startDownload();
        // }
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
