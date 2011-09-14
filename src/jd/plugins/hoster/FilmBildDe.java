//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.util.Calendar;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "film.bild.de" }, urls = { "http://(www\\.)?film\\.bild\\.de/detail\\.aspx\\?s=detail&m=\\d+&pl=(m|t)(#s=movie&m=\\d+&pl=m)?" }, flags = { PluginWrapper.DEBUG_ONLY })
public class FilmBildDe extends PluginForHost {

    private String clipUrl = null;

    public FilmBildDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink downloadLink) {
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("#s=movie&m=\\d+&pl=m", "").replace("pl=t", "pl=m"));
    }

    @Override
    public String getAGBLink() {
        return "http://film.bild.de/agb.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String swfUrl = "http://film.bild.de/swf/BildTvMain.swf";

        if (clipUrl.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, clipUrl);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

            final String[][] uripart = new Regex(clipUrl, "(rtmp.*/ondemand)/(.*?)\\?(auth.*$)").getMatches();
            if (uripart == null || uripart.length != 1 || uripart.length == 1 && uripart[0].length != 3) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

            rtmp.setUrl(uripart[0][0] + "?ovpfv=2.1.5&" + uripart[0][2]);
            rtmp.setPlayPath(uripart[0][1] + "?" + uripart[0][2]);
            rtmp.setSwfVfy(swfUrl);
            rtmp.setResume(true);

            ((RTMPDownload) dl).startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());

        br.getHeaders().put("Referer", downloadLink.getDownloadURL());
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");

        br.getPage("http://film.bild.de/nav.aspx?s=movie&r=" + String.valueOf(Math.random()) + "&_=" + String.valueOf(System.currentTimeMillis()));
        String filename = br.getRegex("<div class=\"promoTitle\">(.*?)</div>").getMatch(0);
        final String next = br.getRegex("playMovie_URL_film\\(\'(.*)\',").getMatch(0);

        if (next != null && next.matches("http://film\\.bild\\.de:80/movie\' \\+ \\(new Date\\(\\)\\.getMilliseconds\\( ?\\)\\) \\+ Math\\.random\\(\\) \\+ \'\\.xml\\?s=xml\\d+")) {
            final Calendar cal = Calendar.getInstance();
            final String id = new Regex(next, "s=xml(\\d+)").getMatch(0);
            br.getPage("http://film.bild.de/movie" + String.valueOf(cal.get(Calendar.MILLISECOND)) + String.valueOf(Math.random()) + ".xml?s=xml" + id);
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (filename == null) {
            filename = br.getRegex("<title\">(.*?)</title>").getMatch(0);
        }
        clipUrl = br.getRegex("<enclosure url=\"(.*?)\"").getMatch(0);
        if (filename == null || clipUrl == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        clipUrl = Encoding.htmlDecode(clipUrl);
        downloadLink.setFinalFileName(filename.trim() + ".flv");
        return AvailableStatus.TRUE;
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
