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
        /* 2020-07-08: Required to reduce error-responses due to opening too many connections in a short time. */
        this.setStartIntervall(2000l);
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("sourceforgedecrypted.net/", "sourceforge.net/"));
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
        return "https://slashdotmedia.com/terms-of-use/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException, InterruptedException {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException, InterruptedException {
        /* 2020-07-08: Don't do that! */
        // final String urlname = new Regex(link.getPluginPatternMatcher(), "/projects/(.*?)/?$").getMatch(0);
        // if (!link.isNameSet() && urlname != null) {
        // link.setName(urlname);
        // }
        URLConnectionAdapter con = null;
        dllink = checkDirectLink(link, "directlink");
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (dllink == null) {
            br.getPage(link.getPluginPatternMatcher());
            if (br.containsHTML("(Error 404|The page you were looking for cannot be found|could not be found or is not available)") || br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String altDlink = br.getRegex("<b>Download</b>\\s*<small title=\"(/[^<>\"]*?)\"").getMatch(0);
            String dlurl = null;
            if (br.getURL().contains("/files/extras/") || br.getURL().contains("prdownloads.sourceforge.net") || br.getURL().contains("/download")) {
                dlurl = getDllink(this.br);
            } else {
                String project = new Regex(link.getPluginPatternMatcher(), "sourceforge\\.net/projects/(.*?)/").getMatch(0);
                if (project == null) {
                    project = new Regex(br.getURL(), "sourceforge\\.net/projects/(.*?)/").getMatch(0);
                }
                if (altDlink != null) {
                    // Avoid ad-installers, see here: http://userscripts.org/scripts/show/174951
                    dlurl = "http://master.dl.sourceforge.net/project/" + project + altDlink;
                } else {
                    final String continuelink = br.getRegex("\"(/projects/" + project + "/files/latest/download[^<>\"/]*?)\"").getMatch(0);
                    if (continuelink == null) {
                        logger.info("Found no downloadable link for: " + link.getPluginPatternMatcher());
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    br.getPage(continuelink);
                    /* In very rare cases, files are not downloadable. */
                    if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains("/download") || br.containsHTML("(<h1>Error encountered</h1>|>We apologize\\. It appears an error has occurred\\.)")) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    dlurl = new Regex(Encoding.htmlDecode(br.toString()), "Please use this([\t\n\r ]+)?<a href=\"(https?://.*?)\"").getMatch(1);
                }
            }
            if (dlurl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (isDownload) {
                dlurl = Encoding.htmlDecode(dlurl);
                final String urlPart = new Regex(dlurl, "(https?://downloads\\.sourceforge\\.net/project/.*?)(https?://sourceforge\\.net/|\\?r=)").getMatch(0);
                final String secondUrlPart = new Regex(dlurl, "(\\&ts=\\d+\\&use_mirror=.+)").getMatch(0);
                /* Either we already got the final link or we have to build it */
                if (urlPart != null && secondUrlPart != null) {
                    dlurl = urlPart + "?r=" + secondUrlPart;
                }
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(false);
                String finallink = null;
                try {
                    for (int i = 0; i <= 5; i++) {
                        if (i == 0) {
                            /* 2020-07-08: Hardcoded pre-download-waittime */
                            this.sleep(5 * 1001l, link);
                            finallink = dlurl;
                        } else if (brc.getRedirectLocation() != null) {
                            finallink = brc.getRedirectLocation();
                        } else {
                            finallink = getDllink(brc);
                        }
                        if (finallink == null) {
                            return null;
                        }
                        con = brc.openHeadConnection(finallink);
                        if (con.getContentType().contains("text")) {
                            logger.info("finallink is no file, continuing...");
                            try {
                                brc.followConnection(true);
                            } catch (IOException e) {
                                logger.log(e);
                            }
                            continue;
                        } else if (con.getResponseCode() == 200) {
                            dllink = finallink;
                            link.setDownloadSize(con.getCompleteContentLength());
                            break;
                        } else {
                            try {
                                brc.followConnection(true);
                            } catch (IOException e) {
                                logger.log(e);
                            }
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
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find any usable mirror", 1 * 60 * 1000l);
                }
                link.setProperty("finallink", dllink);
            }
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
    public void handleFree(final DownloadLink link) throws Exception {
        this.dllink = checkDirectLink(link, "finallink");
        if (this.dllink == null) {
            requestFileInformation(link, true);
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find any usable mirror", 5 * 60 * 1000l);
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!dl.getConnection().isOK() || dl.getConnection().getContentType().contains("text")) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        final String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getResponseCode() != 200 || con.getContentType().contains("text") || con.getCompleteContentLength() == -1) {
                    throw new IOException();
                } else {
                    return dllink;
                }
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return null;
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
