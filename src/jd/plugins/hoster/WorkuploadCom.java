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

import java.io.IOException;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "workupload.com" }, urls = { "https?://(?:www\\.|en\\.)?workupload\\.com/(?:file|start)/([A-Za-z0-9]+)" })
public class WorkuploadCom extends PluginForHost {
    public WorkuploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://workupload.com/tos";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = false;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 20;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 410 });
        br.getPage("https://" + this.getHost() + "/file/" + this.getFID(link));
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410 || this.br.containsHTML("img/404\\.jpg\"|>Whoops\\! 404|> Datei gesperrt")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String sha256 = br.getRegex("(?i)([A-Fa-f0-9]{64})\\s*\\(SHA256\\)").getMatch(0);
        if (sha256 != null) {
            link.setSha256Hash(sha256);
        }
        if (isPasswordProtected(br)) {
            link.getLinkStatus().setStatusText("This url is password protected");
        } else {
            String filename = br.getRegex("<td>Dateiname:</td><td>([^<>\"]*?)<").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("class=\"intro\">[\n\t\r ]*?<b>([^<>\"]+)</b>").getMatch(0);
            }
            String filesize = br.getRegex("<td>Dateigröße:</td><td>([^<>\"]*?)<").getMatch(0);
            if (filename == null || filesize == null) {
                Regex filenameSize = br.getRegex("<p class=\"intro\">[\n\t\r ]*?<b>(.*?)</b>[^\n\t\r <>\"]*?(\\d+(?:\\.\\d+)? ?(KB|MB|GB))[^\n\t\r <>\"]*?");
                if (filename == null) {
                    filename = filenameSize.getMatch(0);
                }
                if (filesize == null) {
                    filesize = filenameSize.getMatch(1);
                }
            }
            if (filesize == null) {
                filesize = br.getRegex("(\\d+(?:\\.\\d+)? ?(KB|MB|GB))").getMatch(0);
            }
            if (filesize == null) {
                filesize = br.getRegex("(\\d+(?:\\.\\d+)? ?(?:B(?:ytes?)?))").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
            }
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setName(Encoding.htmlDecode(filename).trim());
            if (filesize != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            }
        }
        return AvailableStatus.TRUE;
    }

    private boolean isPasswordProtected(final Browser br) {
        return br.containsHTML("id=\"passwordprotected_file_password\"");
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        final String first_url = this.br.getURL();
        // String dllink = checkDirectLink(downloadLink, directlinkproperty);
        String dllink = null;
        if (dllink == null) {
            if (isPasswordProtected(br)) {
                String passCode = link.getDownloadPassword();
                if (passCode == null) {
                    passCode = getUserInput("Password?", link);
                }
                this.br.postPage(this.br.getURL(), "passwordprotected_file%5Bpassword%5D=" + Encoding.urlEncode(passCode) + "&passwordprotected_file%5Bsubmit%5D=&passwordprotected_file%5Bkey%5D=" + this.getFID(link));
                if (isPasswordProtected(br)) {
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                } else {
                    /* User entered valid password */
                    link.setDownloadPassword(passCode);
                }
            }
            this.br.getPage("/start/" + this.getFID(link));
            this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            this.br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            this.br.getPage("/api/file/getDownloadServer/" + this.getFID(link));
            dllink = PluginJSonUtils.getJsonValue(this.br, "url");
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            }
        }
        this.br.getHeaders().put("Referer", first_url);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (br.getURL().contains("/file/")) {
                logger.info("Final downloadurl redirected to main url");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setFilenameFix(true);
        link.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}