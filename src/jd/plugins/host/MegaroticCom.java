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

package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class MegaroticCom extends PluginForHost {

    // static private final String COOKIE = "l=de; v=1; ve_view=1";

    static private final String ERROR_FILENOTFOUND = "Die Datei konnte leider nicht gefunden werden";

    static private final String ERROR_TEMP_NOT_AVAILABLE = "Zugriff auf die Datei ist vor";

    private static final String PATTERN_PASSWORD_WRONG = "Wrong password! Please try again";

    static private final int PENDING_WAITTIME = 45000;

    static private final String SIMPLEPATTERN_CAPTCHA_POST_URL = "<form method=\"POST\" action=\"(.*?)\" target";

    static private final String SIMPLEPATTERN_CAPTCHA_URl = " <img src=\"/capgen\\.php?(.*?)\">";

    static private final String SIMPLEPATTERN_GEN_DOWNLOADLINK = "var (.*?) = String\\.fromCharCode\\(Math\\.abs\\((.*?)\\)\\);(.*?)var (.*?) = '(.*?)' \\+ String\\.fromCharCode\\(Math\\.sqrt\\((.*?)\\)\\);";

    static private final String SIMPLEPATTERN_GEN_DOWNLOADLINK_LINK = "Math\\.sqrt\\((.*?)\\)\\);(.*?)document\\.getElementById\\(\"(.*?)\"\\)\\.innerHTML = '<a href=\"(.*?)' (.*?) '(.*?)\"(.*?)onclick=\"loadingdownload\\(\\)";

    private String captchaPost;

    private String captchaURL;

    private HashMap<String, String> fields;

    private boolean tempUnavailable = false;

    public MegaroticCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://www.megarotic.com/terms/";
    }

    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());

        String filename = br.getRegex("Dateiname:</font></b>(.*?)</div>").getMatch(0).trim();
        String size = br.getRegex("Dateigr..e:</font></b>(.*?)</div>").getMatch(0).trim();
        if (filename == null || filename.length() == 0) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(size));
        return true;
    }

    public String getFileInformationString(DownloadLink downloadLink) {
        return (tempUnavailable ? "<Temp. unavailable> " : "") + downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
    }

    public String getVersion() {
        return getVersion("$Revision$");
    }

    public void handleFree(DownloadLink parameter) throws Exception {
        LinkStatus linkStatus = parameter.getLinkStatus();
        getFileInformation(parameter);
        DownloadLink downloadLink = (DownloadLink) parameter;
        String link = downloadLink.getDownloadURL().replaceAll("/de", "");
        br.setFollowRedirects(true);

        br.setCookie(parameter.getDownloadURL(), "l", "de");
        br.setCookie(parameter.getDownloadURL(), "v", "1");
        br.setCookie(parameter.getDownloadURL(), "ve_view", "1");
        br.getPage(link);
        if (br.containsHTML(ERROR_TEMP_NOT_AVAILABLE)) {

        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);

        }
        if (br.containsHTML(ERROR_FILENOTFOUND)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        }

        captchaURL = "http://" + new URL(link).getHost() + "/capgen.php" + br.getRegex(SIMPLEPATTERN_CAPTCHA_URl).getMatch(0);
        fields = HTMLParser.getInputHiddenFields(br + "", "checkverificationform", "passwordhtml");
        captchaPost = br.getRegex(SIMPLEPATTERN_CAPTCHA_POST_URL).getMatch(0);

        if (captchaURL.endsWith("null") || captchaPost == null) {
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
        }

        File file = this.getLocalCaptchaFile(this);
        logger.info("Captcha " + captchaURL);
        HTTPConnection con = br.cloneBrowser().openGetConnection(captchaURL);

        Browser.download(file, con);

        String code = Plugin.getCaptchaCode(file, this, downloadLink);

        br.postPage(captchaPost, HTMLParser.joinMap(fields, "=", "&") + "&imagestring=" + code);
        if (br.getRegex(SIMPLEPATTERN_CAPTCHA_URl).getMatch(0) != null) {

            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }

        String pwdata = HTMLParser.getFormInputHidden(br + "", "passwordbox", "passwordcountdown");
        if (pwdata != null && pwdata.indexOf("passkey") > 0) {
            logger.info("Password protected");
            String pass = Plugin.getUserInput(null, parameter);
            if (pass == null) {

                linkStatus.addStatus(LinkStatus.ERROR_FATAL);
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.wrongpassword", "Password wrong"));

                return;
            }

            br.postPage("http://" + new URL(link).getHost() + "/de/", pwdata + "&pass=" + pass);

            if (br.containsHTML(PATTERN_PASSWORD_WRONG)) {
                linkStatus.addStatus(LinkStatus.ERROR_FATAL);
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.wrongpassword", "Password wrong"));

                return;
            }

        }
        sleep(PENDING_WAITTIME, downloadLink);

        String[] tmp = br.getRegex(SIMPLEPATTERN_GEN_DOWNLOADLINK).getRow(0);
        Character l = (char) Math.abs(Integer.parseInt(tmp[1].trim()));
        String i = tmp[4] + (char) Math.sqrt(Integer.parseInt(tmp[5].trim()));
        tmp = br.getRegex(SIMPLEPATTERN_GEN_DOWNLOADLINK_LINK).getRow(0);
        String url = Encoding.htmlDecode(tmp[3] + i + l + tmp[5]);
        br.setDebug(true);

        dl = br.openDownload(downloadLink, url, true, 1);
        // dl = RAFDownload.download(downloadLink, br.createRequest(url));
        // dl.setResume(true);
        if (!dl.getConnection().isOK()) {

            logger.warning("Download Limit!");
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            String wait = dl.getConnection().getHeaderField("Retry-After");
            dl.getConnection().disconnect();
            logger.finer("Warten: " + wait + " minuten");
            if (wait != null) {
                linkStatus.setValue(Integer.parseInt(wait.trim()) * 60 * 1000);
            } else {
                linkStatus.setValue(120 * 60 * 1000);
            }
            return;

        }
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
        }

        dl.startDownload();
        // Wenn ein Download Premium mit mehreren chunks angefangen wird, und
        // dann versucht wird ihn free zu resumen, schlägt das fehl, weil jd die
        // mehrfachchunks aus premium nicht resumen kann.
        // In diesem Fall wird der link resetted.
        if (linkStatus.hasStatus(LinkStatus.ERROR_DOWNLOAD_FAILED) && linkStatus.getErrorMessage().contains("Limit Exceeded")) {
            downloadLink.setChunksProgress(null);
            linkStatus.setStatus(LinkStatus.ERROR_RETRY);
        }

    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void reset() {
        captchaPost = null;
        captchaURL = null;
        fields = null;
    }

    public void resetPluginGlobals() {
    }
}
