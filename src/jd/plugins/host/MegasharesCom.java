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
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class MegasharesCom extends PluginForHost {
    static private final String AGB_LINK = "http://d01.megashares.com/tos.php";

    static private final String CODER = "JD-Team";
    static private final String HOST = "megashares.com";

    // http://d01.megashares.com/?d01=ec0acc7
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?megashares\\.com/\\?d.*", Pattern.CASE_INSENSITIVE);

    private static String PLUGIN_PASS = null;

    public MegasharesCom() {

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        String link = downloadLink.getDownloadURL();
        br.setDebug(false);
        br.getPage(link);
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        // Cookies holen
        if (br.containsHTML("continue using Free service")) {
            br.getPage(link);
        }
        // Password protection
       if(! checkPassword(downloadLink)){
           return;
       }
       
        // Sie laden gerade eine datei herunter
        if (br.containsHTML("You already have the maximum")) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(60 * 1000l);
            return;
        }
        // Reconnet/wartezeit check
        String[] dat = br.getRegex("Your download passport will renew.*?in (\\d+):<strong>(\\d+)</strong>:<strong>(\\d+)</strong>").getRow(0);
        if (dat != null) {

            long wait = Long.parseLong(dat[1]) * 60000l + Long.parseLong(dat[2]) * 1000l;
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            linkStatus.setValue(wait);
            return;
        }

        // Captchacheck
        if (br.containsHTML("Your Passport needs to be reactivated.")) {
            String captchaAddress = br.getRegex("<dt>Enter the passport reactivation code in the graphic, then hit the \"Reactivate Passport\" button.</dt>.*?<dd><img src=\"(.*?)\" alt=\"Security Code\" style=.*?>").getMatch(0);
            File file = this.getLocalCaptchaFile(this);
            Browser c = br.cloneBrowser();
            if (!Browser.download(file, c.openGetConnection(captchaAddress)) || !file.exists()) {
                logger.severe("Captcha download failed: " + captchaAddress);

                linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                return;
            }

            HashMap<String, String> input = HTMLParser.getInputHiddenFields(br + "");

            String code = this.getCaptchaCode(file, downloadLink);
            // /?d01=ec0acc7&
            String geturl = link + "&rs=check_passport_renewal&rsargs[]=" + code + "&rsargs[]=" + input.get("random_num") + "&rsargs[]=" + input.get("passport_num") + "&rsargs[]=replace_sec_pprenewal&rsrnd=" + (new Date().getTime());
            br.getPage(geturl);
            br.getPage(link);

            if (br.containsHTML("You already have the maximum")) {
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setValue(30 * 1000l);
                return;
            }
            if(! checkPassword(downloadLink)){
                return;
            }
        }
        // Downloadlink
        String url = br.getRegex("<div id=\"dlink\"><a href=\"(.*?)\">Click here to download</a>").getMatch(0);
        if (url == null) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        // aktuellen Fortschritt prüfen und range Header setzen
        long[] chunkProgress = downloadLink.getChunksProgress();
        if (chunkProgress != null) {
            br.getHeaders().put("Range", "bytes=" + (chunkProgress[0] + 1) + "-");

        }
        // Dateigröße holen
        dat = br.getRegex("<dt>Filename:&nbsp;<strong>(.*?)</strong>&nbsp;&nbsp;&nbsp;(.*?)</dt>").getRow(0);

        HTTPConnection con = br.openGetConnection(url);

        dl = new RAFDownload(this, downloadLink, con);
        downloadLink.setDownloadSize(Regex.getSize(dat[1]));
        dl.setFilesize(Regex.getSize(dat[1]));
//        dl.setFilesizeCheck(false);
        dl.setResume(true);
        dl.startDownload();
    }

    private boolean checkPassword(DownloadLink link) throws IOException {

       if (br.containsHTML("This link requires a password")) {
            Form form = br.getForm("Validate Password");
            String pass = link.getStringProperty("password");
            if (pass != null) {
                form.put("passText", pass);
                br.submitForm(form);
                if (!br.containsHTML("This link requires a password")) {
                    return true;
                }
            }
            pass = PLUGIN_PASS;
            if (pass != null) {
                form.put("passText", pass);
                br.submitForm(form);
                if (!br.containsHTML("This link requires a password")) {
                    return true;
                }
            }
            int i = 0;
            while ((i++) < 5) {
                pass = JDUtilities.getGUI().showUserInputDialog(JDLocale.LF("plugins.hoster.passquestion", "Link '%s' is passwordprotected. Enter password:", link.getName()));
                if (pass != null) {
                    form.put("passText", pass);
                    br.submitForm(form);
                    if (!br.containsHTML("This link requires a password")) {
                        PLUGIN_PASS = pass;
                        link.setProperty("password", pass);
                        return true;
                    }
                }
            }
            
            link.getLinkStatus().addStatus(LinkStatus.ERROR_FATAL);
            link.getLinkStatus().setErrorMessage("Link password wrong");
            return false;
        }
       return true;
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        return null;
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        String link = downloadLink.getDownloadURL();
        br.getPage(link);
        if (br.containsHTML("You already have the maximum")) {
            downloadLink.getLinkStatus().setStatusText("Unchecked due to already loading");
            return true;
        }
        if (br.containsHTML("continue using Free service")) {
            br.getPage(link);
        }

        if (br.containsHTML("This link requires a password")) {

            downloadLink.getLinkStatus().setStatusText("Password protected");
            return true;
        }
        String[] dat = br.getRegex("<dt>Filename:&nbsp;<strong>(.*?)</strong>&nbsp;&nbsp;&nbsp;(.*?)</dt>").getRow(0);
        if (dat == null) { return false; }
        downloadLink.setName(dat[0].trim());
        downloadLink.setDownloadSize(Regex.getSize(dat[1]));
        return true;

    }

    @Override
    public String getHost() {
        return HOST;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginForHost#reset()
     */

    @Override
    public String getPluginName() {
        return HOST;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginForHost#checkAvailability(jd.plugins.DownloadLink)
     */

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision: 2576 $", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
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