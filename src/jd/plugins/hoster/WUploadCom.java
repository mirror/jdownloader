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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wupload.com" }, urls = { "http://(www\\.)?wupload\\.com/file/\\d+" }, flags = { 0 })
public class WUploadCom extends PluginForHost {

    public WUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.wupload.com/terms-and-conditions";
    }

    private static final String MAINPAGE      = "http://www.wupload.com/";
    private static final String STARTTEXT     = "?start=1";
    private static final String RECAPTCHATEXT = "(showRecaptcha|Please enter the captcha below|Recaptcha\\.create)";
    private String              WAITREGEXED   = null;

    public void correctDownloadLink(DownloadLink link) {
        if (!link.getDownloadURL().contains("www.")) link.setUrlDownload(link.getDownloadURL().replace("http://", "http://www."));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        br.setCookie(MAINPAGE, "lang", "en");
        br.setCookie(MAINPAGE, "isJavascriptEnable", "1");
        br.setCookie(MAINPAGE, "role", "anonymous");
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!downloadLink.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return downloadLink.getAvailableStatus();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage(downloadLink.getDownloadURL() + STARTTEXT, "");
        // Check 2 times for short waittime
        shortWait(downloadLink);
        if (!shortWait(downloadLink) && WAITREGEXED != null) {
            if (Integer.parseInt(WAITREGEXED) > 60) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(WAITREGEXED) * 1001l);
            sleep(Integer.parseInt(WAITREGEXED) * 1001l, downloadLink);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        String dllink = getDllink();
        if (dllink == null) {
            String reCaptchaID = br.getRegex("Recaptcha\\.create\\(\"(.*?)\"").getMatch(0);
            if (reCaptchaID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            Form reCaptchaForm = new Form();
            reCaptchaForm.setMethod(MethodType.POST);
            reCaptchaForm.setAction(downloadLink.getDownloadURL() + STARTTEXT);
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setForm(reCaptchaForm);
            rc.setId(reCaptchaID);
            rc.load();
            for (int i = 0; i <= 5; i++) {
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                rc.setCode(c);
                if (br.containsHTML(RECAPTCHATEXT)) {
                    rc.reload();
                    continue;
                }
                break;
            }
            if (br.containsHTML(RECAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            dllink = getDllink();
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink.trim();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("link has expired")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            if (br.containsHTML("(>You can only download 1 file at a time|>To download many files at once <a href=)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private boolean shortWait(DownloadLink downloadLink) throws PluginException, IOException {
        WAITREGEXED = br.getRegex("var countDownDelay = (\\d+);").getMatch(0);
        String tm = br.getRegex("id=\\'tm\\' name=\\'tm\\' value=\\'(.*?)\\'").getMatch(0);
        String tm_hash = br.getRegex("id=\\'tm_hash\\' name=\\'tm_hash\\' value=\\'(.*?)\\'").getMatch(0);
        if (tm != null && tm_hash != null) {
            int waittime = 6;
            if (WAITREGEXED != null) waittime = Integer.parseInt(WAITREGEXED);
            sleep(waittime * 1001l, downloadLink);
            br.postPage(downloadLink.getDownloadURL() + STARTTEXT, "tm=" + tm + "&tm_hash=" + tm_hash);
            return true;
        }
        return false;
    }

    private String getDllink() {
        String dllink = br.getRegex("<p><a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://s\\d+\\.wupload\\.com/download/\\d+/.*?)\"").getMatch(0);
        return dllink;
    }

    @Override
    public void reset() {
    }

    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            StringBuilder sb = new StringBuilder();
            while (true) {
                sb.delete(0, sb.capacity());
                sb.append("redirect=&controls%5Bsubmit%5D=&links=");
                links.clear();
                while (true) {
                    /* we test 100 links at once */
                    if (index == urls.length || links.size() > 100) break;
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
                br.postPage("http://www.wupload.com/link-checker", sb.toString());
                for (DownloadLink dl : links) {
                    String linkPart = new Regex(dl.getDownloadURL(), "(wupload\\.com/file/\\d+)").getMatch(0);
                    if (linkPart == null) {
                        logger.warning(this.getHost() + " availablecheck is broken!");
                        return false;
                    }
                    if (br.containsHTML(linkPart + "</span></td>[\t\n\r ]+<td class=\"fileName\"><span>\\-</span> </td>")) {
                        dl.setAvailable(false);
                        continue;
                    }
                    String[][] fileData = br.getRegex(linkPart + "</span></td>[\t\n\r ]+<td class=\"fileName\"><span>(.*?)</span> \\((.*?)\\)</td>").getMatches();
                    String filename = fileData[0][0];
                    String filesize = fileData[0][1];
                    if (filename == null || filesize == null) {
                        logger.warning(this.getHost() + " availablecheck is broken!");
                        dl.setAvailable(false);
                        continue;
                    } else {
                        dl.setAvailable(true);
                    }
                    dl.setName(filename);
                    dl.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                if (index == urls.length) break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}