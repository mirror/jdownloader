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

import jd.PluginWrapper;
import jd.config.Property;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "sourceforge.net" }, urls = { "https?://(www\\.)?sourceforgedecrypted\\.net/.+" })
public class SourceForgeNet extends PluginForHost {
    public SourceForgeNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("sourceforgedecrypted.net/", "sourceforge.net/"));
    }

    /* DEV NOTES */
    // other:
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://slashdotmedia.com/terms-of-use/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        URLConnectionAdapter con = null;
        dllink = checkDirectLink(downloadLink, "directlink");
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (dllink == null) {
            br.getPage(downloadLink.getDownloadURL());
            if (br.containsHTML("(Error 404|The page you were looking for cannot be found|could not be found or is not available)") || br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String altDlink = br.getRegex("<b>Download</b>[\t\n\r ]+<small title=\"(/[^<>\"]*?)\"").getMatch(0);
            String link = null;
            if (br.getURL().contains("/files/extras/") || br.getURL().contains("prdownloads.sourceforge.net") || br.getURL().contains("/download")) {
                link = getDllink(this.br);
            } else {
                String project = new Regex(downloadLink.getDownloadURL(), "sourceforge\\.net/projects/(.*?)/").getMatch(0);
                if (project == null) {
                    project = new Regex(br.getURL(), "sourceforge\\.net/projects/(.*?)/").getMatch(0);
                }
                if (altDlink != null) {
                    // Avoid ad-installers, see here: http://userscripts.org/scripts/show/174951
                    link = "http://master.dl.sourceforge.net/project/" + project + altDlink;
                } else {
                    final String continuelink = br.getRegex("\"(/projects/" + project + "/files/latest/download[^<>\"/]*?)\"").getMatch(0);
                    if (continuelink == null) {
                        logger.info("Found no downloadable link for: " + downloadLink.getDownloadURL());
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    br.getPage(continuelink);
                    /* In very rare cases, files are not downloadable. */
                    if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains("/download") || br.containsHTML("(<h1>Error encountered</h1>|>We apologize\\. It appears an error has occurred\\.)")) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    link = new Regex(Encoding.htmlDecode(br.toString()), "Please use this([\t\n\r ]+)?<a href=\"(https?://.*?)\"").getMatch(1);
                }
            }
            if (link == null) {
                logger.warning("Decrypter broken, link: " + downloadLink.getDownloadURL());
                return null;
            }
            link = Encoding.htmlDecode(link);
            final String urlPart = new Regex(link, "(https?://downloads\\.sourceforge\\.net/project/.*?)(https?://sourceforge\\.net/|\\?r=)").getMatch(0);
            final String secondUrlPart = new Regex(link, "(\\&ts=\\d+\\&use_mirror=.+)").getMatch(0);
            /* Either we already got the final link or we have to build it */
            if (urlPart != null && secondUrlPart != null) {
                link = urlPart + "?r=" + secondUrlPart;
            }
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(false);
            String finallink = null;
            try {
                for (int i = 0; i <= 5; i++) {
                    if (i == 0) {
                        finallink = link;
                    } else if (brc.getRedirectLocation() != null) {
                        finallink = brc.getRedirectLocation();
                    } else {
                        finallink = getDllink(brc);
                    }
                    if (finallink == null) {
                        return null;
                    }
                    con = brc.openHeadConnection(finallink);
                    if (con.getContentType().contains("html")) {
                        logger.info("finallink is no file, continuing...");
                        brc.followConnection();
                        continue;
                    } else if (con.getResponseCode() == 200) {
                        dllink = finallink;
                        downloadLink.setDownloadSize(con.getLongContentLength());
                        break;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
            if (dllink == null) {
                logger.warning("The finallink is no file!!");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setProperty("finallink", dllink);
        }
        return AvailableStatus.TRUE;
    }

    public static String getDllink(final Browser br) {
        String link = br.getRegex("Please use this([\n\t\r ]+)?<a href=\"(.*?)\"").getMatch(1);
        if (link == null) {
            link = br.getRegex("\"(https?://downloads\\.sourceforge\\.net/project/.*?/.*?use_mirror=.*?)\"").getMatch(0);
        }
        if (link == null) {
            /* 2017-02-02: E.g. html "This file may contain malware and the automatic download has been disabled" */
            link = br.getRegex("<a href=\"(https?://downloads\\.sourceforge\\.net/[^<>\"]+)\" class=\"direct\\-download\"").getMatch(0);
        }
        return link;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
