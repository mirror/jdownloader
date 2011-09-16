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
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filejungle.com" }, urls = { "http://(www\\.)?filejungle\\.com/f/[A-Z0-9]+" }, flags = { 0 })
public class FileJungleCom extends PluginForHost {

    public FileJungleCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filejungle.com/terms_and_conditions.php";
    }

    private static final String CAPTCHAFAILED = "\"error\":\"incorrect\\-captcha\\-sol\"";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        checkLinks(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return link.getAvailableStatus();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        final String damnLanding = br.getRegex("\"(/landing/L\\d+/download_captcha\\.js\\?\\d+)\"").getMatch(0);
        final String id = br.getRegex("var reCAPTCHA_publickey=\\'([^\\'\"<>]+)\\'").getMatch(0);
        final String rcShortenCode = br.getRegex("id=\"recaptcha_shortencode_field\" name=\"recaptcha_shortencode_field\" value=\"([^\\'\"<>]+)\"").getMatch(0);
        if (id == null || damnLanding == null || rcShortenCode == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.filejungle.com" + damnLanding);
        Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.setCustomCharset("utf-8");
        final String postlink = downloadLink.getDownloadURL() + "/" + downloadLink.getName();
        br2.postPage(postlink, "checkDownload=check");
        if (br2.containsHTML("\"captchaFail\"")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many wrong captcha attempts!", 10 * 60 * 1000l);
        if (br2.containsHTML("\"fail\":\"timeLimit\"")) {
            br.postPage(postlink, "checkDownload=showError&errorType=timeLimit");
            String reconnectWait = br.getRegex("Please wait for (\\d+) seconds to download the next file").getMatch(0);
            int wait = 600;
            if (reconnectWait != null) wait = Integer.parseInt(reconnectWait);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
        }
        if (!br2.containsHTML("\"success\":\"showCaptcha\"")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br2);
        Form dlform = new Form();
        dlform.setMethod(MethodType.POST);
        dlform.setAction("http://www.filejungle.com/checkReCaptcha.php");
        dlform.put("recaptcha_shortencode_field", rcShortenCode);
        rc.setForm(dlform);
        rc.setId(id);
        rc.load();
        for (int i = 0; i <= 5; i++) {
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            rc.setCode(c);
            if (br2.containsHTML(CAPTCHAFAILED)) {
                rc.reload();
                continue;
            }
            break;
        }
        if (br2.containsHTML(CAPTCHAFAILED)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (!br2.containsHTML("\"success\":1")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br2.postPage(postlink, "downloadLink=wait");
        int wait = 30;
        String waittime = br2.getRegex("\"waitTime\":(\\d+),\"").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        br2.postPage(postlink, "downloadLink=show");
        // Use normal browser here
        br.postPage(postlink, "download=normal");
        if (br.containsHTML("(>File is not available<|>The page you requested cannot be displayed right now|The file may have removed by the uploader or expired\\.<)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            final Browser checkbr = new Browser();
            checkbr.getHeaders().put("Accept-Encoding", "");
            checkbr.setCustomCharset("utf-8");
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            final StringBuilder sb = new StringBuilder();
            while (true) {
                sb.delete(0, sb.capacity());
                sb.append("urls=");
                links.clear();
                while (true) {
                    /*
                     * we test 100 links at once - its tested with 500 links,
                     * probably we could test even more at the same time...
                     */
                    if (index == urls.length || links.size() > 100) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                int c = 0;
                for (final DownloadLink dl : links) {
                    /*
                     * append fake filename, because api will not report
                     * anything else
                     */
                    if (c > 0) {
                        sb.append("%0D%0A");
                    }
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    c++;
                }
                checkbr.postPage("http://www.filejungle.com/check_links.php", sb.toString());
                for (final DownloadLink dl : links) {
                    final String linkpart = new Regex(dl.getDownloadURL(), "(filejungle\\.com/f/.+)").getMatch(0);
                    if (linkpart == null) {
                        logger.warning("Filejungle availablecheck is broken!");
                        return false;
                    }
                    final String regexForThisLink = "(http://(www\\.)?" + linkpart + "</a></div>[\t\n\r ]+<div class=\"col2\">.*?</div>[\t\n\r ]+<div class=\"col3\">.*?</div>[\t\n\r ]+<div class=\"col4\"><span class=\"icon (approved|declined))";
                    final String theData = checkbr.getRegex(regexForThisLink).getMatch(0);
                    if (theData == null) {
                        logger.warning("Filejungle availablecheck is broken!");
                        return false;
                    }
                    final Regex linkinformation = new Regex(theData, ".*?</a></div>[\t\n\r ]+<div class=\"col2\">(.*?)</div>[\t\n\r ]+<div class=\"col3\">(.*?)</div>[\t\n\r ]+<div class=\"col4\"><span class=\"icon (approved|declined)");
                    final String status = linkinformation.getMatch(2);
                    String filename = linkinformation.getMatch(0);
                    String filesize = linkinformation.getMatch(1);
                    if (filename == null || filesize == null) {
                        logger.warning("Filejungle availablecheck is broken!");
                        dl.setAvailable(false);
                    } else if (!status.equals("approved") || filename.equals("--") || filesize.equals("--")) {
                        filename = linkpart;
                        dl.setAvailable(false);
                    } else {
                        dl.setAvailable(true);
                    }
                    dl.setName(filename);
                    if (filesize != null) {
                        if (filesize.contains(",") && filesize.contains(".")) {
                            /* workaround for 1.000,00 MB bug */
                            filesize = filesize.replaceFirst("\\.", "");
                        }
                        dl.setDownloadSize(SizeFormatter.getSize(filesize));
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
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