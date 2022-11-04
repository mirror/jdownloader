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
package jd.plugins.decrypter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawlerDeepInspector;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.GenericM3u8;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.plugins.components.config.GenericM3u8DecrypterConfig;
import org.jdownloader.plugins.components.config.GenericM3u8DecrypterConfig.CrawlSpeedMode;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin.FEATURE;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "m3u8" }, urls = { "https?://.+\\.m3u8($|(?:\\?|%3F)[^\\s<>\"']*|#.*)" })
public class GenericM3u8Decrypter extends PluginForDecrypt {
    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }

    public GenericM3u8Decrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.GENERIC };
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        CrawledLink source = getCurrentLink();
        String referer = null;
        String cookiesString = null;
        while (source != null) {
            if (source.getDownloadLink() != null && StringUtils.equals(source.getURL(), param.getCryptedUrl())) {
                final DownloadLink downloadLink = source.getDownloadLink();
                cookiesString = downloadLink.getStringProperty("cookies", null);
                if (cookiesString != null) {
                    final String host = Browser.getHost(source.getURL());
                    br.setCookies(host, Cookies.parseCookies(cookiesString, host, null));
                }
            }
            if (!StringUtils.equals(source.getURL(), param.getCryptedUrl())) {
                if (source.getCryptedLink() != null) {
                    referer = source.getURL();
                    br.getPage(source.getURL());
                }
                break;
            } else {
                source = source.getSourceLink();
            }
        }
        /* Look for referer inside URL and prefer that */
        String forced_referer = new Regex(param.getCryptedUrl(), "((\\&|\\?|#)forced_referer=.+)").getMatch(0);
        if (forced_referer != null) {
            forced_referer = new Regex(forced_referer, "forced_referer=([A-Za-z0-9=]+)").getMatch(0);
            if (forced_referer != null) {
                String ref = null;
                if (forced_referer.matches("^[a-fA-F0-9]+$") && forced_referer.length() % 2 == 0) {
                    final byte[] bytes = HexFormatter.hexToByteArray(forced_referer);
                    ref = bytes != null ? new String(bytes) : null;
                }
                if (ref == null) {
                    ref = Encoding.Base64Decode(forced_referer);
                }
                if (ref != null) {
                    try {
                        br.getPage(ref);
                        referer = ref;
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                }
            }
        }
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!LinkCrawlerDeepInspector.looksLikeMpegURL(br.getHttpConnection())) {
            logger.info("!Response is not a valid HLS construct according to headers!");
            /* This is only an indicator. Continue anyways. */
        }
        return parseM3U8(this, param.getCryptedUrl(), br, referer, cookiesString, null);
    }

    public static ArrayList<DownloadLink> parseM3U8(final PluginForDecrypt plugin, final String m3u8URL, final Browser br, final String referer, final String cookiesString, final String preSetName) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final GenericM3u8DecrypterConfig cfg = PluginJsonConfig.get(GenericM3u8DecrypterConfig.class);
        final CrawlSpeedMode mode = cfg.getCrawlSpeedMode();
        if (br.containsHTML("#EXT-X-STREAM-INF")) {
            final Map<HlsContainer, URL> hlsContainers = new HashMap<HlsContainer, URL>();
            final String sessionDataTitle = br.getRegex("#EXT-X-SESSION-DATA:DATA-ID\\s*=\\s*\"[^\"]*title\"[^\r\n]*VALUE\\s*=\\s*\"(.*?)\"").getMatch(0);
            final ArrayList<String> infos = new ArrayList<String>();
            for (final String line : Regex.getLines(br.toString())) {
                if (StringUtils.startsWithCaseInsensitive(line, "concat") || StringUtils.contains(line, "file:")) {
                    continue;
                } else if (!line.startsWith("#")) {
                    infos.add(line);
                    final String m3u8Content = StringUtils.join(infos, "\r\n");
                    final List<HlsContainer> hlsContainer = HlsContainer.parseHlsQualities(m3u8Content, br);
                    if (hlsContainer.size() > 1) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else if (hlsContainer.size() == 1) {
                        hlsContainers.put(hlsContainer.get(0), br.getURL(line));
                    } else {
                        // Parser found multiple HlsContainers? This should never happen and indicates a problem with this parser!
                    }
                    infos.clear();
                } else {
                    infos.add(line);
                }
            }
            /*
             * TODO: Put bandwidth into filenames if same resolution (width!) video is available multiple times and/or use label in filename
             * or at least set label as property, see: https://svn.jdownloader.org/issues/90277
             */
            final String finalFallbackTitle = new Regex(m3u8URL, "/([^/]+)\\.m3u8").getMatch(0);
            FilePackage fp = FilePackage.getInstance();
            if (preSetName != null) {
                fp.setName(preSetName);
            } else {
                fp = null;
            }
            Long estimatedDurationMillis = null;
            for (final Entry<HlsContainer, URL> entry : hlsContainers.entrySet()) {
                final HlsContainer hls = entry.getKey();
                final URL url = entry.getValue();
                if (fp == null) {
                    final String singleStreamFallbackTitle = new Regex(url.toString(), "/([^/]+)\\.m3u8").getMatch(0);
                    if (singleStreamFallbackTitle != null) {
                        fp = FilePackage.getInstance();
                        fp.setName(singleStreamFallbackTitle);
                    } else if (finalFallbackTitle != null) {
                        /* TODO: Maybe ignore this if it is "master" (from "master.m3u8"). */
                        fp = FilePackage.getInstance();
                        fp.setName(finalFallbackTitle);
                    }
                }
                final DownloadLink link = new DownloadLink(null, null, plugin.getHost(), GenericM3u8.createURLForThisPlugin(url.toString()), true);
                link.setProperty("m3u8Source", m3u8URL);
                link.setReferrerUrl(referer);
                link.setProperty("cookies", cookiesString);
                if (StringUtils.isNotEmpty(preSetName)) {
                    link.setProperty(GenericM3u8.PRESET_NAME_PROPERTY, preSetName);
                } else if (StringUtils.isNotEmpty(sessionDataTitle)) {
                    link.setProperty(GenericM3u8.PRESET_NAME_PROPERTY, sessionDataTitle);
                }
                hls.setPropertiesOnDownloadLink(link);
                if (mode == CrawlSpeedMode.FAST) {
                    link.setAvailable(true);
                    if (hls.getAverageBandwidth() > 0 || hls.getBandwidth() > 0) {
                        if (estimatedDurationMillis == null) {
                            /**
                             * Load first item to get the estimated play-duration which we expect to be the same for all items. </br> Based
                             * on this we can set estimated filesizes while at the same time providing a super fast crawling experience.
                             */
                            final List<M3U8Playlist> playlist = hls.getM3U8(br);
                            estimatedDurationMillis = M3U8Playlist.getEstimatedDuration(playlist);
                            link.setDownloadSize(M3U8Playlist.getEstimatedSize(playlist));
                        } else {
                            /*
                             * TODO: Maybe prefer smaller of both possible bandwidth values for filesize calculation as that seems to be
                             * more accurate?
                             */
                            int bandwidth = hls.getAverageBandwidth();
                            if (bandwidth < 0) {
                                bandwidth = hls.getBandwidth();
                            }
                            link.setDownloadSize(bandwidth / 8 * (estimatedDurationMillis / 1000));
                        }
                    }
                    if (estimatedDurationMillis != null) {
                        link.setProperty(GenericM3u8.PROPERTY_DURATION_ESTIMATED_MILLIS, estimatedDurationMillis);
                    }
                } else if (mode == CrawlSpeedMode.SUPERFAST) {
                    link.setAvailable(true);
                }
                if (fp != null) {
                    link._setFilePackage(fp);
                }
                GenericM3u8.setFilename(link, false);
                ret.add(link);
            }
        } else {
            final DownloadLink link = new DownloadLink(null, null, plugin.getHost(), GenericM3u8.createURLForThisPlugin(m3u8URL), true);
            link.setReferrerUrl(referer);
            link.setProperty("cookies", cookiesString);
            if (mode == CrawlSpeedMode.SUPERFAST) {
                link.setAvailable(true);
            }
            if (StringUtils.isNotEmpty(preSetName)) {
                link.setProperty(GenericM3u8.PRESET_NAME_PROPERTY, preSetName);
            }
            GenericM3u8.setFilename(link, false);
            ret.add(link);
        }
        return ret;
    }

    @Override
    public boolean hasCaptcha(final CryptedLink link, final Account acc) {
        return false;
    }

    @Override
    public Class<GenericM3u8DecrypterConfig> getConfigInterface() {
        return GenericM3u8DecrypterConfig.class;
    }
}