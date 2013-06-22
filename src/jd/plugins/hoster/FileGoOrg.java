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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filego.org" }, urls = { "http://(www\\.)?filego\\.org/\\?d=[A-Z0-9]+" }, flags = { 0 })
public class FileGoOrg extends PluginForHost {

    public FileGoOrg(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(COOKIE_HOST + "/register.php?g=3");
    }

    // MhfScriptBasic 1.6
    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/rules.php";
    }

    private static final String COOKIE_HOST = "http://filego.org";
    private static final String NOCHUNKS    = "NOCHUNKS";
    private static final String NORESUME    = "NORESUME";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // Not needed but using the latest FF is probably no bad idea
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:17.0) Gecko/20100101 Firefox/17.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.setCookie(COOKIE_HOST, "Fg_mylang", "en");
        br.getPage(parameter.getDownloadURL());
        if (br.getURL().contains("&code=DL_FileNotFound") || br.containsHTML(">The file did not exist or has been deleted") || br.containsHTML("innerHTML=\"The file did not exist or has been deleted\\.\\.\\.") || br.getURL().equals("http://filego.org/fileno.php")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("The file was not uploaded correctly, try again and follow instruction")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = getData("Filename:");
        if (filename == null) {
            // embeded
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
            if (filename == null) {
                // embeded failover
                filename = br.getRegex("<meta name=\"description\" content=\"(.*?), \"").getMatch(0);
                if (filename == null || filename.matches("")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        String filesize = getData("File size:");
        // no filesize for embed videos!
        parameter.setFinalFileName(filename.trim());
        if (filesize != null) parameter.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(downloadLink);

        String finalLink = null;
        // first try to get the streaming link (skips captcha)
        try {
            /**
             * We could also use: http://filego.org/fghtml5.php?id=ID or http://filego.org/fgdivx.php?id=ID
             */
            br.getPage("http://filego.org/fgflash.php?id=" + new Regex(downloadLink.getDownloadURL(), "([A-Z0-9]+)$").getMatch(0));
            final String[] hits = br.getRegex("\\'(http://s\\d+\\.filego\\.org/[^<>\";]*?\\.mp4)\\'").getColumn(0);
            for (final String hit : hits) {
                if (hits != null && hits.length != 0) {
                    try {
                        Browser br2 = br.cloneBrowser();
                        URLConnectionAdapter con = br2.openGetConnection(hit);
                        if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                            continue;
                        }
                        con.disconnect();
                        finalLink = hit;
                        break;
                    } catch (Exception e) {
                        finalLink = null;
                    }
                }
            }
        } catch (final Exception e) {
        }
        if (finalLink == null) {
            // embeded content
            final Form free = br.getFormbyProperty("name", "vali2");
            if (free == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            int val3 = Integer.parseInt(free.getInputField("val3").getValue());
            free.remove("val3");
            final String vvar = br.getRegex("var secon=(\\d+);").getMatch(0);
            final String add = br.getRegex("parseInt\\(secon\\) \\+ (\\d+);").getMatch(0);
            if (vvar != null && add != null) {
                val3 += Integer.parseInt(vvar) + Integer.parseInt(add);
            } else {
                val3 += 20;
            }
            free.put("val3", Integer.toString(val3));
            br.submitForm(free);
            // Download still works without account
            // if (br.containsHTML("Guest Can\\&#39;t Download Video")) {
            // try {
            // throw new PluginException(LinkStatus.ERROR_PREMIUM,
            // PluginException.VALUE_ID_PREMIUM_ONLY);
            // } catch (final Throwable e) {
            // if (e instanceof PluginException) throw (PluginException) e;
            // }
            // throw new PluginException(LinkStatus.ERROR_FATAL,
            // "Only downloadable via account!");
            // }
            // old stuff
            if (br.containsHTML("value=\"Free Users\"")) {
                br.postPage(downloadLink.getDownloadURL(), "Free=Free+Users");
            } else if (br.getFormbyProperty("name", "entryform1") != null) {
                br.submitForm(br.getFormbyProperty("name", "entryform1"));
            }
            final String rcID = br.getRegex("challenge\\?k=([^<>\"]*?)\"").getMatch(0);
            if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcID);
            rc.load();
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode(cf, downloadLink);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage(downloadLink.getDownloadURL(), "downloadverify=1&d=1&recaptcha_response_field=" + c + "&recaptcha_challenge_field=" + rc.getChallenge());
            if (br.containsHTML("incorrect\\-captcha\\-sol")) {
                try {
                    invalidateLastChallengeResponse();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                try {
                    validateLastChallengeResponse();
                } catch (final Throwable e) {
                }
            }
            finalLink = findLink();
            int wait = 100;
            final String waittime = br.getRegex("countdown\\((\\d+)\\);").getMatch(0);
            if (waittime != null) wait = Integer.parseInt(waittime);
            sleep(wait * 1001l, downloadLink);
        }

        int chunks = 0;
        boolean resume = true;
        if (downloadLink.getBooleanProperty(FileGoOrg.NORESUME, false)) resume = false;
        if (downloadLink.getBooleanProperty(FileGoOrg.NOCHUNKS, false) || resume == false) {
            chunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalLink, resume, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();

            if (br.containsHTML(">AccessKey is expired, please request")) throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL server error, waittime skipped?");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (downloadLink.getBooleanProperty(FileGoOrg.NORESUME, false) == false) {
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty(FileGoOrg.NORESUME, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(FileGoOrg.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(FileGoOrg.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    private String findLink() throws Exception {
        final String finalLink = br.getRegex("\\'(http://[a-z0-9]+\\.filego\\.org/fl/[a-z0-9]+/[^<>\"]*?)\\'").getMatch(0);
        if (finalLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return finalLink;
    }

    private String getData(final String data) {
        return br.getRegex("<b>" + data + "</b></td>[\t\n\r ]+<td>(<font size=\"\\d+\">)?([^<>\"]*?)<").getMatch(1);
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}