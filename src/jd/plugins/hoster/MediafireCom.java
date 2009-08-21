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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
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
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mediafire.com" }, urls = { "http://[\\w\\.]*?mediafire\\.com/(download\\.php\\?.+|\\?(?!sharekey).+|file/.+)" }, flags = { 0 })
public class MediafireCom extends PluginForHost {

    static private final String offlinelink = "tos_aup_violation";

    public MediafireCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.mediafire.com/terms_of_service.php";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException, InterruptedException {
        this.setBrowserExclusive();
        String url = downloadLink.getDownloadURL();

        for (int i = 0; i < 3; i++) {
            try {
                br.getPage(url);
                // rc= new Recaptcha(this);
                if (br.getRedirectLocation() != null && br.getCookie("http://www.mediafire.com", "ukey") != null) {
                    downloadLink.setProperty("type", "direct");
                    if (!downloadLink.getStringProperty("origin", "").equalsIgnoreCase("decrypter")) {
                        downloadLink.setName(Plugin.extractFileNameFromURL(br.getRedirectLocation()));
                    }
                    return AvailableStatus.TRUE;
                }
                break;
            } catch (IOException e) {
                if (e.getMessage().contains("code: 500")) {
                    logger.info("ErrorCode 500! Wait a moment!");
                    Thread.sleep(200);
                    continue;
                } else
                    return AvailableStatus.FALSE;
            }

        }
        if (br.getRegex(offlinelink).matches()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<title>(.*?)<\\/title>").getMatch(0);
        String filesize = br.getRegex("<input type=\"hidden\" id=\"sharedtabsfileinfo1-fs\" value=\"(.*?)\">").getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setFinalFileName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    // @Override
    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        String url = null;

        try {
            for (int i = 0; i < 3; i++) {
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

                }
                if (downloadLink.getStringProperty("type", "").equalsIgnoreCase("direct")) {
                    url = br.getRedirectLocation();
                } else {

                    if (!br.containsHTML("\\s+cu\\('")) {
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
                        if (form != null && !br.containsHTML("cu\\('[a-z0-9]*'")) {
                            link.setProperty("pass", null);
                            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
                        } else {
                            link.setProperty("pass", passCode);
                        }
                    }

                    String qk = null, pk = null, r = null;
                    String[] parameters = br.getRegex("\\s+cu\\('(.*?)','(.*?)','(.*?)'\\);").getRow(0);
                    qk = parameters[0];
                    pk = parameters[1];
                    r = parameters[2];

                    br.getPage("http://www.mediafire.com/dynamic/download.php?qk=" + qk + "&pk=" + pk + "&r=" + r);

                    String error = br.getRegex("var et=(.*?);").getMatch(0);
                    if (error != null && !error.trim().equalsIgnoreCase("15")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
                    String js = br.getRegex("'Your download is starting.*?(http.*)\\+ '\"> Click here to start download..</a>'").getMatch(0).trim();
                    String vars = br.getRegex("<!--(.*?)function").getMatch(0).trim();
                    Context cx = Context.enter();
                    Scriptable scope = cx.initStandardObjects();
                    String eval = "function f(){\r\n" + vars + "\r\n return \"" + js + ";\r\n}\r\n f();";
                    Object result = cx.evaluateString(scope, eval, "<cmd>", 1, null);

                    url = Context.toString(result);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
            dl.startDownload();
        } catch (EvaluatorException e) {
            // too complexx retry
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }
}
