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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "francetelevisions.fr" }, urls = { "http://francetelevisionsdecrypted/[A-Za-z0-9\\-_]+@[A-Za-z0-9\\-_]+" })
public class FrancetelevisionsCom extends PluginForHost {
    public FrancetelevisionsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.francetv.fr/";
    }

    LinkedHashMap<String, Object> entries = null;

    /**
     * Basically all french public TV stations use 1 network / API and we only need this one API request to get all information needed to
     * download their videos :)
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        this.br.setAllowedResponseCodes(400);
        this.br.setFollowRedirects(true);
        final String[] videoinfo = link.getDownloadURL().substring(link.getDownloadURL().lastIndexOf("/") + 1).split("@");
        final String videoid = videoinfo[0];
        /* 2017-05-10: The 'catalogue' parameter is not required anymore or at least not for all videoids! */
        final String catalogue = videoinfo[1].equals("null") ? "" : videoinfo[1];
        this.br.getPage("https://sivideo.webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=" + videoid + "&catalogue=" + catalogue + "&callback=_jsonp_loader_callback_request_0");
        final String json = this.br.getRegex("^_jsonp_loader_callback_request_0\\((.+)\\)$").getMatch(0);
        final long responsecode = this.br.getHttpConnection().getResponseCode();
        if (responsecode == 400 || responsecode == 404 || json == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        final String title = (String) entries.get("titre");
        final String subtitle = (String) entries.get("sous_titre");
        final String description = (String) entries.get("synopsis");
        final String date = (String) JavaScriptEngineFactory.walkJson(entries, "diffusion/date_debut");
        String channel = (String) entries.get("chaine");
        final DecimalFormat df = new DecimalFormat("00");
        final long season = JavaScriptEngineFactory.toLong(entries.get("saison"), -1);
        final long episode = JavaScriptEngineFactory.toLong(entries.get("episode"), -1);
        if (inValidate(title)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (inValidate(channel)) {
            /* Fallback to default channel */
            channel = "francetelevisions";
        }
        String filename = "";
        if (!inValidate(date)) {
            /* Yap - the date is not always given! */
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
        filename = Encoding.htmlDecode(filename).trim();
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
        String hls_master = null;
        String url_http = null;
        String url_temp = null;
        final ArrayList<Object> ressourcelist = (ArrayList) entries.get("videos");
        for (final Object videoo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) videoo;
            final String format = (String) entries.get("format");
            url_temp = (String) entries.get("url");
            if (inValidate(format) || inValidate(url_temp)) {
                continue;
            }
            if (format.equals("mp4-dp")) {
                url_http = url_temp;
            } else if (format.contains("hls") || format.contains("m3u8")) {
                hls_master = url_temp;
            }
        }
        if (hls_master == null && url_http == null) {
            /*
             * That should never happen. Even geo-blocked users should get the final downloadlinks even though download attempts will end up
             * in response code 403 which is handled correctly below.
             */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (url_http != null) {
            /* Download http (rare case!) */
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url_http, true, 0);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "This content is not available in your country");
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            // this.br.getPage("http://hdfauthftv-a.akamaihd.net/esi/TA?format=json&url=" + Encoding.urlEncode(hls_master) +
            // "&callback=_jsonp_loader_callback_request_2");
            // final String json = this.br.getRegex("^_jsonp_loader_callback_request_2\\((.+)\\)$").getMatch(0);
            // entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
            // hls_master = (String) entries.get("url");
            // this.br.getPage(hls_master);
            /* Download hls */
            br.getPage(hls_master);
            if (this.br.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "This content is not available in your country");
            } else if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, url_hls);
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