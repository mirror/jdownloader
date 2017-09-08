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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sta.sh" }, urls = { "https?://(www\\.)?stadecrypted\\.sh/(zip/)?[a-z0-9]+" })
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
    private static final String GENERALFILENAMEREGEX   = "name=\"og:title\" content=\"([^<>\"]*?)\"";
    private static final String TYPE_HTML              = "class=\"text\">HTML download</span>";
    private boolean             HTMLALLOWED            = false;
    private final String        COOKIE_HOST            = "http://www.deviantart.com";
    private String              DLLINK                 = null;
    private final String        MATURECONTENTFILTER    = ">Mature Content Filter<";
    private final String        INVALIDLINKS           = "http://(www\\.)?sta\\.sh/(muro|writer|login)";
    private final String        TYPE_ZIP               = "http://(www\\.)?sta\\.sh/zip/[a-z0-9]+";
    private static String       FORCEHTMLDOWNLOAD      = "FORCEHTMLDOWNLOAD";
    private static String       USE_LINKID_AS_FILENAME = "USE_LINKID_AS_FILENAME";
    private static String       DOWNLOAD_ZIP           = "DOWNLOAD_ZIP";

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("stadecrypted.sh/", "sta.sh/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        if (link.getDownloadURL().matches(INVALIDLINKS)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null;
        String ext = null;
        String filesize = null;
        br.setFollowRedirects(true);
        try {
            br.getPage(link.getDownloadURL());
        } catch (final IllegalStateException e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("/error\\-title\\-oops\\.png\\)") || br.containsHTML("The page you wanted to visit doesn't exist")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final boolean loggedIn = false;
        // Motionbooks are not supported (yet)
        if (br.containsHTML(",target: \\'motionbooks/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filename = br.getRegex(GENERALFILENAMEREGEX).getMatch(0);
        if (filename == null) {
            filename = new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
        }
        filename = Encoding.htmlDecode(filename.trim());
        if (this.getPluginConfig().getBooleanProperty(FORCEHTMLDOWNLOAD, false)) {
            HTMLALLOWED = true;
            DLLINK = br.getURL();
            filename = findServerFilename(this.br, filename);
            ext = "html";
        } else if (isZip(link)) {
            /* Special case, dllink is already set via decrypter via property */
            DLLINK = link.getStringProperty("directlink", null);
            ext = "zip";
        } else if (br.containsHTML("\"label\">Download<")) {
            // final Regex fInfo =
            // br.getRegex("<strong>Download File</strong><br/>[\t\n\r ]+<small>([A-Za-z0-9]{1,5}), ([^<>\"]*?)</small>");
            // ext = fInfo.getMatch(0);
            // filesize = fInfo.getMatch(1);
            DLLINK = br.getRegex("\"(https?://(www\\.)?sta\\.sh/download/[^<>\"]*?)\"").getMatch(0);
            if (DLLINK != null) {
                ext = new Regex(DLLINK, "\\.(jpg|png|gif|pdf|swf|zip)").getMatch(0);
            }
            if (DLLINK == null || ext == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            DLLINK = Encoding.htmlDecode(DLLINK.trim());
        } else if (br.containsHTML(TYPE_HTML)) {
            HTMLALLOWED = true;
            filename = findServerFilename(this.br, filename);
            ext = "html";
        } else {
            filesize = br.getRegex("<label>Image Size:</label>([^<>\"]*?)<br>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("<dt>Image Size</dt><dd>([^<>\"]*?)</dd>").getMatch(0);
            }
            // Maybe its a video
            if (filesize == null) {
                filesize = br.getRegex("<label>File Size:</label>([^<>\"]*?)<br/>").getMatch(0);
            }
            if (br.containsHTML(MATURECONTENTFILTER) && !loggedIn) {
                link.getLinkStatus().setStatusText("Mature content can only be downloaded via account");
                link.setName(filename);
                if (filesize != null) {
                    link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
                }
                return AvailableStatus.TRUE;
            }
            filename = findServerFilename(this.br, filename);
            if (ext == null || ext.length() > 5) {
                ext = getFileExt(this.br);
            }
            /* Just download the html */
            if (ext == null) {
                HTMLALLOWED = true;
                ext = "html";
                DLLINK = br.getURL();
            }
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
        } else {
            final Browser br2 = br.cloneBrowser();
            URLConnectionAdapter con = null;
            try {
                if (DLLINK == null) {
                    DLLINK = getDllink(this.br);
                }
                con = br2.openGetConnection(DLLINK);
                if (con.getContentType().contains("html") && !HTMLALLOWED) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    link.setDownloadSize(con.getLongContentLength());
                    if (isZip(link)) {
                        filename = getFileNameFromHeader(con);
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        ext = ext.toLowerCase();
        /* User wanted link-id as filename - at least when he added the link via decrypter. */
        if (this.getPluginConfig().getBooleanProperty(USE_LINKID_AS_FILENAME, false)) {
            filename = new Regex(link.getDownloadURL(), "sta\\.sh/(.+)").getMatch(0);
        }
        if (!filename.endsWith(ext)) {
            filename += "." + ext.trim();
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    public static String getFileExt(final Browser br) {
        String filename = br.getRegex(GENERALFILENAMEREGEX).getMatch(0);
        String ext = br.getRegex("<strong>Download Image</strong><br><small>([A-Za-z0-9]{1,5}),").getMatch(0);
        if (ext == null && filename != null) {
            ext = new Regex(filename, "\\.([A-Za-z0-9]{1,5})$").getMatch(0);
        }
        filename = findServerFilename(br, filename);
        if (ext == null || ext.length() > 5) {
            final String dllink = getCrippledDllink(br);
            if (dllink != null) {
                ext = dllink.substring(dllink.lastIndexOf(".") + 1);
            }
        }
        return ext;
    }

    public static String getDllink(final Browser br) throws PluginException {
        String dllink = null;
        // Check if it's a video
        dllink = br.getRegex("\"src\":\"(http:[^<>\"]*?mp4)\"").getMatch(0);
        // First try to get downloadlink, if that doesn't exist, try to get the
        // link to the picture which is displayed in browser
        if (dllink == null) {
            dllink = br.getRegex("\"(http://(www\\.)?sta\\.sh/download/[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            if (br.containsHTML(">Mature Content</span>")) {
                dllink = br.getRegex("data\\-gmiclass=\"ResViewSizer_img\".*?src=\"(http://[^<>\"]*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("<img collect_rid=\"\\d+:\\d+\" src=\"(https?://[^\"]+)").getMatch(0);
                }
            } else {
                dllink = br.getRegex("(name|property)=\"og:image\" content=\"(http://[^<>\"]*?)\"").getMatch(1);
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
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

    public static String findServerFilename(final Browser br, final String oldfilename) {
        // Try to get server filename, if not possible, return old one
        String newfilename = null;
        final String dllink = getCrippledDllink(br);
        if (dllink != null) {
            newfilename = new Regex(dllink, "/([^<>\"/]+)$").getMatch(0);
        } else {
            newfilename = oldfilename;
        }
        return newfilename;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
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
        if (DLLINK == null) {
            DLLINK = getDllink(this.br);
        }
        boolean resume = true;
        if (isZip(downloadLink)) {
            resume = false;
        }
        // Disable chunks as we only download pictures or small files
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, resume, 1);
        if (dl.getConnection().getContentType().contains("html") && !this.HTMLALLOWED) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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