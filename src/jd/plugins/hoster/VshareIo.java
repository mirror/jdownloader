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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vshare.io" }, urls = { "https?://(?:www\\.)?vshare\\.io/(?:d|v)/[a-z0-9]+" })
public class VshareIo extends PluginForHost {
    public VshareIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("/v/", "/d/"));
    }

    @Override
    public String getAGBLink() {
        return "http://vshare.io/TOS.php";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = false;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 20;

    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    //
    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        correctDownloadLink(link);
        br.setFollowRedirects(true);
        String fid = getFID(link);
        link.setLinkID(fid);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("error=404") || br.getURL().contains("/404/") || br.containsHTML(">We are sorry,")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex finfo = br.getRegex("<p>([^<>\"]+) - (\\d{1,4}(?:\\.\\d{1,2})? [A-Za-z]{1,5})</p>");
        String filename = finfo.getMatch(0);
        String filesize = finfo.getMatch(1);
        if (filename == null) {
            filename = br.getRegex(">\\s*([^<>]+)<br/>\\s*<iframe").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("div id=\"404\".*?>\\s*(.*?)\\s*<").getMatch(0);
                if (filename != null) {
                    String dllink = br.getRegex("style=\"text-decoration:none;\" href=\"(https?[^<>\"]+)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("\"(https?://s\\d+\\.vshare\\.io/[^<>\"]+)\"").getMatch(0);
                        if (dllink == null) {
                            br.getPage(String.format("/v/%s/width-650/height-430/1", fid));
                            dllink = getDllink();
                        }
                    }
                    if (dllink != null) {
                        final String ext = getFileNameExtensionFromURL(dllink);
                        if (ext != null) {
                            filename = filename + ext;
                        }
                    }
                }
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        link.setName(filename);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            br.getPage(String.format("/v/%s/width-650/height-430/1", getFID(downloadLink)));
            dllink = getDllink();
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private String getDllink() {
        String js = br.getRegex("(eval.*?\\)\\s)").getMatch(0);
        StringBuffer sb = new StringBuffer();
        sb.append("function El(){}function jQuery(n,r){return new El}var result='';El.prototype.append=function(n){result+=n};var $=jQuery;");
        sb.append(js);
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        String result = null;
        try {
            engine.eval(sb.toString());
            result = engine.get("result").toString();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        // result=<source
        // src="https://s108.vshare.io/s,110-1000-1-0-0/186277/333009/184864/ff-fa905e0e5055835ada10478afd25e691,5a0e70b1,262b501_720.mp4"
        // type="video/mp4" label="720p" res="720"><source
        // src="https://s108.vshare.io/s,110-1000-1-0-0/186277/333009/184864/ff-fa905e0e5055835ada10478afd25e691,5a0e70b1,262b501_480.mp4"
        // type="video/mp4" label="480p" res="480"><source
        // src="https://s108.vshare.io/s,110-1000-1-0-0/186277/333009/184864/ff-fa905e0e5055835ada10478afd25e691,5a0e70b1,262b501_360.mp4"
        // type="video/mp4" label="360p" res="360"><source
        // src="https://s108.vshare.io/s,110-1000-1-0-0/186277/333009/184864/ff-fa905e0e5055835ada10478afd25e691,5a0e70b1,262b501_240.mp4"
        // type="video/mp4" label="240p" res="240">
        return new Regex(result, "src=\"([^\"]+)").getMatch(0);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
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
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
