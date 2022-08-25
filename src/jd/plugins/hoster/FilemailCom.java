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

import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "filemail.com" }, urls = { "https?://(?:www\\.)?(filemail\\.com/d/[A-Za-z0-9]+|fil\\.email/[A-Za-z0-9]+)" })
public class FilemailCom extends PluginForHost {
    public FilemailCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.filemail.com/terms";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;
    private String               dllink            = null;
    private static final String  TYPE_SHORT        = ".+fil\\.email/[A-Za-z0-9]+";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setAllowedResponseCodes(new int[] { 500 });
        br.setFollowRedirects(false);
        final String url_with_transferid;
        if (link.getDownloadURL().matches(TYPE_SHORT)) {
            br.getPage(link.getDownloadURL());
            url_with_transferid = br.getRedirectLocation();
            if (url_with_transferid == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            url_with_transferid = link.getDownloadURL();
        }
        final String transferid = new Regex(url_with_transferid, "/d/([A-Za-z0-9]+)$").getMatch(0);
        if (transferid == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setLinkID(transferid);
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.getPage("https://www." + this.getHost() + "/api/transfer/get?filesLimit=100&skipreg=false&transferid=" + transferid);
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Map<String, Object> transfer = (Map<String, Object>) entries.get("transfer");
        final String message = (String) transfer.get("message");
        final boolean isExpired = ((Boolean) transfer.get("isexpired")).booleanValue();
        try {
            /* File information can still be available even for expired items. */
            final List<Object> ressourcelist = (List<Object>) transfer.get("files");
            if (ressourcelist.size() == 1) {
                final Map<String, Object> file = (Map<String, Object>) ressourcelist.get(0);
                link.setVerifiedFileSize(((Number) file.get("filesize")).longValue());
                link.setFinalFileName(file.get("filename").toString());
                link.setMD5Hash(file.get("md5").toString());
                dllink = file.get("downloadurl").toString();
            } else {
                /* Folder: Download all files as compressed .zip file. */
                if (StringUtils.isEmpty(message)) {
                    link.setFinalFileName(transferid + ".zip");
                } else {
                    link.setFinalFileName(transferid + "_" + message + ".zip");
                }
                link.setDownloadSize(((Number) transfer.get("size")).longValue());
                dllink = transfer.get("compressedfileurl").toString();
            }
        } finally {
            if (isExpired) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
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