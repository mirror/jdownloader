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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bbc.com" }, urls = { "http://bbcdecrypted/[pb][a-z0-9]{7}" })
public class BbcCom extends PluginForHost {

    public BbcCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.bbc.co.uk/terms/";
    }

    private String rtmp_host       = null;
    private String rtmp_app        = null;
    private String rtmp_playpath   = null;
    private String rtmp_authString = null;

    private String hls_master      = null;

    /** Thanks goes to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/bbc.py */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String vpid = new Regex(link.getDownloadURL(), "bbcdecrypted/(.+)").getMatch(0);
        String title = link.getStringProperty("decrypterfilename");
        /* HLS - try that first as it will give us higher bitrates */
        this.br.getPage("http://open.live.bbc.co.uk/mediaselector/5/select/version/2.0/mediaset/iptv-all/vpid/" + vpid);
        if (!this.br.getHttpConnection().isOK()) {
            /* RTMP #1 */
            /* 403 or 404 == geoblocked|offline|needsRTMP */
            /* Fallback e.g. vpids: p01dvmbh, b06s1fj9 */
            /* Possible "device" strings: "pc", "iptv-all", "journalism-pc" */
            this.br.getPage("http://open.live.bbc.co.uk/mediaselector/5/select/version/2.0/mediaset/pc/vpid/" + vpid);
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title_downloadurl = null;
        String transferformat = null;
        String filesize_str = null;
        long filesize_max = 0;
        long filesize_temp = 0;
        /* Find BEST possible quality throughout different streaming protocols. */
        final String media[] = this.br.getRegex("<media(.*?)</media>").getColumn(0);
        for (final String mediasingle : media) {
            final String[] connections = new Regex(mediasingle, "(<connection.*?)/>").getColumn(0);
            if (connections == null || connections.length == 0) {
                /* Whatever - skip such a case */
                continue;
            }
            /* Every protocol can have multiple 'mirrors' or even sub-protocols (http --> dash, hls, hds, directhttp) */
            for (final String connection : connections) {
                transferformat = new Regex(connection, "transferFormat=\"([^<>\"]+)\"").getMatch(0);
                if (transferformat != null && transferformat.matches("hds|dash")) {
                    /* Skip unsupported protocols */
                    continue;
                }
                filesize_str = new Regex(mediasingle, "media_file_size=\"(\\d+)\"").getMatch(0);
                /* Do not RegEx again if we already have our hls_master */
                if (hls_master == null) {
                    hls_master = new Regex(connection, "\"(https?://[^<>\"]+\\.m3u8[^<>\"]*?)\"").getMatch(0);
                }
                /* Do not RegEx again if we already have our rtmp parameters */
                if (rtmp_app == null && rtmp_host == null && rtmp_playpath == null && rtmp_authString == null) {
                    rtmp_app = new Regex(connection, "application=\"([^<>\"]+)\"").getMatch(0);
                    rtmp_host = new Regex(connection, "server=\"([^<>\"]+)\"").getMatch(0);
                    rtmp_playpath = new Regex(connection, "identifier=\"((?:mp4|flv):[^<>\"]+)\"").getMatch(0);
                    rtmp_authString = new Regex(connection, "authString=\"([^<>\"]*?)\"").getMatch(0);
                }
            }
            if (filesize_str == null) {
                /* No filesize given? Skip this media! */
                continue;
            }
            filesize_temp = Long.parseLong(filesize_str);
            if (filesize_temp > filesize_max) {
                filesize_max = filesize_temp;
            }
        }

        if (rtmp_playpath != null) {
            title_downloadurl = new Regex(rtmp_playpath, "([^<>\"/]+)\\.mp4").getMatch(0);
        }
        if (title == null) {
            title = title_downloadurl;
        }
        if (title == null) {
            /* Finally, fallback to vpid as filename */
            title = vpid;
        }

        link.setFinalFileName(title + ".mp4");
        if (filesize_max > 0) {
            link.setDownloadSize(filesize_max);
        }

        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (hls_master == null && (this.rtmp_app == null || this.rtmp_host == null || this.rtmp_playpath == null) && this.br.getHttpConnection().getResponseCode() == 403) {
            /*
             * 2017-03-24: Example html in this case: <?xml version="1.0" encoding="UTF-8"?><mediaSelection
             * xmlns="http://bbc.co.uk/2008/mp/mediaselection"><error id="geolocation"/></mediaSelection>
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "GEO-Blocked");
        }
        if (hls_master != null) {
            hls_master = Encoding.htmlDecode(hls_master);
            br.getPage(hls_master);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, url_hls);
            dl.startDownload();
        } else {
            if (this.rtmp_app == null || this.rtmp_host == null || this.rtmp_playpath == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String rtmpurl = "rtmp://" + this.rtmp_host + "/" + this.rtmp_app;
            /* authString is needed in some cases */
            if (this.rtmp_authString != null) {
                this.rtmp_authString = Encoding.htmlDecode(this.rtmp_authString);
                rtmpurl += "?" + this.rtmp_authString;
                /* 2016-05-31: (Sometimes) needed for app "ondemand" and "a5999/e1" */
                rtmp_app += "?" + this.rtmp_authString;
            }
            try {
                dl = new RTMPDownload(this, downloadLink, rtmpurl);
            } catch (final NoClassDefFoundError e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
            }
            /* Setup rtmp connection */
            jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setSwfUrl("http://emp.bbci.co.uk/emp/SMPf/1.16.6/StandardMediaPlayerChromelessFlash.swf");
            /* 2016-05-31: tcUrl is very important for some urls e.g. http://www.bbc.co.uk/programmes/b01s5cdn */
            rtmp.setTcUrl(rtmpurl);
            rtmp.setUrl(rtmpurl);
            rtmp.setPageUrl(downloadLink.getDownloadURL());
            rtmp.setPlayPath(this.rtmp_playpath);
            rtmp.setApp(this.rtmp_app);
            /* Last update: 2016-05-31 */
            rtmp.setFlashVer("WIN 21,0,0,242");
            rtmp.setResume(false);
            ((RTMPDownload) dl).startDownload();
        }
    }

    // @SuppressWarnings({ "static-access" })
    // private String formatDate(String input) {
    // final long date;
    // if (input.matches("\\d+")) {
    // date = Long.parseLong(input) * 1000;
    // } else {
    // final Calendar cal = Calendar.getInstance();
    // input += cal.get(cal.YEAR);
    // date = TimeFormatter.getMilliSeconds(input, "E '|' dd.MM.yyyy", Locale.GERMAN);
    // }
    // String formattedDate = null;
    // final String targetFormat = "yyyy-MM-dd";
    // Date theDate = new Date(date);
    // try {
    // final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
    // formattedDate = formatter.format(theDate);
    // } catch (Exception e) {
    // /* prevent input error killing plugin */
    // formattedDate = input;
    // }
    // return formattedDate;
    // }

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