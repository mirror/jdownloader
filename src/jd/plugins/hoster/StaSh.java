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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sta.sh" }, urls = { "https?://(?:www\\.)?sta\\.sh/(zip/)?([a-z0-9]+)" })
public class StaSh extends PluginForHost {
    public StaSh(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/";
    }

    /** Code is very similar to DeviantArtCom - keep it updated! */
    private static final String GENERALFILENAMEREGEX   = "(?:name|property)=\"og:title\"[^>]*content=\"([^<>\"]*?)\"";
    private final String        TYPE_HTML              = "class=\"text\">HTML download</span>";
    private boolean             HTMLALLOWED            = false;
    private final String        COOKIE_HOST            = "http://www.deviantart.com";
    private String              dllink                 = null;
    private final String        MATURECONTENTFILTER    = ">Mature Content Filter<";
    private final String        INVALIDLINKS           = "https?://[^/]+/(muro|writer|login)";
    private final String        TYPE_ZIP               = "https?://[^/]+/zip/[a-z0-9]+";
    private String              FORCEHTMLDOWNLOAD      = "FORCEHTMLDOWNLOAD";
    private String              USE_LINKID_AS_FILENAME = "USE_LINKID_AS_FILENAME";
    private String              DOWNLOAD_ZIP           = "DOWNLOAD_ZIP";

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
        if (!link.isNameSet()) {
            link.setName(this.getFID(link));
        }
        this.setBrowserExclusive();
        if (link.getPluginPatternMatcher().matches(INVALIDLINKS)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null;
        String ext = null;
        String filesize = null;
        br.setFollowRedirects(true);
        if (isZip(link)) {
            dllink = link.getStringProperty("directlink");
            ext = "zip";
        } else {
            br.getPage(link.getPluginPatternMatcher());
            ext = br.getRegex("<strong>\\s*Download Image\\s*</strong>\\s*<br><small>([A-Za-z0-9]{1,5}),").getMatch(0);
            if (br.containsHTML("/error\\-title\\-oops\\.png\\)") || br.containsHTML("The page you wanted to visit doesn't exist")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final boolean loggedIn = false;
            // Motionbooks are not supported (yet)
            if (br.containsHTML(",target: \\'motionbooks/")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex(GENERALFILENAMEREGEX).getMatch(0);
            if (this.getPluginConfig().getBooleanProperty(FORCEHTMLDOWNLOAD, false)) {
                HTMLALLOWED = true;
                dllink = br.getURL();
                filename = br._getURL().getPath();
                ext = "html";
            } else if (br.containsHTML("\"label\">\\s*Download\\s*<")) {
                // final Regex fInfo =
                // br.getRegex("<strong>Download File</strong><br/>[\t\n\r ]+<small>([A-Za-z0-9]{1,5}), ([^<>\"]*?)</small>");
                // ext = fInfo.getMatch(0);
                // filesize = fInfo.getMatch(1);
                ext = br.getRegex("(?i)class=\"label\">Download</span>\\s*<span class=\"text\">\\s*([A-Za-z]+)").getMatch(0);
                dllink = br.getRegex("\"(https?://[^/]+/_api/download/file[^\"]+)").getMatch(0);
                if (dllink == null) {
                    /* 2021-08-25: This may lead to a 404 while it works in browser?! */
                    dllink = br.getRegex("\"(https?://(?:www\\.)?sta\\.sh/download/[^<>\"]*?)\"").getMatch(0);
                }
                if (ext == null && dllink != null) {
                    ext = Plugin.getFileNameExtensionFromURL(dllink);
                }
            } else if (br.containsHTML(TYPE_HTML)) {
                HTMLALLOWED = true;
                filename = br._getURL().getPath();
                ext = "html";
            } else {
                filesize = br.getRegex("<label>\\s*Image Size:\\s*</label>([^<>\"]*?)<br>").getMatch(0);
                if (filesize == null) {
                    filesize = br.getRegex("<dt>\\s*Image Size\\s*</dt><dd>([^<>\"]*?)</dd>").getMatch(0);
                }
                /* Maybe its a video */
                if (filesize == null) {
                    filesize = br.getRegex("<label>\\s*File Size:\\s*</label>([^<>\"]*?)<br/>").getMatch(0);
                }
                if (br.containsHTML(MATURECONTENTFILTER) && !loggedIn) {
                    link.getLinkStatus().setStatusText("Mature content can only be downloaded via account");
                    link.setName(filename);
                    if (filesize != null) {
                        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
                    }
                    return AvailableStatus.TRUE;
                }
                /* Just download the html */
                if (ext == null) {
                    HTMLALLOWED = true;
                    ext = "html";
                    dllink = br.getURL();
                }
            }
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
        }
        if (dllink == null) {
            dllink = getDllink(this.br);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (dllink != null) {
            dllink = Encoding.htmlOnlyDecode(dllink);
            if (ext == null) {
                ext = Plugin.getFileNameExtensionFromURL(dllink);
            }
        }
        /* User wanted link-id as filename - at least when he added the link via crawler. */
        if (filename == null || this.getPluginConfig().getBooleanProperty(USE_LINKID_AS_FILENAME, false)) {
            filename = this.getFID(link);
        }
        if (filesize == null) {
            final Browser br2 = br.cloneBrowser();
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(dllink);
                if (!looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (con.getCompleteContentLength() > 0) {
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                }
                final String filenameFromContentDisposition = Plugin.getFileNameFromDispositionHeader(con);
                if (filenameFromContentDisposition != null) {
                    filename = filenameFromContentDisposition;
                    link.setFinalFileName(filenameFromContentDisposition);
                } else {
                    final String extFromMimetype = this.getExtensionFromMimeType(con);
                    if (extFromMimetype != null) {
                        ext = extFromMimetype;
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        if (filename != null) {
            link.setFinalFileName(Encoding.htmlDecode(filename).trim());
        }
        return AvailableStatus.TRUE;
    }

    public static String getDllink(final Browser br) throws PluginException {
        String dllink = null;
        // Check if it's a video
        dllink = br.getRegex("\"src\":\"(https?:[^<>\"]*?mp4)\"").getMatch(0);
        // First try to get downloadlink, if that doesn't exist, try to get the
        // link to the picture which is displayed in browser
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://(?:www\\.)?sta\\.sh/download/[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            if (br.containsHTML(">Mature Content</span>")) {
                dllink = br.getRegex("data\\-gmiclass=\"ResViewSizer_img\".*?src=\"(https?://[^<>\"]*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("<img collect_rid=\"\\d+:\\d+\" src=\"(https?://[^\"]+)").getMatch(0);
                }
            } else {
                dllink = br.getRegex("(name|property)=\"og:image\" content=\"(https?://[^<>\"]*?)\"").getMatch(1);
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = PluginJSonUtils.unescape(dllink);
        dllink = Encoding.htmlDecode(dllink);
        return dllink;
    }

    public static String getCrippledDllink(final Browser br) {
        String crippleddllink = null;
        try {
            final String linkWithExt = getDllink(br);
            final String toRemove = new Regex(linkWithExt, "(\\?token=.+)").getMatch(0);
            if (toRemove != null) {
                crippleddllink = linkWithExt.replace(toRemove, "");
            } else {
                crippleddllink = linkWithExt;
            }
        } catch (final Exception e) {
        }
        return crippleddllink;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        if (br.containsHTML(MATURECONTENTFILTER)) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "Mature content can only be downloaded via account");
        }
        if (dllink == null) {
            dllink = getDllink(this.br);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final boolean resume = !isZip(link);
        // Disable chunks as we only download pictures or small files
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    protected boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection) {
        final String contenttype = urlConnection.getContentType();
        if (HTMLALLOWED && StringUtils.containsIgnoreCase(contenttype, "html")) {
            return true;
        } else {
            return super.looksLikeDownloadableContent(urlConnection);
        }
    }

    @Override
    public String getDescription() {
        return "JDownloader's Sta.sh Plugin helps downloading data from sta.sh.";
    }

    public void setConfigElements() {
        String forcehtmldownloadtext;
        String set_id_as_filename_text;
        final String lang = System.getProperty("user.language");
        if ("de".equalsIgnoreCase(lang)) {
            forcehtmldownloadtext = "HTML Code statt dem eigentlichen Inhalt (Dateien/Bilder) laden?";
            set_id_as_filename_text = "Link-ID als Dateiname nutzen (sta.sh/LINKID)?";
        } else {
            forcehtmldownloadtext = "Download html code instead of the media (files/pictures)?";
            set_id_as_filename_text = "Use link-ID as filename (sta.sh/LINKID)?";
        }
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FORCEHTMLDOWNLOAD, JDL.L("plugins.hoster.StaSh.forceHTMLDownload", forcehtmldownloadtext)).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_LINKID_AS_FILENAME, JDL.L("plugins.hoster.StaSh.useLinkIDAsFilename", set_id_as_filename_text)).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DOWNLOAD_ZIP, JDL.L("plugins.hoster.StaSh.DownloadZip", "Download .zip file of all files in the folder?")).setDefaultValue(true));
    }

    private boolean isZip(final DownloadLink dl) {
        return dl.getBooleanProperty("iszip", false);
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}