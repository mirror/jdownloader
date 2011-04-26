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
import java.util.Random;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "piggyshare.com" }, urls = { "http://(www\\.)?piggyshare\\.com/file/[a-z0-9]+" }, flags = { 0 })
public class PiggyShareCom extends PluginForHost {

    public PiggyShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String FILEIDREGEX   = "piggyshare\\.com/file/(.+)";
    private static final String CAPTCHAFAILED = "statusText:\"Given words are incorrect\\!\"";

    @Override
    public String getAGBLink() {
        return "http://piggyshare.com/documents/terms";
    }

    // Info: files over 500 MB can only be downloaded by premium users BUT
    // we can skip that here muhaha^^
    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String addedLink = downloadLink.getDownloadURL();
        br.getPage(addedLink);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.findID();
        br.getPage("http://piggyshare.com/download/~starttimer/" + new Regex(addedLink, FILEIDREGEX).getMatch(0) + "/" + new Random().nextInt(1000000000));
        handleErrors();
        long timeBefore = System.currentTimeMillis();
        Form reCaptchaForm = new Form();
        reCaptchaForm.setMethod(MethodType.POST);
        reCaptchaForm.setAction("http://piggyshare.com/download/~download");
        rc.setForm(reCaptchaForm);
        for (int i = 0; i <= 5; i++) {
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            // Only wait on first attempt
            if (i == 0) {
                waitTime(timeBefore, downloadLink);
                br.getPage("http://piggyshare.com/download/~stoptimer/" + new Regex(addedLink, FILEIDREGEX).getMatch(0) + "/" + new Random().nextInt(1000000000));
                if (!br.containsHTML("status:\"ok\"")) {
                    logger.warning("Something might have gone wrong...");
                    logger.warning(br.toString());
                }
            }
            rc.setCode(c);
            if (br.containsHTML(CAPTCHAFAILED)) {
                rc.reload();
                continue;
            }
            break;
        }
        handleErrors();
        String dllink = br.getRegex("status:\"ok\", statusText:\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://st\\d+\\.piggyshare\\.com/download/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void waitTime(long timeBefore, DownloadLink downloadLink) throws PluginException {
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        // Ticket Time
        int tt = 60;
        String ttt = br.getRegex("id=\\\\\"timeToDownload\\\\\">(\\d+)</b>").getMatch(0);
        if (ttt != null) tt = Integer.parseInt(ttt);
        tt -= passedTime;
        logger.info("Waittime detected, waiting " + ttt + " - " + passedTime + " seconds from now on...");
        if (tt > 0) sleep(tt * 1001l, downloadLink);
    }

    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            br.setCookie("http://piggyshare.com/", "lang", "en");
            br.setFollowRedirects(false);
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            StringBuilder sb = new StringBuilder();
            while (true) {
                sb.delete(0, sb.capacity());
                sb.append("links=");
                links.clear();
                while (true) {
                    /*
                     * we test 10 links at once, checking more links at the same
                     * time isn't possible
                     */
                    if (index == urls.length || links.size() > 9) break;
                    links.add(urls[index]);
                    index++;
                }
                int c = 0;
                for (DownloadLink dl : links) {
                    /*
                     * append fake filename, because api will not report
                     * anything else
                     */
                    if (c > 0) sb.append("%0D%0A");
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    c++;
                }
                br.postPage("http://piggyshare.com/links/~check/", sb.toString());
                for (DownloadLink dl : links) {
                    String fileid = new Regex(dl.getDownloadURL(), FILEIDREGEX).getMatch(0);
                    if (fileid == null) {
                        logger.warning("Piggyshare availablecheck is broken!");
                        return false;
                    }
                    if (br.containsHTML("piggyshare\\.com/file/" + fileid + "\"")) {
                        dl.setAvailable(false);
                    } else {
                        Regex regexForThisLink = br.getRegex("\"<b>([\\w\\.]+)</b> \\(<b>([A-Za-z0-9\\. ]+)</b>\\)<br/><span style=\\\\\"color:gray\\\\\">http://piggyshare\\.com/file/" + fileid + "([\n\t\r]+)?</span><br/>\"");
                        String filename = regexForThisLink.getMatch(0);
                        String filesize = regexForThisLink.getMatch(1);
                        if (filename == null || filesize == null) {
                            logger.warning("Piggyshare availablecheck is broken!");
                            dl.setAvailable(false);
                            continue;
                        }
                        dl.setAvailable(true);
                        dl.setName(filename);
                        dl.setDownloadSize(SizeFormatter.getSize(filesize));
                    }
                }
                if (index == urls.length) break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void handleErrors() throws NumberFormatException, PluginException {
        if (br.containsHTML("<b>ERROR:</b> Wait")) {
            logger.warning("FATAL waittime error...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML(">ERROR:</b> You are waiting to download another file")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 30 * 1000l);
        String reconnectWaittime = br.getRegex("> You need to wait (\\d+) minutes in order to download another file").getMatch(0);
        if (reconnectWaittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWaittime) * 60 * 1001l);
        if (br.containsHTML(CAPTCHAFAILED)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (br.containsHTML("statusText:\"Your download link has expired\\!\"")) {
            logger.info("Downloadlink expired, retrying...");
            throw new PluginException(LinkStatus.ERROR_RETRY, "Downloadlink expired");
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!downloadLink.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return downloadLink.getAvailableStatus();
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