//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "onf.ca" }, urls = { "https?://(www\\.)?(onf|nfb)\\.ca/film/[a-z0-9\\-_]+" })
public class OnfCa extends PluginForHost {

    public OnfCa(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.nfb.ca/about/important-notices/";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    private static final String app = "a8908/v5";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"title\">([^<>\"]*?)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final boolean use_new_handling = true;
        if (use_new_handling) {
            br.getPage(br.getURL() + "/embed/player/?player_mode=&embed_mode=0&context_type=film");
            /* 2017-04-28: New way, kaltura, HLS */
            String dllink = null;
            final String player_embed_url = this.br.getRegex("(\\.kaltura\\.com/p/\\d+/sp/\\d+/embedIframeJs/[^<>\"]+)\"").getMatch(0);
            if (player_embed_url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String partner_id = new Regex(player_embed_url, "/partner_id/(\\d+)").getMatch(0);
            if (partner_id == null) {
                partner_id = "2081491";
            }
            String uiconf_id = this.br.getRegex("uiconf_id/(\\d+)").getMatch(0);
            if (uiconf_id == null) {
                uiconf_id = "36018111";
            }
            String sp = new Regex(player_embed_url, "/sp/(\\d+)/").getMatch(0);
            if (sp == null) {
                sp = "208149100";
            }
            final String entry_id = PluginJSonUtils.getJson(this.br, "entry_id");

            if (StringUtils.isEmpty(entry_id)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            this.br.getPage("https://cdnapisec.kaltura.com/html5/html5lib/v2.51/mwEmbedFrame.php?&wid=_"
                    + partner_id
                    + "&uiconf_id="
                    + uiconf_id
                    + "&entry_id="
                    + entry_id
                    + "&flashvars[localizationCode]=fr&flashvars[IframeCustomPluginCss1]=%2Fmedias%2Fstatic%2Fbrand%2Fcss%2Fplayer.css&flashvars[IframeCustomPluginCss2]=%2Fmedias%2Fstatic%2Fbrand%2Fcss%2FplayerColor%2FplayerColor_yellow.css&flashvars[autoPlay]=false&flashvars[related.plugin]=false&flashvars[CCPlugin]=%7B%22plugin%22%3Atrue%2C%22parent%22%3A%22controlsContainer%22%2C%22iframeHTML5Js%22%3A%22%2Fmedias%2Fstatic%2Fbrand%2Fjs%2Fcustom%2FkalturaPlayerCCPlugin.js%22%2C%22order%22%3A63%2C%22off_label%22%3A%22Off%22%7D&flashvars[DVPlugin]=%7B%22plugin%22%3Atrue%2C%22parent%22%3A%22controlsContainer%22%2C%22iframeHTML5Js%22%3A%22%2Fmedias%2Fstatic%2Fbrand%2Fjs%2Fcustom%2FkalturaPlayerDVPlugin.js%22%2C%22showDV%22%3Afalse%2C%22alternateEntryId%22%3A%221_mpvcxu6w%22%2C%22toggleActiveDV%22%3Afalse%2C%22order%22%3A64%2C%22video_description_string%22%3A%22Vid%C3%A9odescription%22%7D&flashvars[strings]=%7B%22mwe-embedplayer-play_clip%22%3A%22Jouer%22%2C%22mwe-embedplayer-pause_clip%22%3A%22Pause%22%2C%22mwe-embedplayer-volume-mute%22%3A%22Activer%20le%20son%22%2C%22mwe-embedplayer-volume-unmute%22%3A%22D%C3%A9sactiver%20le%20son%22%2C%22mwe-embedplayer-replay%22%3A%22Rejouer%22%2C%22mwe-embedplayer-select_source%22%3A%22Qualit%C3%A9s%20offertes%22%2C%22mwe-embedplayer-switch_source%22%3A%22Changement%20en%20cours%22%2C%22mwe-embedplayer-auto_source%22%3A%22Automatique%22%2C%22mwe-embedplayer-timed_text%22%3A%22Sous-titrage%22%2C%22mwe-embedplayer-player_fullscreen%22%3A%22Plein%20%C3%A9cran%22%2C%22mwe-embedplayer-player_closefullscreen%22%3A%22Quitter%20le%20mode%20plein%20%C3%A9cran%22%2C%22ks-CLIP_NOT_FOUND%22%3A%22Une%20erreur%20s'est%20produite%20en%20jouant%20cette%20vid%C3%A9o.%20Veuillez%20v%C3%A9rifier%20votre%20connexion%20Internet%20et%20r%C3%A9essayer.%22%2C%22ks-CLIP_NOT_FOUND_TITLE%22%3A%22Cette%20vid%C3%A9o%20n'a%20pas%20%C3%A9t%C3%A9%20trouv%C3%A9e.%22%2C%22ks-ENTRY_DELETED%22%3A%22Ce%20contenu%20n'est%20plus%20accessible.%22%2C%22ks-ENTRY_DELETED_TITLE%22%3A%22Ce%20contenu%20a%20%C3%A9t%C3%A9%20supprim%C3%A9.%22%7D&flashvars[streamerType]=auto&playerId=kaltura_player&forceMobileHTML5=true&urid=2.53.2");
            String js = this.br.getRegex("kalturaIframePackageData = (\\{.*?\\}\\}\\});").getMatch(0);
            if (js == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            js = js.replace("\\\"", "\"");
            js = new Regex(js, "\"flavorAssets\":(\\[.*?\\])").getMatch(0);
            if (js == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final ArrayList<Object> ressourcelist = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(js);
            long filesize = 0;
            long max_bitrate = 0;
            long max_bitrate_temp = 0;
            LinkedHashMap<String, Object> entries = null;
            for (final Object videoo : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) videoo;
                final String flavourid = (String) entries.get("id");
                if (flavourid == null) {
                    continue;
                }
                max_bitrate_temp = JavaScriptEngineFactory.toLong(entries.get("bitrate"), 0);
                if (max_bitrate_temp > max_bitrate) {
                    // dllink = "https://cdnapisec.kaltura.com/p/" + partner_id + "/sp/" + sp + "/playManifest/entryId/" + entry_id +
                    // "/flavorId/" + flavourid + "/format/url/protocol/https/a.mp4";
                    dllink = "https://cdnapisec.kaltura.com/p/" + partner_id + "/sp/" + sp + "/playManifest/entryId/" + entry_id + "/flavorId/" + flavourid + "/format/applehttp/protocol/https/a.m3u8?referrer=aHR0cHM6Ly93d3cub25mLmNh&clientTag=html5:v2.53.2&uiConfId=36018111&responseFormat=jsonp&callback=jQuery111106283834349302871_1493393692868";
                    max_bitrate = max_bitrate_temp;
                    filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
                }
            }
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(dllink);
            dllink = PluginJSonUtils.getJson(this.br, "url");
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, dllink);
            dl.startDownload();
        } else {
            /* Old way (before April 2017) - RTMP */
            br.getPage(br.getURL() + "/player_config");
            if (br.toString().equals("No htmlCode read")) {
                /* Media is only available as paid-version. */
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) {
                        throw (PluginException) e;
                    }
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
            }
            final String[] playpaths = br.getRegex("<url>(mp4:[^<>\"]*?)</url>").getColumn(0);
            if (playpaths == null || playpaths.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String currentdomain = new Regex(br.getURL(), "https?://(?:www\\.)?([^<>\"/]+)/").getMatch(0);
            final String filmtitle_url = new Regex(br.getURL(), "/film/([^<>/\"]+)/").getMatch(0);
            final String rtmpurl = "rtmp://nfbca-stream-rtmp.nfbcdn.ca/" + app;
            final String pageurl = "http://www." + currentdomain + "/film/" + filmtitle_url + "/embed/player?player_mode=&embed_mode=0&context_type=film";
            try {
                dl = new RTMPDownload(this, downloadLink, rtmpurl);
            } catch (final NoClassDefFoundError e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
            }
            /* Setup rtmp connection */
            jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setPageUrl(pageurl);
            rtmp.setUrl(rtmpurl);
            /* Chose highest quality available */
            final String playpath = playpaths[playpaths.length - 1];
            rtmp.setPlayPath(playpath);
            rtmp.setApp(app);
            rtmp.setFlashVer("WIN 16,0,0,235");
            rtmp.setSwfUrl("http://media1.nfb.ca/medias/flash/NFBVideoPlayer.swf");
            rtmp.setResume(true);
            ((RTMPDownload) dl).startDownload();
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KalturaVideoPlatform;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}