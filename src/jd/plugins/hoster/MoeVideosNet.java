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

import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "moevideos.net" }, urls = { "http://moevideosdecrypted\\.net/\\d+" }, flags = { 0 })
public class MoeVideosNet extends PluginForHost {

    private String               DLLINK;
    private static AtomicBoolean isDled = new AtomicBoolean(false);

    public MoeVideosNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.moevideos.net/tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private String               uid            = null;
    private static final boolean DECRYPTER_ONLY = true;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "([0-9a-f\\.]+)$").getMatch(0) + ".flv");
        if (DECRYPTER_ONLY) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        setBrowserExclusive();
        br.setFollowRedirects(true);
        String dllink = downloadLink.getDownloadURL();
        /* uid */
        uid = new Regex(dllink, "uid=(.*?)$").getMatch(0);
        if (uid == null) uid = new Regex(dllink, "(video/|file=)(.*?)$").getMatch(1);
        if (uid == null) {
            br.getPage(dllink);
            if (br.containsHTML("Vídeo no existe posiblemente")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        }
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (uid == null) {
            final Form iAmHuman = br.getFormbyProperty("name", "formulario");
            if (iAmHuman == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            iAmHuman.remove("enviar");

            /* Waittime is still skippable */
            // String waittime = br.getRegex("var tiempo = (\\d+);").getMatch(0);
            // int wait = 5;
            // if (waittime != null) wait = Integer.parseInt(waittime);
            // sleep(wait * 1001l, downloadLink);

            br.submitForm(iAmHuman);
            uid = br.getRegex("file=([0-9a-f\\.]+)(\\&|\"|\\')").getMatch(0);
        }
        /* finallink */
        br.postPage("http://api.letitbit.net/", "r=[\"tVL0gjqo5\",[\"preview/flv_image\",{\"uid\":\"" + uid + "\"}],[\"preview/flv_link\",{\"uid\":\"" + uid + "\"}]]");
        final String fsize = br.getRegex("\"convert_size\":\"(\\d+)\"").getMatch(0);
        if (fsize != null) downloadLink.setDownloadSize(Long.parseLong(fsize));

        boolean status = br.getRegex("\"status\":\"OK\"").matches() ? true : false;

        DLLINK = br.getRegex("\"link\":\"([^\"]+)").getMatch(0);
        if (DLLINK == null) {
            if (status) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        DLLINK = DLLINK.replaceAll("\\\\", "");
        /* filename */
        String filename = new Regex(DLLINK, "/[0-9a-f]+_\\d+_(.*?)\\.flv").getMatch(0);
        filename = filename != null ? filename : "unknown_filename" + uid + ".flv";
        downloadLink.setFinalFileName(filename);

        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 401) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 401", 30 * 60 * 1000l);
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            isDled.set(true);
            dl.startDownload();
        } finally {
            isDled.set(false);
        }
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