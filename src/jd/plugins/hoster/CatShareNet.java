//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "catshare.net" }, urls = { "http://(www\\.)?catshare\\.net/[A-Za-z0-9]{16}" }, flags = { 0 })
public class CatShareNet extends PluginForHost {

    private String BRBEFORE = "";
    private String HOSTER   = "http://catshare.net";

    // DEV NOTES
    // captchatype: recaptcha
    // non account: 1 * 1

    public CatShareNet(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(HOSTER + "/login");
    }

    public void checkErrors(DownloadLink theLink, boolean beforeRecaptcha) throws NumberFormatException, PluginException {
        // Some waittimes...
        if (beforeRecaptcha) {
            if (br.containsHTML("<h4 style=\"margin-right: 10px;\">Odczekaj <big id=\"counter\"></big> lub kup")) {
                // possible waiitime after last download
                String waitTime = br.getRegex("<script>[ \t\n\r\f]+var count = ([0-9]+);").getMatch(0);
                logger.warning("Waittime detected for link " + theLink.getDownloadURL());
                Long waitTimeSeconds = Long.parseLong(waitTime);
                if (waitTimeSeconds != 60l) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Waittime detected", (waitTimeSeconds + 5) * 1000L); }

            }
        }
    }

    // never got one, but left this for future usage
    public void checkServerErrors() throws NumberFormatException, PluginException {
        if (new Regex(BRBEFORE, Pattern.compile("No file", Pattern.CASE_INSENSITIVE)).matches()) throw new PluginException(LinkStatus.ERROR_FATAL, "Server error");
        if (new Regex(BRBEFORE, "(Not Found|<h1>(404 )?Not Found</h1>)").matches()) {
            logger.warning("Server says link offline, please recheck that!");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public void doFree(DownloadLink downloadLink, boolean resumable, int maxChunks) throws Exception, PluginException {
        String passCode = null;
        checkErrors(downloadLink, true);

        String dllink = null;
        long timeBefore = System.currentTimeMillis();
        boolean password = false;
        boolean skipWaittime = false;

        // only ReCaptcha
        Form dlForm = new Form();
        if (new Regex(BRBEFORE, "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)").matches()) {
            dlForm = br.getForm(0);
            if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            logger.info("Detected captcha method \"Re Captcha\" for this host");
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setForm(dlForm);
            String id = this.br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
            rc.setId(id);
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            Form rcform = rc.getForm();
            rcform.put("recaptcha_challenge_field", rc.getChallenge());
            rcform.put("recaptcha_response_field", Encoding.urlEncode(c));
            logger.info("Put captchacode " + c + " obtained by captcha metod \"Re Captcha\" in the form and submitted it.");
            dlForm = rc.getForm();
            // waittime is often skippable for reCaptcha handling
            // skipWaittime = true;
        } else {
            logger.warning("Unknown ReCaptcha method for: " + downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown ReCaptcha method!");
        }

        /* Captcha END */
        // if (password) passCode = handlePassword(passCode, dlForm, downloadLink);
        br.submitForm(dlForm);
        logger.info("Submitted DLForm");
        doSomething();
        checkErrors(downloadLink, false);
        dllink = getDllink();
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Regex didn't match!");
        }
        logger.info("Final downloadlink = " + dllink + " starting the download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            doSomething();
            checkServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) downloadLink.setProperty("pass", passCode);
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    // Removed fake messages which can kill the plugin
    public void doSomething() throws NumberFormatException, PluginException {
        BRBEFORE = br.toString();
        ArrayList<String> someStuff = new ArrayList<String>();
        ArrayList<String> regexStuff = new ArrayList<String>();
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        regexStuff.add("(display: none;\">.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");
        for (String aRegex : regexStuff) {
            String lolz[] = br.getRegex(aRegex).getColumn(0);
            if (lolz != null) {
                for (String dingdang : lolz) {
                    someStuff.add(dingdang);
                }
            }
        }
        for (String fun : someStuff) {
            BRBEFORE = BRBEFORE.replace(fun, "");
        }
    }

    @Override
    public String getAGBLink() {
        return HOSTER + "/regulamin";
    }

    public String getDllink() {
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = new Regex(BRBEFORE, "Download: <a href=\"(.*?)\"").getMatch(0);
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, false, 1);
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        final String downloadURL = link.getDownloadURL();
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadURL);
        doSomething();
        if (br.containsHTML("<title>Error 404</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String fileName = new Regex(BRBEFORE, "<h3 class=\"pull-left\" style=\"margin-left: 10px;\">(.*)</h3>[ \t\n\r\f]+<h3 class=\"pull-right\"").getMatch(0);

        String fileSize = new Regex(BRBEFORE, "<h3 class=\"pull-right\" style=\"margin-right: 10px;\">(.+?)</h3>").getMatch(0);

        if (fileName == null || fileSize == null) {
            logger.warning("For link: " + downloadURL + ", final filename or filesize is null!");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "filename or filesize not found!");
        }

        link.setName(fileName.trim());
        link.setDownloadSize(SizeFormatter.getSize(fileSize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}