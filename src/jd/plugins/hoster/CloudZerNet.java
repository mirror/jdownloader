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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cloudzer.net" }, urls = { "http://(www\\.)?cloudzer\\.net/file/[a-z0-9]+" }, flags = { 0 })
public class CloudZerNet extends PluginForHost {

    public CloudZerNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://cloudzer.net/legal";
    }

    public static class StringContainer {
        public String string = null;
    }

    private Pattern                IPREGEX       = Pattern.compile("(([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9]))", Pattern.CASE_INSENSITIVE);
    private static AtomicBoolean   hasDled       = new AtomicBoolean(false);
    private static AtomicLong      timeBefore    = new AtomicLong(0);
    private String                 LASTIP        = "LASTIP";
    private static StringContainer lastIP        = new StringContainer();
    private static final long      RECONNECTWAIT = 3600000;
    private static String[]        IPCHECK       = new String[] { "http://ipcheck0.jdownloader.org", "http://ipcheck1.jdownloader.org", "http://ipcheck2.jdownloader.org", "http://ipcheck3.jdownloader.org" };

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().equals("http://cloudzer.net/404") || br.containsHTML(">Please check the URL for typing errors")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("id=\"filename\">([^<>\"]*?)</b>").getMatch(0);
        String filesize = br.getRegex("<span class=\"size\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String currentIP = getIP();
        try {
            /**
             * Experimental reconnect handling to prevent having to enter a captcha just to see that a limit has been reached
             */
            logger.info("New Download: currentIP = " + currentIP);
            if (hasDled.get() && ipChanged(currentIP, downloadLink) == false) {
                long result = System.currentTimeMillis() - timeBefore.get();
                if (result < RECONNECTWAIT && result > 0) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, RECONNECTWAIT - result);
            }
            final String fid = new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
            final Browser br2 = br.cloneBrowser();
            br2.getPage("http://cloudzer.net/js/download.js");
            final String rcID = br2.getRegex("Recaptcha\\.create\\(\"([^<>\"]*?)\"").getMatch(0);
            if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getHeaders().put("X-Prototype-Version", "1.7");
            final String waittime = br.getRegex("name=\"wait\" content=\"(\\d+)\"").getMatch(0);

            br.postPage("http://cloudzer.net/io/ticket/slot/" + fid, "");
            if (!br.containsHTML("\"succ\":true")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            final long timeBefore = System.currentTimeMillis();
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcID);
            rc.load();
            for (int i = 1; i <= 5; i++) {
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, downloadLink);
                if (i == 1) {
                    waitTime(timeBefore, downloadLink, waittime);
                }
                br.postPage("http://cloudzer.net/io/ticket/captcha/" + fid, "recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                if (br.containsHTML("\"err\":\"captcha\"")) {
                    rc.reload();
                    continue;
                }
                break;
            }
            if (br.containsHTML("\"err\":\"captcha\"")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            if (br.containsHTML("\"err\":\"limit\\-dl\"")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
            if (br.containsHTML("Sie haben die max\\. Anzahl an Free\\-Downloads")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Max. simultan free downloads limit reached!", 5 * 60 * 1000l);
            String dllink = get("url");
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dllink = dllink.replace("\\", "");
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
            dl.startDownload();
            hasDled.set(true);
        } catch (Exception e) {
            hasDled.set(false);
            throw e;
        } finally {
            timeBefore.set(System.currentTimeMillis());
            setIP(currentIP, downloadLink);
        }
    }

    private void waitTime(long timeBefore, final DownloadLink downloadLink, final String waittime) throws PluginException {
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        /** Ticket Time */
        int wait = 30;
        if (waittime != null) wait = Integer.parseInt(waittime);
        wait -= passedTime;
        if (wait > 0) sleep(wait * 1000l, downloadLink);
    }

    private String get(final String parameter) {
        return br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
    }

    private String getIP() throws PluginException {
        Browser ip = new Browser();
        String currentIP = null;
        ArrayList<String> checkIP = new ArrayList<String>(Arrays.asList(IPCHECK));
        Collections.shuffle(checkIP);
        for (String ipServer : checkIP) {
            if (currentIP == null) {
                try {
                    ip.getPage(ipServer);
                    currentIP = ip.getRegex(IPREGEX).getMatch(0);
                    if (currentIP != null) break;
                } catch (Throwable e) {
                }
            }
        }
        if (currentIP == null) {
            logger.warning("firewall/antivirus/malware/peerblock software is most likely is restricting accesss to JDownloader IP checking services");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return currentIP;
    }

    private boolean setIP(String IP, final DownloadLink link) throws PluginException {
        synchronized (IPCHECK) {
            if (IP != null && !new Regex(IP, IPREGEX).matches()) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (ipChanged(IP, link) == false) {
                // Static IP or failure to reconnect! We don't change lastIP
                logger.warning("Your IP hasn't changed since last download");
                return false;
            } else {
                String lastIP = IP;
                link.setProperty(LASTIP, lastIP);
                CloudZerNet.lastIP.string = lastIP;
                logger.info("LastIP = " + lastIP);
                return true;
            }
        }
    }

    private boolean ipChanged(String IP, DownloadLink link) throws PluginException {
        String currentIP = null;
        if (IP != null && new Regex(IP, IPREGEX).matches()) {
            currentIP = IP;
        } else {
            currentIP = getIP();
        }
        if (currentIP == null) return false;
        String lastIP = link.getStringProperty(LASTIP, null);
        if (lastIP == null) lastIP = CloudZerNet.lastIP.string;
        return !currentIP.equals(lastIP);
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}