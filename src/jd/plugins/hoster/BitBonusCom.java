//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

/** Is similar to FilesMonsterCom */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bitbonus.com" }, urls = { "http://bitbonusdecrypted\\.com/(download/[A-Za-z0-9\\-_]+/free/2/|download/)[A-Za-z0-9\\-_]+" }, flags = { 0 })
public class BitBonusCom extends PluginForHost {

    private static final String PREMIUMONLYUSERTEXT = "Only downloadable via premium";
    private static final String CAPTCHAFAILED       = "(Captcha number error or expired|api\\.recaptcha\\.net)";
    private static Object       LOCK                = new Object();

    public BitBonusCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("bitbonusdecrypted.com/", "bitbonus.com/"));
    }

    @Override
    public String getAGBLink() {

        return "http://bitbonus.com/rules.php";

    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(false);
        if (downloadLink.getDownloadURL().contains("/free/2/")) {
            br.getPage(downloadLink.getStringProperty("mainlink"));
            if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            downloadLink.setFinalFileName(downloadLink.getName());
            downloadLink.setDownloadSize(downloadLink.getDownloadSize());
        } else {
            final URLConnectionAdapter con = br.openGetConnection(downloadLink.getDownloadURL());
            if (con.getResponseCode() == 500) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            br.followConnection();
            if (br.containsHTML("(>Internal Server Error|Please notify the webmaster if you believe there is a problem|<title>BitBonus\\.com</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            synchronized (LOCK) {

                JDUtilities.getPluginForDecrypt("bitbonus.com");

            }

            String filename = br.getRegex(jd.plugins.decrypter.BitBonusComDecrypt.FILENAMEREGEX).getMatch(0);
            String filesize = jd.plugins.decrypter.BitBonusComDecrypt.getSize(br);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            if (filesize != null) {
                downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
            }
        }
        if (downloadLink.getStringProperty("PREMIUMONLY") != null) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.bitbonuscom.only4premium", PREMIUMONLYUSERTEXT));
        return AvailableStatus.TRUE;

    }

    private String getNewTemporaryLink(String mainlink, String originalfilename) throws IOException, PluginException {
        // Find a new temporary link
        String mainlinkpart = new Regex(mainlink, "bitbonus\\.com/download/(.+)").getMatch(0);
        String temporaryLink = null;
        String[] allInfo = jd.plugins.decrypter.BitBonusComDecrypt.getTempLinks(br);
        if (allInfo != null && allInfo.length != 0) {
            for (String singleInfo : allInfo)
                if (singleInfo.contains("\"name\":\"" + originalfilename + "\"")) temporaryLink = new Regex(singleInfo, "\"dlcode\":\"(.*?)\"").getMatch(0);
        }
        if (temporaryLink != null) temporaryLink = "http://bitbonus.com/download/" + mainlinkpart + "/free/2/" + temporaryLink + "/";
        return temporaryLink;
    }

    private void handleErrors() throws PluginException {
        logger.info("Handling errors...");
        String wait = br.getRegex("You can wait for the start of downloading (\\d+)").getMatch(0);
        if (wait == null) {
            wait = br.getRegex("is already in use (\\d+)").getMatch(0);
            if (wait == null) {
                wait = br.getRegex("You can start new download in (\\d+)").getMatch(0);
                if (wait == null) {
                    if (wait == null) {
                        wait = br.getRegex("will be available for free download in (\\d+) min\\.").getMatch(0);
                        if (wait == null) {
                            wait = br.getRegex("<br>Next free download will be available in (\\d+) min").getMatch(0);
                            if (wait == null) {
                                wait = br.getRegex("will be available for free download in (\\d+) min").getMatch(0);
                            }
                        }
                    }
                }
            }

        }
        if (wait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(wait) * 60 * 1001l);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (downloadLink.getStringProperty("PREMIUMONLY") != null) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.bitbonus.only4premium", PREMIUMONLYUSERTEXT));
        handleErrors();
        br.setFollowRedirects(true);
        downloadLink.getLinkStatus().setStatusText("Waiting for ticket...");
        String newTemporaryLink = getNewTemporaryLink(downloadLink.getStringProperty("mainlink"), downloadLink.getStringProperty("origfilename"));
        if (newTemporaryLink == null) {
            logger.warning("Failed to find a new temporary link for this link...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(newTemporaryLink);
        /* now we have the data page, check for wait time and data id */
        // Captcha handling
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        for (int i = 0; i <= 5; i++) {
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            rc.setCode(c);
            if (br.containsHTML(CAPTCHAFAILED)) continue;
            break;
        }
        handleErrors();
        if (br.containsHTML(CAPTCHAFAILED)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String strangeLink = br.getRegex("get_link\\(\\'(/download/.*?)\\'\\)").getMatch(0);
        if (strangeLink == null) {
            logger.warning("The following string could not be found: strangeLink");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        strangeLink = "http://bitbonus.com" + strangeLink;
        String regexedwaittime = br.getRegex("id=\\'sec\\' class=\"seconds\">(\\d+)</span>").getMatch(0);
        if (regexedwaittime == null) regexedwaittime = br.getRegex("var timeout=\\'(\\d+)\\';").getMatch(0);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("X-Prototype-Version", "1.6.0.3");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        int shortWaittime = 45;
        if (regexedwaittime != null) {
            shortWaittime = Integer.parseInt(regexedwaittime);
        } else {
            logger.warning("Waittime regex doesn't work, using default waittime...");
        }
        sleep(shortWaittime * 1100l, downloadLink);
        try {
            br.getPage(strangeLink);
        } catch (Exception e) {
        }
        if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available", 30 * 60 * 1000l);
        handleErrors();
        String dllink = br.getRegex("url\":\"(http:.*?)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("The following string could not be found: dllink");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The downloadlink doesn't seem to refer to a file, following the connection...");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}