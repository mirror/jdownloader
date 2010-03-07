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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.pluginUtils.Recaptcha;
import jd.utils.locale.JDL;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mediafire.com" }, urls = { "http://[\\w\\.]*?mediafire\\.com/(download\\.php\\?|\\?(?!sharekey)|file/).+" }, flags = { 2 })
public class MediafireCom extends PluginForHost {

    static private final String offlinelink = "tos_aup_violation";

    /** The name of the error page used by MediaFire */
    private static final String ERROR_PAGE = "error.php";

    /**
     * The number of retries to be performed in order to determine if a file is
     * available
     */
    private static final int NUMBER_OF_RETRIES = 3;

    public MediafireCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.mediafire.com/register.php");
        Browser.setRequestIntervalLimitGlobal(getHost(), 250);
    }

    @Override
    public String getAGBLink() {
        return "http://www.mediafire.com/terms_of_service.php";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("http://media", "http://www.media"));
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage("http://www.mediafire.com/");
        Form form = br.getFormbyProperty("name", "form_login1");
        if (form == null) form = br.getFormBySubmitvalue("login_email");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.put("login_email", Encoding.urlEncode(account.getUser()));
        form.put("login_pass", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        br.getPage("http://www.mediafire.com/myfiles.php");
        String acc = br.getRegex("Account:.*?style=\"margin.*?\">(.*?)</").getMatch(0);
        if (br.getCookie("http://www.mediafire.com", "user").equals("x") || !acc.equals("MediaPro")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        account.setValid(true);
        br.getPage("http://www.mediafire.com/myaccount.php");
        String hostedFiles = br.getRegex("> From.*?(\\d+).*?total files <").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Long.parseLong(hostedFiles));
        String usedspace = br.getRegex("Total Storage Used </div> <div style=\".*?div style=\"font-size.*?\">(.*?)</div").getMatch(0);
        if (usedspace != null) ai.setUsedSpace(usedspace.trim());
        String trafficleft = br.getRegex("Available Bandwidth </div> <div style=.*?<div style=\"font-size.*?\">(.*?)</div").getMatch(0);
        if (trafficleft != null) ai.setTrafficLeft(Regex.getSize(trafficleft.trim()));
        ai.setStatus("Premium User");
        return ai;
    }

    private void handlePW(DownloadLink downloadLink) throws Exception {
        if (br.containsHTML("dh\\(''\\)")) {
            String passCode;
            DownloadLink link = downloadLink;
            Form form = br.getFormbyProperty("name", "form_password");
            if (link.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }
            form.put("downloadp", passCode);
            br.submitForm(form);
            form = br.getFormbyProperty("name", "form_password");
            if (form != null && br.containsHTML("dh\\(''\\)")) {
                link.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
            } else {
                link.setProperty("pass", passCode);
            }
        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        String url = null;
        if (downloadLink.getStringProperty("type", "").equalsIgnoreCase("direct")) {
            logger.info("DirectDownload");
            url = br.getRedirectLocation();
        } else {
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
            handlePW(downloadLink);
            url = getDownloadUrl();
        }
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            logger.info("Error (4)");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException, InterruptedException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        downloadLink.setProperty("type", "");
        String url = downloadLink.getDownloadURL();
        AvailableStatus status = AvailableStatus.TRUE;
        for (int i = 0; i < NUMBER_OF_RETRIES; i++) {
            try {
                br.getPage(url);
                String redirectURL = br.getRedirectLocation();
                if (redirectURL != null && redirectURL.indexOf(ERROR_PAGE) > 0) {
                    status = AvailableStatus.FALSE;
                    String errorCode = redirectURL.substring(redirectURL.indexOf("=") + 1, redirectURL.length());
                    if (errorCode.equals("320")) {
                        logger.warning("The requested file ['" + url + "'] is invalid");
                    }
                    break;
                }

                if (redirectURL != null && br.getCookie("http://www.mediafire.com", "ukey") != null) {
                    if (url.contains("download.php") || url.contains("fire.com/file/")) {
                        /* new redirect format */
                        if (!new Regex(redirectURL, "http://download\\d+\\.mediafire").matches()) {
                            br.getPage(redirectURL);
                            break;
                        }
                    }
                    downloadLink.setProperty("type", "direct");
                    if (!downloadLink.getStringProperty("origin", "").equalsIgnoreCase("decrypter")) {
                        downloadLink.setName(Plugin.extractFileNameFromURL(redirectURL));
                    }
                    return AvailableStatus.TRUE;
                }

                break;
            } catch (IOException e) {
                if (e.getMessage().contains("code: 500")) {
                    logger.info("ErrorCode 500! Wait a moment!");
                    Thread.sleep(200);
                    continue;
                } else {
                    status = AvailableStatus.FALSE;
                }
            }
        }
        if (status == AvailableStatus.FALSE) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.getRegex(offlinelink).matches()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)<\\/title>").getMatch(0);
        String filesize = br.getRegex("<input type=\"hidden\" id=\"sharedtabsfileinfo1-fs\" value=\"(.*?)\">").getMatch(0);
        if (filesize == null) filesize = br.getRegex("<input type=\"hidden\" id=\"sharedtabsfileinfo-fs\" value=\"(.*?)\">").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setFinalFileName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        status = AvailableStatus.TRUE;
        return status;
    }

    /**
     * get the parameters for dynamicdownloadpage and id for downloadbutton
     * 
     * @return
     * @throws PluginException
     */
    private String[] getParameters() throws Exception {
        String eval = br.getRegex("(var [a-zA-Z0-9]+='';var [a-zA-Z0-9]+=unes.*?)eval\\(").getMatch(0);
        String evalWhat = br.getRegex("var [a-zA-Z0-9]+='';var [a-zA-Z0-9]+=unes.*?eval\\((.*?)\\)").getMatch(0);
        String params = null;
        try {
            while (true) {
                if (eval == null || evalWhat == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                String fun = "function f(){ " + eval + " return " + evalWhat + "} f()";
                Context cx = ContextFactory.getGlobal().enter();
                Scriptable scope = cx.initStandardObjects();
                Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
                params = Context.toString(result);
                if (!params.startsWith("var") && !params.startsWith("eval")) break;
                eval = new Regex(params, "(var.*?)eval\\(").getMatch(0);
                evalWhat = new Regex(params, "var.*?eval\\((.*?)\\)").getMatch(0);
            }
        } catch (PluginException e2) {
            throw e2;
        } catch (Exception e) {
            logger.severe(params + " " + eval);
            throw e;
        }
        String ret[] = new Regex(params, "'(.*?)'").getColumn(0);
        if (ret == null || ret.length != 3) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String id = new Regex(params, "(.*?)\\(").getMatch(0);
        String ret2[] = new String[] { ret[0], ret[1], ret[2], id };
        return ret2;
    }

    /**
     * evals js script
     * 
     * @param input
     * @return
     * @throws Exception
     */
    private String evalLoop(String input) throws Exception {
        String eval = new Regex(input, "(var.*?)(eval\\(|$)").getMatch(0).trim();
        String evalWhat = new Regex(input, "var(.*?)=").getMatch(0).trim();
        String params = null;
        try {
            while (true) {
                if (eval == null || evalWhat == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                String fun = "function f(){ " + eval + " return " + evalWhat + "} f()";
                Context cx = ContextFactory.getGlobal().enter();
                Scriptable scope = cx.initStandardObjects();
                Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
                params = Context.toString(result);
                if (!params.startsWith("var") && !params.startsWith("eval")) break;
                eval = new Regex(params, "(var.*?)(eval\\(|$)").getMatch(0).trim();
                evalWhat = new Regex(params, "var(.*?)=").getMatch(0).trim();
            }
        } catch (PluginException e2) {
            throw e2;
        } catch (Exception e) {
            logger.severe(params + " " + eval);
            throw e;
        }
        return params;
    }

    private String getDownloadUrl() throws Exception {
        String url = null;
        String vars = null;
        String idmatch = null;
        String qk = null, pk = null, r = null;
        String[] parameters = getParameters();
        qk = parameters[0];
        pk = parameters[1];
        r = parameters[2];
        /*
         * these are our downloadbutton id's, we need them to find the right one
         * for building downloadurl later
         */
        String[] ids = br.getRegex("function " + parameters[3] + "\\(.*?io=.*?ElementbyId\\('(.*?)'").getColumn(0);
        // String[] ids =
        // br.getRegex("<div class=\".*?\" style=\".*?\" id=\"(.*?)\"").getColumn(0);
        br.getPage("http://www.mediafire.com/dynamic/download.php?qk=" + qk + "&pk=" + pk + "&r=" + r);
        String error = br.getRegex("var et=(.*?);").getMatch(0);
        /* now we have to js the functions */
        String evals[] = br.getRegex("(var [a-zA-Z0-9]+='';var [a-zA-Z0-9]+=unescape.*?)eval\\(").getColumn(0);
        if (error != null && !error.trim().equalsIgnoreCase("15")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
        /* all var parts to build a downloadurl */
        vars = br.getRegex("<!--(.*?)function").getMatch(0);
        if (vars == null) {
            logger.info("Error (1)");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String[][] tmpvars = new Regex(vars.trim(), "var.*?([a-zA-Z0-9]*?)\\s*?=\\s*?'(.*?)';").getMatches();
        HashMap<String, String> varmap = new HashMap<String, String>();
        for (String[] tmp : tmpvars) {
            varmap.put(tmp[0], tmp[1]);
        }
        for (String eval : evals) {
            String evaled = evalLoop(eval);
            String searchID = new Regex(evaled, "getElementById\\('(.*?)'\\)").getMatch(0);
            if (searchID == null) continue;
            for (String id : ids) {
                if (id.equalsIgnoreCase(searchID)) {
                    idmatch = new Regex(evaled, "(http:.*?)\"> Cl").getMatch(0);
                    if (idmatch == null) continue;
                    break;
                }
            }
            // idmatch = br.getRegex(id + ".*?(http:.*?)\\+'\">").getMatch(0);
            if (idmatch != null) {
                /*
                 * we found the right id, so lets build our downloadurl
                 */
                while (true) {
                    if (idmatch == null) break;
                    String nextreplace = new Regex(idmatch, "\\+(.*?)(\\+|$)").getMatch(0);
                    if (nextreplace == null) break;
                    String match = varmap.get(nextreplace.trim());
                    if (match != null) {
                        idmatch = idmatch.replaceFirst("\\+" + nextreplace, match);
                    } else {
                        idmatch = idmatch.replaceFirst("\\+" + nextreplace, nextreplace.replaceAll("'", ""));
                    }
                }
                if (idmatch != null) url = idmatch.replaceAll(" |\"", "");
                break;
            }
        }
        /* we need a valid url now, if not then something went wrong */
        if (url == null) {
            logger.info("Error (2)");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return url;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        String url = null;
        br.setDebug(true);
        for (int i = 0; i < NUMBER_OF_RETRIES; i++) {
            if (url != null) break;
            requestFileInformation(downloadLink);
            try {
                Recaptcha rc = new Recaptcha(br);
                Form form = br.getFormbyProperty("name", "form_captcha");
                String id = br.getRegex("e\\?k=(.+?)\"").getMatch(0);
                if (id != null) {
                    rc.setId(id);
                    InputField challenge = new InputField("recaptcha_challenge_field", null);
                    InputField code = new InputField("recaptcha_response_field", null);
                    form.addInputField(challenge);
                    form.addInputField(code);
                    rc.setForm(form);
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());

                    try {
                        String c = getCaptchaCode(cf, downloadLink);
                        rc.setCode(c);
                    } catch (PluginException e) {
                        /**
                         * captcha input timeout run out.. try to reconnect
                         */
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
                    }
                }
            } catch (Exception e) {
                JDLogger.exception(e);
            }

            if (downloadLink.getStringProperty("type", "").equalsIgnoreCase("direct")) {
                logger.info("DirectDownload");
                url = br.getRedirectLocation();
            } else {
                handlePW(downloadLink);
                url = getDownloadUrl();
            }
        }
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            logger.info("Error (3)");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("type", "");
    }
}
