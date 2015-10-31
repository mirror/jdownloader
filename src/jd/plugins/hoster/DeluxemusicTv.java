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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.TimeFormatter;

/*Similar websites: bca-onlive.de, asscompact.de*/
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deluxemusic.tv" }, urls = { "http://deluxemusic\\.tvdecrypted/\\d+_\\d+" }, flags = { 0 })
public class DeluxemusicTv extends PluginForHost {

    public DeluxemusicTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.asscompact.de/node/95";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("asscompactdecrypted.de/", "asscompact.de/"));
    }

    private String xml_source = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String playlist_url = link.getStringProperty("playlist_url", null);
        if (playlist_url == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.br.getPage(playlist_url);
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String[] xml_array = jd.plugins.decrypter.DeluxemusicTv.getTrackArray(this.br);
        this.xml_source = xml_array[getArrayid(link)];
        return parseTrackInfo(link, this.br.toString(), xml_array);
    }

    public static AvailableStatus parseTrackInfo(final DownloadLink link, final String xml_all, final String[] xml_array) throws IOException, PluginException {
        final String playlist_id = getPlaylistid(link);
        final String xml_source = xml_array[getArrayid(link)];

        String title = getXML(xml_source, "title");
        String date = new Regex(xml_all, "Die Sets vom (\\d{1,2}\\. [A-Za-z]+)").getMatch(0);
        if (date == null) {
            date = new Regex(xml_source, "UPDATE DELUXE (\\d{4} \\d{1,2} \\d{1,2})").getMatch(0);
        }
        if (title == null) {
            link.setAvailable(false);
            return AvailableStatus.FALSE;
        }
        if (date == null) {
            /* Final attempt to find a date */
            date = new Regex(title, "(\\d{4} \\d{1,2} \\d{1,2})").getMatch(0);
        }
        String filename = "";

        if (date != null) {
            /* Remove date from title - we don't need it twice! */
            title = title.replace(" " + date, "");

            final String date_formatted = formatDate(date);
            filename = date_formatted + "_";
        }

        filename += "deluxemusictv_playlist_" + playlist_id + "_";
        filename += title + ".mp4";

        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicode(filename);

        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    public static boolean isOffline(final Browser br) throws IOException {
        if (br.getHttpConnection().getResponseCode() == 404 || br.toString().length() < 200) {
            return true;
        }
        return false;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);

        final String streamer = new Regex(xml_source, "rel=\"streamer\">(rtmp://[^<>\"]*?)</meta>").getMatch(0);
        final String location = getXML(xml_source, "location");

        if (streamer == null || location == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        final String url_playpath = "mp4:" + location;

        final String rtmp_app = "deluxemusic.tv/_definst_/";
        final String url_rtmp = streamer;
        dl = new RTMPDownload(this, downloadLink, url_rtmp);
        final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

        rtmp.setPlayPath(url_playpath);
        rtmp.setPageUrl(this.br.getURL());
        rtmp.setSwfVfy("http://static.deluxemusic.tv.dl1.ipercast.net/theme/deluxemusic.tv/flash/player.swf");
        rtmp.setFlashVer("WIN 16,0,0,305");
        rtmp.setApp(rtmp_app);
        rtmp.setUrl(url_rtmp);
        rtmp.setResume(false);
        dl.startDownload();
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    public static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    public static String formatDate(String input) {
        final String source_format;
        if (input.matches("\\d{4} \\d{1,2} \\d{1,2}")) {
            source_format = "yyyy MM dd";
        } else {
            final Calendar now = Calendar.getInstance();
            final int year = now.get(Calendar.YEAR);
            input += " " + Integer.toString(year);
            source_format = "dd. MMMM yyyy";
        }
        final long date = TimeFormatter.getMilliSeconds(input, source_format, Locale.GERMAN);
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = input;
        }
        return formattedDate;
    }

    @SuppressWarnings("deprecation")
    public static String getPlaylistid(final DownloadLink dl) {
        final Regex info = new Regex(dl.getDownloadURL(), "(\\d+)_(\\d+)");
        final String playlist_id = info.getMatch(0);
        return playlist_id;
    }

    @SuppressWarnings("deprecation")
    public static int getArrayid(final DownloadLink dl) {
        final Regex info = new Regex(dl.getDownloadURL(), "(\\d+)_(\\d+)");
        final String array_id = info.getMatch(1);
        return Integer.parseInt(array_id);
    }

    @SuppressWarnings("unused")
    private String getXML(final String parameter) {
        return getXML(this.br.toString(), parameter);
    }

    public static String getXML(final String source, final String parameter) {
        String result = new Regex(source, "<" + parameter + "( type=\"[^<>\"/]*?\")?>([^<>]*?)</" + parameter + ">").getMatch(1);
        if (result == null) {
            result = new Regex(source, "<" + parameter + "><\\!\\[CDATA\\[([^<>]*?)\\]\\]></" + parameter + ">").getMatch(0);
        }
        return result;
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