//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.nutils.JDHash;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "duckload.com" }, urls = { "http://[\\w\\.]*?(duckload\\.com|youload\\.to)/(download/[a-z0-9]+|(divx|play)/[A-Z0-9\\.-]+|[a-zA-Z0-9\\.]+)" }, flags = { 0 })
public class DuckLoad extends PluginForHost {

    private static final String MAINPAGE  = "http://duckload.com/";
    private static final String FLASHPAGE = "http://flash.duckload.com/video/";

    public DuckLoad(final PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(3000l);
    }

    @Override
    public String getAGBLink() {
        return "http://duckload.com/impressum.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        this.requestFileInformation(downloadLink);
        String dllink = null;
        // Filedownload now with recaptcha and without waittime
        if (downloadLink.getDownloadURL().contains("/download/")) {
            this.br.setFollowRedirects(false);
            String id = this.br.getRegex("recaptcha/api/challenge\\?k=(.*?)\"").getMatch(0);
            if (id != null) {
                Boolean failed = true;
                for (int i = 0; i <= 5; i++) {
                    id = this.br.getRegex("recaptcha/api/challenge\\?k=(.*?)\"").getMatch(0);
                    if (id == null) {
                        Plugin.logger.warning("id is null...");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    id = id.trim();
                    final Form reCaptchaForm = this.br.getForm(1);
                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(this.br);
                    rc.setForm(reCaptchaForm);
                    rc.setId(id);
                    rc.load();
                    final File cf = rc.downloadCaptcha(this.getLocalCaptchaFile());
                    final String c = this.getCaptchaCode(cf, downloadLink);
                    rc.getForm().put("recaptcha_response_field", c);
                    rc.getForm().put("recaptcha_challenge_field", rc.getChallenge());
                    rc.getForm().put("free_dl", "");
                    this.br.submitForm(rc.getForm());
                    if (this.br.getRedirectLocation() == null) {
                        this.br.getPage(downloadLink.getDownloadURL());
                        continue;
                    }
                    failed = false;
                    break;
                }
                if (failed) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
                dllink = this.br.getRedirectLocation().toString();
            }
        } else {
            this.br.setFollowRedirects(true);
            int waitThis = 20;
            final String wait = this.br.getRegex("id=\"number\">(\\d+)</span> seconds").getMatch(0);
            if (wait != null) {
                waitThis = Integer.parseInt(wait);
            }
            this.sleep(waitThis * 1001l, downloadLink);
            this.br.postPage(this.br.getURL(), "secret=&next=true");
        }
        if (dllink == null) {
            dllink = this.br.getRegex("<param name=\"src\" value=\"(http://.*?)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = this.br.getRegex("\"(http://dl\\d+\\.duckload\\.com/Get/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+/[A-Z0-9]+)\"").getMatch(0);
            // swf-Download
            if ((dllink == null) && this.br.containsHTML("duckloadplayer\\.swf")) {
                final long cache = System.currentTimeMillis();
                final String id = this.br.getURL().substring(this.br.getURL().lastIndexOf("/") + 1);
                final String md5 = JDHash.getMD5(id + cache + "SuperSecretSalt");
                final long random = cache / 2 + 7331;
                dllink = DuckLoad.FLASHPAGE + "api.php?id=" + id + "&random=" + random + "&md5=" + md5 + "&cache=" + cache;
                this.br.getHeaders().put("Referer", DuckLoad.FLASHPAGE + "duckloadplayer.swf?id=" + id + "&cookie=/[[DYNAMIC]]/3");
                this.br.getHeaders().put("x-flash-version", "10,1,53,64");
                this.br.getPage(dllink);
                this.br.getHeaders().clear();
                final String part1 = this.br.getRegex("ident\":\"(.*?)\",").getMatch(0);
                String part2 = this.br.getRegex("link\":\"(.*?)\"").getMatch(0);
                if (part2 != null) {
                    part2 = part2.replace("\\/", "/");
                }
                dllink = "http://dl" + part1 + ".duckload.com" + part2;
                if ((part1 == null) || (part2 == null)) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            } else {
                final String part1 = this.br.getRegex("\\'ident=(.*?)\\';").getMatch(0);
                final String part2 = this.br.getRegex("\\'token=(.*?)\\&\\';").getMatch(0);
                final String part3 = this.br.getRegex("\\'\\&filename=(.*?)\\&\\';").getMatch(0);
                if ((part1 == null) || (part2 == null) || (part3 == null)) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                dllink = "http://www.duckload.com/api/as2/link/" + part1 + "/" + part2 + "/" + part3;
                int secondWait = 10;
                final String secondWaitRegexed = this.br.getRegex("\\'timetowait=(\\d+)\\&\\'").getMatch(0);
                if (secondWaitRegexed != null) {
                    secondWait = Integer.parseInt(secondWaitRegexed);
                }
                this.sleep(secondWait * 1001l, downloadLink);
            }
        }
        this.dl = BrowserAdapter.openDownload(this.br, downloadLink, dllink, true, -2);
        if (this.dl.getConnection().getContentType().contains("html")) {
            this.br.followConnection();
            if (((this.br.getURL() != null) && this.br.getURL().contains("/error/")) || this.br.containsHTML("ErrorCode: e983")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error"); }
            if (this.br.getRequest().getUrl().toString().contentEquals("http://" + this.br.getHost() + "/")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Redirect error"); }
            this.dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.setFilenameFix(true);
        this.dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        this.br.setCookie(DuckLoad.MAINPAGE, "dl_set_lang", "en");
        this.br.setFollowRedirects(true);
        this.br.getPage(link.getDownloadURL());
        if (this.br.containsHTML("(File not found\\.|download\\.notfound)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = this.br.getRegex("<title>(.*?) @ DuckLoad\\.com</title>").getMatch(0);
        if (filename == null) {
            filename = this.br.getRegex("Download from <strong>\"(.*?)\"</strong>").getMatch(0);
        }
        if (this.br.containsHTML("\\(<i>")) {
            String filesize = this.br.getRegex("\\(<i>(.*?)</i>").getMatch(0);
            final String unit = this.br.getRegex("\\(<i>.*?strong>(\\w+)</strong>\\)").getMatch(0);
            filesize += unit;
            if ((filesize != null) && (unit != null)) {
                link.setDownloadSize(Regex.getSize(filesize.trim()));
            }
        }
        // Server doesn't give us the correct name so we set it here
        if (!filename.contains("."))
            link.setName(filename.trim());
        else
            link.setFinalFileName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}