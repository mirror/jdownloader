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

/**
 * @author noone2407
 */
package jd.plugins.hoster;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 1 $", interfaceVersion = 1, names = { "tv.zing.vn" }, urls = { "http://tv.zing.vn/video/(\\S+).html" }, flags = { 2 })
public class ZingTv extends PluginForHost {

    private String dllink = null;

    public ZingTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://mp3.zing.vn/huong-dan/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        String url = downloadLink.getDownloadURL();
        br.setDebug(true);
        br.setFollowRedirects(true);
        // get the name of the movie from url
        String filename = "video";
        Pattern pattern = Pattern.compile("http://tv.zing.vn/video/([\\w-]+)\\/([\\w\\d]+).html");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            filename = matcher.group(1);
        }
        downloadLink.setName(filename);
        downloadLink.setFinalFileName(filename + ".mp4");
        downloadLink.setHost("tv.zing.vn");
        // get the list of urls
        br.getPage("http://getlinkfs.com/getfile/zingtv.php?link=" + url);
        List<String> allMatches = new ArrayList<String>();
        Matcher m = br.getRegex("\"http://stream(.*?)\"").getMatcher();
        while (m.find()) {
            if (!allMatches.contains(m.group())) {
                allMatches.add(m.group());
            }
        }
        // get the url of best quality video
        String link = null;
        for (final String s : allMatches) {
            link = s.replace("\"", "");
        }
        dllink = link;
        int filesize = getFileSize(new URL(link));
        downloadLink.setDownloadSize(filesize);

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink downloadLink) {
    }

    private int getFileSize(URL url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            return -1;
        } finally {
            conn.disconnect();
        }
    }
}
