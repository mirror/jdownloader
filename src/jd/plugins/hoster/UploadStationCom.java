//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadstation.com" }, urls = { "http://(www\\.)?uploadstation\\.com/file/[A-Za-z0-9]+" }, flags = { 0 })
public class UploadStationCom extends PluginForHost {

    public UploadStationCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.uploadstation.com/toc.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<h1>File not available</h1>|<b>The file could not be found\\. Please check the download link)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex("<div class=\"download_item\">(.*?) \\(([\\d+\\.]+ [A-Za-z]{1,8})\\)</div>");
        String filename = fileInfo.getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        // Works like fileserve.com, they use the same scripts
        requestFileInformation(downloadLink);
        this.handleErrors(br);
        final String fileId = this.br.getRegex("uploadstation\\.com/file/([a-zA-Z0-9]+)").getMatch(0);
        this.br.setFollowRedirects(false);
        // Not needed(yet)
        // String captchaJSPage =
        // this.br.getRegex("\"(/landing/[A-Za-z0-9]+/download_captcha\\.js\\?(\\d+)?)\"").getMatch(0);
        // if (captchaJSPage == null) {
        // logger.warning("captchaJSPage is null...");
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        // captchaJSPage = "http://uploadstation.com" + captchaJSPage;
        final Browser br2 = this.br.cloneBrowser();
        br2.setCustomCharset("utf-8");
        // br2.getPage(captchaJSPage);
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.postPage(downloadLink.getDownloadURL(), "checkDownload=check");
        if (!br2.containsHTML("success\":\"showCaptcha\"")) {
            logger.info("There seems to be an error, no captcha is shown!");
            handleCaptchaErrors(br2, downloadLink);
            handleErrors(br2);
        }
        Boolean failed = true;
        for (int i = 0; i <= 10; i++) {
            final String id = this.br.getRegex("var reCAPTCHA_publickey=\\'(.*?)\\';").getMatch(0);
            if ((!br.containsHTML("api\\.recaptcha\\.net") && !br.containsHTML("\"javascript:Recaptcha\\.reload")) || id == null) {
                handleCaptchaErrors(br2, downloadLink);
                logger.warning("id or fileId is null or the browser doesn't contain the reCaptcha text...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Form reCaptchaForm = new Form();
            reCaptchaForm.setMethod(Form.MethodType.POST);
            reCaptchaForm.setAction("http://www.uploadstation.com/checkReCaptcha.php");
            reCaptchaForm.put("recaptcha_shortencode_field", fileId);
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(this.br);
            rc.setForm(reCaptchaForm);
            rc.setId(id);
            rc.load();
            final File cf = rc.downloadCaptcha(this.getLocalCaptchaFile());
            final String c = this.getCaptchaCode(cf, downloadLink);
            if (c == null || c.length() == 0) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Recaptcha failed");
            rc.getForm().put("recaptcha_response_field", c);
            rc.getForm().put("recaptcha_challenge_field", rc.getChallenge());
            br2.submitForm(rc.getForm());
            if (br2.containsHTML("incorrect-captcha-sol")) {
                handleCaptchaErrors(br2, downloadLink);
                this.br.getPage(downloadLink.getDownloadURL());
                continue;
            }
            failed = false;
            break;
        }
        if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        handleCaptchaErrors(br2, downloadLink);
        handleErrors(br2);
        this.br.postPage(downloadLink.getDownloadURL(), "downloadLink=wait");
        // Ticket Time
        if (!this.br.getHttpConnection().isOK()) {
            logger.warning("The connection is not okay...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String reconTime = br.toString();
        int tt = 10;
        if (reconTime.length() < 500) {
            reconTime = new Regex(reconTime, ".*?(\\d+).*?").getMatch(0);
            logger.info("Waittime detected, waiting " + reconTime + " seconds from now on...");
            tt = Integer.parseInt(reconTime.trim());
        } else {
            logger.warning("Couldn't find dynamic waittime");
            logger.warning(br.toString());
        }
        this.sleep(tt * 1001, downloadLink);
        br2.postPage(downloadLink.getDownloadURL(), "downloadLink=show");
        this.br.postPage(downloadLink.getDownloadURL(), "download=normal");
        final String dllink = this.br.getRedirectLocation();
        if (dllink == null) {
            this.handleErrors(br);
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, dllink, false, 1);
        if (this.dl.getConnection().getResponseCode() == 404) {
            logger.info("got a 404 error...");
            this.br.followConnection();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (this.dl.getConnection().getContentType().contains("html")) {
            logger.info("The finallink doesn't seem to be a file...");
            this.br.followConnection();
            this.handleErrors(br);
            logger.warning("Unexpected error at the last step...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setFilenameFix(true);
        this.dl.startDownload();
    }

    private void handleErrors(Browser br2) throws PluginException {
        logger.info("Handling errors...");
        String waittime = br.getRegex("<h1>You need to wait (\\d+) seconds to download next file<br>").getMatch(0);
        if (waittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 1001l);
        if (br.containsHTML("To remove download restriction, please choose your suitable plan as below</h1>")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
    }

    private void handleCaptchaErrors(Browser br2, DownloadLink downloadLink) throws IOException, PluginException {
        // Handles captcha errors and additionsl limits
        logger.info("Checking captcha errors...");
        if (br.containsHTML("No htmlCode read")) {
            logger.info("Unexpected captcha error happened");
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String fail = br2.getRegex("\"(fail|error)\":\"(.*?)\"").getMatch(1);
        String waittime = br2.getRegex("\"(waitTime|msg)\":(\\d+)").getMatch(1);
        if (fail != null && waittime != null) {
            if (fail.equals("captcha-fail") || fail.equals("captchaFail")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many wrong captcha attempts!", 10 * 60 * 1000l);
            br2.postPage(downloadLink.getDownloadURL(), "checkDownload=showError&errorType=" + fail + "&waitTime=" + waittime);
            // Just an additional check
            if (br2.containsHTML("Please retry later\\.<") || br2.containsHTML(">Your IP has failed the captcha too many times")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many wrong captcha attempts!", 10 * 60 * 1000l);
        } else if (fail != null) {
            // This coiuld be a limit message which appears after posting this,
            // it should then be handled with handleErrors
            br2.postPage(downloadLink.getDownloadURL(), "checkDownload=showError&errorType=" + fail);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}