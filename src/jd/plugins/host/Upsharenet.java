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
import java.net.URL;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;

public class Upsharenet extends PluginForHost {
    private String captchaCode;
    private File captchaFile;
    private String downloadurl;
    private String passCode = null;

    public Upsharenet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.upshare.net/faq.php?setlang=en";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {

        /* .eu zu .net weiterleitung */
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("upshare\\.(net|eu)", "upshare\\.net"));
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());

        downloadurl = downloadLink.getDownloadURL();
        try {
            br.getPage(downloadurl);

            if (!br.containsHTML("Your requested file is not found")) {
                String linkinfo[][] = new Regex(br, Pattern.compile("<b>File size:</b></td>[\\r\\n\\s]*<td align=left>([0-9\\.]*) ([GKMB]*)</td>", Pattern.CASE_INSENSITIVE)).getMatches();

                if (linkinfo[0][1].matches("MB")) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(linkinfo[0][0]) * 1024 * 1024));
                } else if (linkinfo[0][1].matches("KB")) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(linkinfo[0][0]) * 1024));
                }
                downloadLink.setName(new URL(downloadurl).getQuery().substring(3));
                return true;
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
    }

    @Override
    public String getVersion() {
        
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        Form form = br.getForms()[1];
        /* Captcha File holen */
        captchaFile = getLocalCaptchaFile(this);

        br.getDownload(captchaFile, "http://www.upshare.net/captcha.php");

        /* CaptchaCode holen */
        captchaCode = Plugin.getCaptchaCode(captchaFile, this, downloadLink);

        form.put("captchacode", captchaCode);
        /* Passwort holen holen */
        if (form.hasInputFieldByName("downloadpw")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            form.put("downloadpw", passCode);
        }
        /* Pass/Captcha check */
        br.submitForm(form);

        if (br.containsHTML("<span>Password Error</span>")) {
            /* PassCode war falsch, also Löschen */
            downloadLink.setProperty("pass", null);
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        if (br.containsHTML("<span>Captcha number error or expired</span>")) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        if (br.containsHTML("<span>You have got max allowed download sessions from the same IP!</span>")) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.setValue(60 * 60 * 1000);
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            return;
        }
        /* PassCode war richtig, also Speichern */
        downloadLink.setProperty("pass", passCode);
        /* DownloadLink holen */
        String link = new Regex(br, Pattern.compile("document.location=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (link == null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            return;
        }
        br.openDownload(downloadLink, link).startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        /* TODO: Wert prüfen */
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }

}
