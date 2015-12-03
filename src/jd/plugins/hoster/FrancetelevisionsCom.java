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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hls.HLSDownloader;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "francetelevisions.fr" }, urls = { "http://francetelevisionsdecrypted/[A-Za-z0-9\\-_]+@[A-Za-z0-9\\-_]+" }, flags = { 0 })
public class FrancetelevisionsCom extends PluginForHost {

    public FrancetelevisionsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.francetv.fr/";
    }

    LinkedHashMap<String, Object> entries = null;

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        this.br.setAllowedResponseCodes(400);
        br.setFollowRedirects(true);
        final String[] videoinfo = link.getDownloadURL().substring(link.getDownloadURL().lastIndexOf("/") + 1).split("@");
        final String videoid = videoinfo[0];
        final String catalogue = videoinfo[1];
        this.br.getPage("http://sivideo.webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=" + videoid + "&catalogue=" + catalogue + "&callback=_jsonp_loader_callback_request_0");
        final String json = this.br.getRegex("^_jsonp_loader_callback_request_0\\((.+)\\)$").getMatch(0);
        final long responsecode = this.br.getHttpConnection().getResponseCode();
        if (responsecode == 400 || responsecode == 404 || json == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(json);
        final String title = (String) entries.get("titre");
        final String subtitle = (String) entries.get("sous_titre");
        final String description = (String) entries.get("synopsis");
        final String date = (String) DummyScriptEnginePlugin.walkJson(entries, "diffusion/date_debut");
        String channel = (String) entries.get("chaine");

        final DecimalFormat df = new DecimalFormat("00");
        final long season = DummyScriptEnginePlugin.toLong(entries.get("saison"), -1);
        final long episode = DummyScriptEnginePlugin.toLong(entries.get("episode"), -1);

        if (inValidate(title)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (inValidate(channel)) {
            /* Fallback to default channel */
            channel = "francetelevisions";
        }
        String filename = "";
        if (!inValidate(date)) {
            final String date_formatted = formatDate(date);
            filename += date_formatted + "_";
        }
        filename += channel + "_" + title;
        if (season != -1 && episode != -1) {
            filename += "_S" + df.format(season) + "E" + df.format(episode);
        }
        if (!inValidate(subtitle)) {
            filename += " - " + subtitle;
        }
        filename += ".mp4";
        filename = encodeUnicode(filename);
        link.setFinalFileName(filename);

        if (!inValidate(description) && inValidate(link.getComment())) {
            link.setComment(description);
        }

        return AvailableStatus.TRUE;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dlurl = null;
        final ArrayList<Object> ressourcelist = (ArrayList) entries.get("videos");
        for (final Object videoo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) videoo;
            final String format = (String) entries.get("format");
            if (inValidate(format)) {
                continue;
            }
            if (format.contains("hls") || format.contains("m3u8") || format.equals("mp4-dp")) {
                dlurl = (String) entries.get("url");
                if (!inValidate(dlurl)) {
                    break;
                }
            }
        }
        if (dlurl == null) {
            /* Seems like what the user wants to download hasn't aired yet --> Wait and retry later! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dlurl.contains(".m3u8")) {
            /* Download hls */
            br.getPage(dlurl);
            if (this.br.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "This content is not available in your country");
            }
            final String[] medias = this.br.getRegex("#EXT-X-STREAM-INF([^\r\n]+[\r\n]+[^\r\n]+)").getColumn(-1);
            if (medias == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String url_hls = null;
            long bandwidth_highest = 0;
            for (final String media : medias) {
                // name = quality
                // final String quality = new Regex(media, "NAME=\"(.*?)\"").getMatch(0);
                final String bw = new Regex(media, "BANDWIDTH=(\\d+)").getMatch(0);
                final long bandwidth_temp = Long.parseLong(bw);
                if (bandwidth_temp > bandwidth_highest) {
                    bandwidth_highest = bandwidth_temp;
                    url_hls = new Regex(media, "https?://[^\r\n]+").getMatch(-1);
                }
            }
            if (url_hls == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, url_hls);
        } else {
            /* Download http (rare case!) */
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlurl, true, 0);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    private String formatDate(final String input) {
        final long date = TimeFormatter.getMilliSeconds(input, "dd/MM/yyyy HH:mm", Locale.FRANCE);
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

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
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

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
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