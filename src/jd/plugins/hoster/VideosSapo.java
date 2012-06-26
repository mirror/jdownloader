//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.text.SimpleDateFormat;
import java.util.Date;

import jd.PluginWrapper;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videos.sapo.pt" }, urls = { "http://(\\w+\\.)?videos\\.sapo\\.(pt|cv|ao|mz|tl)/\\w{20}" }, flags = { 0 })
public class VideosSapo extends PluginForHost {

    private String DLLINK = null;

    public VideosSapo(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://seguranca.sapo.pt/termosdeutilizacao/videos.html";
    }

    private void getDownloadlink(final String dlurl) throws IOException {
        String playLink = br.getRegex("\\'(http://rd\\d+\\.videos\\.sapo\\.(pt|cv|ao|mz|tl)/[A-Za-z0-9]+/mov/\\d+)\\'").getMatch(0);
        if (playLink == null) {
            playLink = br.getRegex("videoVerifyMrec\\(\"(http://[^<>\"]+)").getMatch(0);
        }
        br.getPage(dlurl + "/rss2?hide_comments=true");
        String time = br.getRegex("<lastBuildDate>(.*?)</lastBuildDate>").getMatch(0);
        if (playLink == null || time == null) { return; }

        final SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
        int serverTimeDif;
        try {
            final Date date = df.parse(time);
            serverTimeDif = (int) (Math.floor(date.getTime() / 1000) - Math.floor(System.currentTimeMillis() / 1000));
        } catch (final Throwable e) {
            return;
        }
        time = Integer.toString((int) Math.floor(System.currentTimeMillis() / 1000) + serverTimeDif);
        DLLINK = playLink + "?player=INTERNO&time=" + time + "&token=" + JDHash.getMD5(Encoding.Base64Decode("c3ZlOWYjNzNz") + dlurl.substring(dlurl.lastIndexOf("/") + 1) + time);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        final String dlurl = downloadLink.getDownloadURL();
        br.getPage(dlurl);
        String filename = br.getRegex("<div class=\"titvideo\">([^<>\"]*?)</div>").getMatch(0);
        if (filename == null) filename = br.getRegex("<h1 class=\"wid_content\\-title\">([^<>\"]*?)</h1>").getMatch(0);
        getDownloadlink(dlurl);
        if (filename == null || DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        downloadLink.setFinalFileName(filename.trim() + ".mp4");
        try {
            if (!br.openGetConnection(DLLINK).getContentType().contains("html")) {
                downloadLink.setDownloadSize(br.getHttpConnection().getLongContentLength());
                return AvailableStatus.TRUE;
            }
        } finally {
            try {
                if (br.getHttpConnection() != null) {
                    br.getHttpConnection().disconnect();
                }
            } catch (final Throwable e) {
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}