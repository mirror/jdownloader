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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class ShareOnlineBiz extends PluginForHost {
    private String captchaCode;
    private File captchaFile;
    private String passCode;
    private String url;

    public ShareOnlineBiz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://share-online.biz/rules.php";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        url = downloadLink.getDownloadURL();
        for (int i = 1; i < 3; i++) {
            try {
                Thread.sleep(1000);/*
                                    * Sicherheitspause, sonst gibts 403 Response
                                    */
                String page = br.getPage(url);
                if (page != null && br.getRedirectLocation() == null) {
                    String filename = br.getRegex(Pattern.compile("<b>File name:</b></td>.*?<td align=left width=150px><div style=.*?><b>(.*?)</b></div></td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
                    String sizev = br.getRegex(Pattern.compile("<br>You have requested <font color=.*?>.*?</font> \\((.*?)\\) .</b>", Pattern.CASE_INSENSITIVE)).getMatch(0);

                    if (filename == null || sizev == null) return false;
                    downloadLink.setDownloadSize(Regex.getSize(sizev));
                    downloadLink.setName(filename);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        url = downloadLink.getDownloadURL();
        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        /* Captcha File holen */
        captchaFile = getLocalCaptchaFile(this);
        HTTPConnection captcha_con = br.cloneBrowser().openGetConnection("http://www.share-online.biz/captcha.php");
        if (captcha_con.getContentType().contains("text")) {
            /* Fehler beim Captcha */
            logger.severe("Captcha Download fehlgeschlagen!");
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        Browser.download(captchaFile, captcha_con);
        /* CaptchaCode holen */
        captchaCode = Plugin.getCaptchaCode(captchaFile, this, downloadLink);
        Form form = br.getForm(1);
        if (form.containsHTML("name=downloadpw")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            form.put("downloadpw", passCode);
        }

        /* Überprüfen(Captcha,Password) */
        form.put("captchacode", captchaCode);
        br.submitForm(form);
        if (br.containsHTML("Captcha number error or expired") || br.containsHTML("Unfortunately the password you entered is not correct")) {
            if (br.containsHTML("Unfortunately the password you entered is not correct")) {
                /* PassCode war falsch, also Löschen */
                downloadLink.setProperty("pass", null);
            }
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }

        /* Downloadlimit erreicht */
        if (br.containsHTML("max allowed download sessions") | br.containsHTML("this download is too big")) {
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            linkStatus.setValue(3600000l);
            return;
        }

        /* PassCode war richtig, also Speichern */
        downloadLink.setProperty("pass", passCode);
        /* DownloadLink holen, thx @dwd */
        String all = br.getRegex("eval\\(unescape\\(.*?\"\\)\\)\\);").getMatch(-1);
        String dec = br.getRegex("loadfilelink\\.decode\\(\".*?\"\\);").getMatch(-1);
        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects();
        String fun = "function f(){ " + all + "\nreturn " + dec + "} f()";
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
        url = Context.toString(result);
        Context.exit();

        /* 15 seks warten */
        sleep(15000, downloadLink);

        /* Datei herunterladen */
        br.openDownload(downloadLink, url).startDownload();

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
