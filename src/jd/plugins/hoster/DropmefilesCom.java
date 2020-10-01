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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dropmefiles.com" }, urls = { "https?://(?:www\\.)?dropmefiles\\.com/([A-Za-z0-9]{3,})" })
public class DropmefilesCom extends PluginForHost {
    public DropmefilesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://dropmefiles.com/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = -1;
    private String               dllink            = null;

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
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(getHost(), "language", "en");
        br.getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("due to ending of the share period|due to exceeding the limit|class=\"fileCount\">0</div>") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = setDllinkAndReturnFilename();
        String filesize = br.getRegex("class=\"fileSize\">([^<>\"]*?)<").getMatch(0);
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename.trim()));
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    private String setDllinkAndReturnFilename() {
        String filename = null;
        final String downloadurl_source = this.br.getRegex("download_btn dragout start_dl_btn.+data\\-downloadurl=\"([^\"]+)").getMatch(0);
        if (downloadurl_source != null && downloadurl_source.contains(":")) {
            final String[] dlinfo = downloadurl_source.split(":");
            if (dlinfo.length >= 3) {
                filename = dlinfo[1];
            }
            dllink = new Regex(downloadurl_source, "(https?.+)").getMatch(0);
        }
        return filename;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS);
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks) throws Exception, PluginException {
        Form pwform = getPwForm();
        if (pwform != null) {
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            String passCode = link.getDownloadPassword();
            if (passCode == null) {
                passCode = getUserInput("Password?", link);
            }
            pwform.setMethod(MethodType.POST);
            pwform.setAction("/s1/checkPassword");
            pwform.put("password", Encoding.urlEncode(passCode));
            pwform.put("uid", this.getFID(link));
            brc.submitForm(pwform);
            if (!brc.containsHTML("\"result\"\\s*:\\s*\"ok\"")) {
                /* Wrong password */
                link.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            } else {
                link.setDownloadPassword(passCode);
                br.getPage(link.getPluginPatternMatcher());
                setDllinkAndReturnFilename();
            }
        }
        if (dllink == null) {
            if (this.br.containsHTML("download when uploaded")) {
                /* 2017-02-22: User already get their downloadlinks while the upload is still ongoing! */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wait until the file is uploaded to download it");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection()) && dl.getConnection().getLongContentLength() < 1000) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server issue", 1 * 60 * 1000l);
        }
        dl.startDownload();
    }

    private Form getPwForm() {
        for (final Form form : this.br.getForms()) {
            if (form.hasInputFieldByName("password") && !form.hasInputFieldByName("username")) {
                return form;
            }
        }
        return null;
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