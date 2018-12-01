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

import java.io.IOException;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "filehorse.com" }, urls = { "https?://(www\\.)?(mac\\.)?filehorse\\.com/download\\-[a-z0-9\\-]+/(\\d+/)?" })
public class FileHorseCom extends PluginForHost {
    public FileHorseCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filehorse.com/terms/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        // Offline links should also have nice filenames
        link.setName(new Regex(link.getDownloadURL(), "filehorse\\.com/download\\-(.+)").getMatch(0));
        br = new Browser();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>404 Error \\- Page Not Found<|>Page Not Found)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>\"]*?) Download for").getMatch(0);
        String filesize = br.getRegex("id=\"btn_file_size\">\\(([^<>\"]*?)\\)</span>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex(">File size / license:</p><p>(\\d+(\\.\\d+)? [A-Za-z]{1,5})").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex(">Download Now</span></a><p>\\(([^<>\"]*?)\\) Safe").getMatch(0);
        }
        final String pagepiece = br.getRegex("<div id=\"sidebar\">(.*?)<\\!\\-\\- AddThis Button BEGIN \\-\\->").getMatch(0);
        if (filesize == null && pagepiece != null) {
            filesize = new Regex(pagepiece, "(\\d+(\\.{1,2})? (B|b|MB|KB|GB))").getMatch(0);
        }
        if (filename == null || filesize == null) {
            if (!br.containsHTML("\"main_down_link\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()) + ".exe");
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL() + "download/");
        String dllink = br.getRegex("http-equiv=\"refresh\" content=\"\\d+; url=(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://(www\\.)?filehorse\\.com/download/file/[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("If it doesn't\\s*<a href=\"(https?://[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            Form d = br.getFormBySubmitvalue(Encoding.urlEncode("Download Now"));
            // NEW, recaptchav2
            if (d != null && d.containsHTML("google\\.com/recaptcha/")) {
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                d.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                br.submitForm(d);
                dllink = br.getRegex("<a href=\"(https?://(\\w+\\.)?filehorse\\.com/download/file/[^\"]+)").getMatch(0);
            } else {
                br.submitForm(d);
                dllink = br.getRegex("<a href=\"(https?://(\\w+\\.)?filehorse\\.com/download/file/[^\"]+)").getMatch(0);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, Encoding.htmlDecode(dllink.trim()), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}