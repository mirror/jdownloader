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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.VariantInfoMassengeschmackTv;
import jd.plugins.hoster.MassengeschmackTv;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "massengeschmack.tv" }, urls = { "https?://(?:www\\.)?massengeschmack\\.tv/((?:play|clip)/|index_single\\.php\\?id=)[a-z0-9\\-]+|https?://(?:www\\.)?massengeschmack\\.tv/live/[a-z0-9\\-]+" })
public class MassengeschmackTvCrawler extends PluginForDecrypt {
    public MassengeschmackTvCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static String correctAddedURL(final String url_source) {
        final String mgtv_videoid = new Regex(url_source, "massengeschmack\\.tv/(?:(?:play|clip)/|index_single\\.php\\?id=)([a-z0-9\\-]+)").getMatch(0);
        final String url_new = String.format("https://massengeschmack.tv/play/%s", mgtv_videoid);
        return url_new;
    }

    private final List<String> all_known_qualities = Arrays.asList("1080p", "720p", "432p", "AUDIO_m4a", "AUDIO_mp3");

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = correctAddedURL(param.toString());
        boolean is_premiumonly_content = false;
        String dllink = null;
        final String url_videoid = MassengeschmackTv.getUrlNameForMassengeschmackGeneral(parameter);
        String url_videoid_without_episodenumber = MassengeschmackTv.getVideoidWithoutEpisodenumber(url_videoid);
        this.br.setFollowRedirects(true);
        long filesize = -1;
        long filesize_max = 0;
        long filesize_temp = 0;
        final String api_best_url = MassengeschmackTv.getBESTDllinkSpecialAPICall(url_videoid);
        String dllink_temp = null;
        String filesize_string = null;
        String episodenumber = null;
        String channel = null;
        String episodename = null;
        String date = null;
        String description = null;
        URLConnectionAdapter con = null;
        /* Login */
        boolean loggedin = false;
        final PluginForHost hosterPlugin = JDUtilities.getPluginForHost(this.getHost());
        final SubConfiguration cfg = hosterPlugin.getPluginConfig();
        final Account account = AccountController.getInstance().getValidAccount(hosterPlugin);
        try {
            MassengeschmackTv.login(this.br, account, false);
            loggedin = true;
        } catch (final Throwable e) {
            loggedin = false;
        }
        List<String> all_selected_qualities = new ArrayList<String>();
        /* Check which qualities the user wants */
        final boolean add1080p = cfg.getBooleanProperty("LOAD_1080p", true);
        final boolean add720p = cfg.getBooleanProperty("LOAD_720p", true);
        final boolean add432p = cfg.getBooleanProperty("LOAD_432p", true);
        final boolean addAudioM4a = cfg.getBooleanProperty("LOAD_AUDIO_m4a", true);
        final boolean addAudioMp3 = cfg.getBooleanProperty("LOAD_AUDIO_mp3", true);
        if (add1080p) {
            all_selected_qualities.add("1080p");
        }
        if (add720p) {
            all_selected_qualities.add("720p");
        }
        if (add432p) {
            all_selected_qualities.add("432p");
        }
        if (addAudioM4a) {
            all_selected_qualities.add("AUDIO_m4a");
        }
        if (addAudioMp3) {
            all_selected_qualities.add("AUDIO_mp3");
        }
        /* User selected nothing --> Add everything */
        if (all_selected_qualities.isEmpty()) {
            all_selected_qualities = this.all_known_qualities;
        }
        /* TODO */
        // final VariantInfoMassengeschmackTv chosenVariant = getActiveVariantByLink(link);
        ArrayList<VariantInfoMassengeschmackTv> variants = new ArrayList<VariantInfoMassengeschmackTv>();
        final Browser br2 = this.br.cloneBrowser();
        VariantInfoMassengeschmackTv bestVariant = null;
        try {
            if (parameter.matches(MassengeschmackTv.TYPE_MASSENGESCHMACK_GENERAL) && MassengeschmackTv.VIDEOS_ENABLE_API && loggedin) {
                br.getPage(String.format(MassengeschmackTv.API_BASE_URL + MassengeschmackTv.API_GET_CLIP, url_videoid));
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
                final ArrayList<Object> files = (ArrayList) entries.get("files");
                channel = (String) entries.get("pdesc");
                episodename = (String) entries.get("title");
                description = (String) entries.get("desc");
                date = Long.toString(JavaScriptEngineFactory.toLong(entries.get("date"), -1));
                for (final Object fileo : files) {
                    entries = (LinkedHashMap<String, Object>) fileo;
                    final String qualityDescription = (String) entries.get("desc");
                    final String type = (String) entries.get("t");
                    filesize_temp = JavaScriptEngineFactory.toLong(entries.get("size"), -1);
                    dllink_temp = (String) entries.get("url");
                    if (StringUtils.isEmpty(dllink_temp) || filesize_temp == -1 || StringUtils.isEmpty(type) || StringUtils.isEmpty(qualityDescription)) {
                        continue;
                    }
                    if (dllink_temp.startsWith("//")) {
                        dllink_temp = "https:" + dllink_temp;
                    }
                    String quality;
                    String resolution = null;
                    if ("film".equalsIgnoreCase(type)) {
                        /* Video */
                        resolution = (String) entries.get("dimensions");
                        if (StringUtils.isEmpty(resolution)) {
                            /* This should never happen */
                            continue;
                        }
                        quality = new Regex(qualityDescription, "(\\d+p)").getMatch(0);
                        if (StringUtils.isEmpty(quality)) {
                            /* This should never happen */
                            quality = "unknown_" + resolution;
                        }
                    } else {
                        /* Audio */
                        if (dllink_temp.endsWith(".m4a")) {
                            quality = "AUDIO_m4a";
                        } else {
                            quality = "AUDIO_mp3";
                        }
                    }
                    final VariantInfoMassengeschmackTv currVariant = new VariantInfoMassengeschmackTv(dllink_temp, quality, filesize_temp);
                    if (!StringUtils.isEmpty(resolution)) {
                        currVariant.setResolution(resolution);
                    }
                    variants.add(currVariant);
                    if (filesize_temp > filesize_max) {
                        filesize_max = filesize_temp;
                        bestVariant = currVariant;
                        /* Set filesize and directurl - that value will be used later if we only have a single quality available. */
                        filesize = filesize_max;
                        dllink = dllink_temp;
                    }
                }
                /* Get downloadlink and videoextension */
                if (!StringUtils.isEmpty(dllink)) {
                    /* Okay we already have a final downloadlink but let's try to get an even higher quality via api_best_url. */
                    /*
                     * This might sometimes get us the HD version even if it is not officially available for registered-/free users:
                     * http://forum.massengeschmack.tv/showthread.php?17604-Api&p=439951#post439951
                     */
                    con = this.br.openHeadConnection(api_best_url);
                    if (con.isOK() && !con.getContentType().contains("html")) {
                        filesize = con.getLongContentLength();
                        dllink = api_best_url;
                    }
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
                if (StringUtils.isEmpty(dllink) && account.getType() == AccountType.FREE) {
                    /*
                     * Rare special case: User has a free account, video is NOT downloadable for freeusers but is watchable. In this case we
                     * cannot get any downloadlinks via API --> Website handling needed! This could also be used as a fallback for API
                     * failures but it is not intended to be that ;)
                     */
                    try {
                        this.br.getPage("http://massengeschmack.tv/play/" + url_videoid);
                        dllink = MassengeschmackTv.getStreamDllinkMassengeschmackWebsite(this.br);
                    } catch (final Throwable e) {
                    }
                }
                /* Errorhandling for worst case - no filename information available at all! */
                if (!StringUtils.isEmpty(channel) && !StringUtils.isEmpty(episodename) && !date.equals("-1")) {
                    /* Oops we have no filename information at all --> Fallback to finallink-filename or url-filename. */
                    if (!StringUtils.isEmpty(episodename)) {
                        episodenumber = new Regex(episodename, "Folge (\\d+)").getMatch(0);
                        if (StringUtils.isEmpty(episodenumber)) {
                            episodenumber = MassengeschmackTv.getEpisodenumberFromVideoid(url_videoid);
                        }
                        /* Fix episodename - remove episodenumber inside episodename */
                        if (!StringUtils.isEmpty(episodenumber)) {
                            /* Clean episodename! */
                            final String realepisodename = new Regex(episodename, "Folge \\d+: \"([^<>\"]*?)\"").getMatch(0);
                            if (!StringUtils.isEmpty(realepisodename)) {
                                episodename = realepisodename;
                            } else {
                                episodename = episodename.replace("Folge " + episodenumber, "");
                            }
                        }
                    }
                }
            } else if (parameter.matches(MassengeschmackTv.TYPE_MASSENGESCHMACK_GENERAL) || parameter.matches(MassengeschmackTv.TYPE_MASSENGESCHMACK_LIVE)) {
                is_premiumonly_content = false;
                br.getPage(parameter);
                if (br.containsHTML(MassengeschmackTv.HTML_MASSENGESCHMACK_OFFLINE) || this.br.getHttpConnection().getResponseCode() == 404) {
                    decryptedLinks.add(this.createDownloadlink(parameter));
                    return decryptedLinks;
                } else if (this.br.getHttpConnection().getResponseCode() == 403) {
                    is_premiumonly_content = true;
                } else {
                    is_premiumonly_content = false;
                    if (parameter.matches(MassengeschmackTv.TYPE_MASSENGESCHMACK_LIVE)) {
                        /* Get m3u8 main playlist */
                        dllink = br.getRegex("\"(https?://dl\\.massengeschmack\\.tv/live/[^<>\"]*?adaptive\\.m3u8)\"").getMatch(0);
                        episodename = this.br.getRegex("class=\"active\"><span>([^<>\"]*?)</span></li>").getMatch(0);
                        if (episodename == null) {
                            /* Fallback to url */
                            episodename = new Regex(parameter, "([a-z0-9\\-]+)$").getMatch(0);
                        }
                        episodenumber = new Regex(episodename, "(\\d+)$").getMatch(0);
                        if (episodenumber != null) {
                            /* Remove episodenumber from episodename! */
                            episodename = episodename.substring(0, episodename.length() - episodenumber.length());
                        }
                        /* Get date without time */
                        date = br.getRegex("<p class=\"muted\">(\\d{2}\\.\\d{2}\\.\\d{2} \\d{2}:\\d{2})[^<>\"]+<").getMatch(0);
                    } else {
                        /* 2016-04-14: Prefer stream urls over download as the .webm qualities are usually higher */
                        dllink = MassengeschmackTv.getStreamDllinkMassengeschmackWebsite(this.br);
                        /*
                         * Add all other qualities found in HTML --> Usually only SD quality is downloadable for free users.
                         */
                        final String[] downloadlink_info = this.br.getRegex("(massengeschmack\\.tv/dl/.*?</small></em></a></li>)").getColumn(0);
                        if (downloadlink_info != null && downloadlink_info.length > 0) {
                            for (final String dlinfo : downloadlink_info) {
                                /* E.g. <strong>HD 720p</strong> <em><small>1280x720 (875 MiB)</small> */
                                final Regex qualityRegex = new Regex(dlinfo, "<strong>([^<>]+)</strong> <em><small>([^<>\"]+)</small>");
                                final String variantQualityName = qualityRegex.getMatch(0);
                                final String variantQualityInfo = qualityRegex.getMatch(1);
                                dllink_temp = new Regex(dlinfo, "(/dl/[^<>\"]+)\"").getMatch(0);
                                filesize_string = new Regex(dlinfo, "\\((\\d+(?:,\\d+)? (?:MiB|GiB))\\)").getMatch(0);
                                if (!StringUtils.isEmpty(filesize_string)) {
                                    filesize_string = filesize_string.replace(",", ".");
                                    filesize_temp = SizeFormatter.getSize(filesize_string);
                                }
                                if (StringUtils.isEmpty(variantQualityName) || StringUtils.isEmpty(variantQualityInfo) || StringUtils.isEmpty(dllink_temp)) {
                                    /* Skip invalid items */
                                    continue;
                                }
                                if (dllink_temp.startsWith("/")) {
                                    dllink_temp = "https://" + br.getHost() + dllink_temp;
                                }
                                final String quality;
                                String videoResolution = null;
                                if (dllink_temp.contains(".mp4") || dllink_temp.contains(".webm")) {
                                    /* Video */
                                    final String variantQualityP = new Regex(variantQualityName, "(\\d+p)").getMatch(0);
                                    videoResolution = new Regex(variantQualityInfo, "(\\d+x\\d+)").getMatch(0);
                                    if (StringUtils.isEmpty(variantQualityP) || StringUtils.isEmpty(videoResolution)) {
                                        continue;
                                    }
                                    quality = variantQualityP;
                                } else {
                                    /* Audio */
                                    if (dllink_temp.endsWith(".m4a")) {
                                        quality = "AUDIO_m4a";
                                    } else {
                                        quality = "AUDIO_mp3";
                                    }
                                }
                                final VariantInfoMassengeschmackTv currVariant;
                                /* Given filesize is buggy sometimes --> Display as unknown then e.g. massengeschmack.tv/clip/fktv1 */
                                if (!StringUtils.isEmpty(filesize_string) && filesize_temp > 500000) {
                                    currVariant = new VariantInfoMassengeschmackTv(dllink_temp, quality, filesize_temp);
                                } else {
                                    currVariant = new VariantInfoMassengeschmackTv(dllink_temp, quality);
                                }
                                if (!StringUtils.isEmpty(videoResolution)) {
                                    currVariant.setResolution(videoResolution);
                                }
                                variants.add(currVariant);
                                if (filesize_temp > filesize_max) {
                                    filesize_max = filesize_temp;
                                    bestVariant = currVariant;
                                    /*
                                     * Set filesize and directurl - that value will be used later if we only have a single quality
                                     * available.
                                     */
                                    filesize = filesize_max;
                                    dllink = dllink_temp;
                                }
                            }
                        }
                        /* TODO: Check this */
                        if (!StringUtils.isEmpty(dllink)) {
                            /* Okay we already have a final downloadlink but let's try to get an even higher quality via api_best_url. */
                            /*
                             * This might sometimes get us the HD version even if it is not officially available for registered-/free users:
                             * http://forum.massengeschmack.tv/showthread.php?17604-Api&p=439951#post439951
                             */
                            con = br2.openHeadConnection(api_best_url);
                            if (con.isOK() && !con.getContentType().contains("html")) {
                                filesize = con.getLongContentLength();
                                dllink = api_best_url;
                            }
                            try {
                                con.disconnect();
                            } catch (final Throwable e) {
                            }
                        }
                        channel = br.getRegex("href=\"/mag\\-cover\\.php[^\"]+\">([^<>\"]*?)<").getMatch(0);
                        date = br.getRegex("<small><a[^>]+></a>([^<>\"]+)</small>").getMatch(0);
                        description = this.br.getRegex("</h4>[\t\n\r ]+<p>([^<]+)</").getMatch(0);
                        episodename = br.getRegex("<li class=\"active\"><span>([^<>\"]+)<").getMatch(0);
                        episodenumber = this.br.getRegex("class=\"active\"><span>Folge (\\d+)</span></li>").getMatch(0);
                        if (StringUtils.isEmpty(episodenumber) && !StringUtils.isEmpty(episodename)) {
                            episodenumber = new Regex(episodename, "Folge (\\d+)").getMatch(0);
                        }
                        if (StringUtils.isEmpty(episodenumber)) {
                            episodenumber = MassengeschmackTv.getEpisodenumberFromVideoid(url_videoid);
                        }
                        if (!StringUtils.isEmpty(episodename) && !StringUtils.isEmpty(episodenumber)) {
                            /* Remove episodenumber from episodename so that the name we create later looks better. */
                            final String episodetext = String.format("Folge %s", episodenumber);
                            final String episodetext2 = String.format("Folge %s: ", episodenumber);
                            episodename = episodename.replace(episodetext2, "");
                            episodename = episodename.replace(episodetext, "");
                        }
                        if (!StringUtils.isEmpty(channel) && !StringUtils.isEmpty(episodename)) {
                            /*
                             * Remove channelname inside episodename to get better filenames - this may actually inValidate the episodename
                             * completely if it only consists of the channelname but that is fine.
                             */
                            final String possible_channeltext_inside_episodename = String.format("%s - ", channel);
                            episodename = episodename.replace(possible_channeltext_inside_episodename, "");
                        }
                    }
                }
            } else {
                throw new DecrypterException();
            }
            if (filesize < 0 && filesize_string != null && !StringUtils.isEmpty(dllink)) {
                if (filesize < 0 && filesize_string != null) {
                    filesize = SizeFormatter.getSize(filesize_string);
                }
                if (filesize < 0 && !StringUtils.isEmpty(dllink) && !dllink.contains(".m3u8")) {
                    con = br2.openHeadConnection(dllink);
                    final long responsecode = con.getResponseCode();
                    if (con.isOK() && !con.getContentType().contains("html")) {
                        filesize = con.getLongContentLength();
                    } else if (responsecode == MassengeschmackTv.API_RESPONSECODE_ERROR_LOGIN_WRONG || responsecode == 403) {
                        is_premiumonly_content = true;
                    } else {
                        /* 404 and/or html --> Probably offline */
                        decryptedLinks.add(this.createDownloadlink(parameter));
                        return decryptedLinks;
                    }
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (variants.isEmpty()) {
            /* There is only one variant (e.g. free download or hls download) */
            /* TODO: Find quality-string via URL */
            String quality = "unknown_TODO_FIXME";
            final VariantInfoMassengeschmackTv singleQualityVariant;
            if (!StringUtils.isEmpty(filesize_string)) {
                singleQualityVariant = new VariantInfoMassengeschmackTv(dllink, quality, filesize);
            } else {
                singleQualityVariant = new VariantInfoMassengeschmackTv(dllink, quality);
            }
            variants.add(singleQualityVariant);
        }
        final HashMap<String, DownloadLink> all_found_downloadlinks = new HashMap<String, DownloadLink>();
        /* TODO: Switch to variant support */
        // if (variants.size() > 1) {
        // MassengeschmackTv.sortVariants(variants);
        // }
        // /* Set best quality */
        // MassengeschmackTv.setVariant(dl_variant, variants.get(variants.size() - 1));
        // dl_variant.setVariantSupport(true);
        for (final VariantInfoMassengeschmackTv variant : variants) {
            final DownloadLink dl = this.createDownloadlink(variant.getUrl());
            setDownloadLinkProperties(dl, variant, date, channel, episodename, episodenumber, url_videoid_without_episodenumber, url_videoid, description);
            all_found_downloadlinks.put(variant.getQualityName(), dl);
        }
        final HashMap<String, DownloadLink> finalSelectedQualityMap = handleQualitySelection(all_found_downloadlinks, all_selected_qualities, false, false, true);
        /* Finally add selected URLs */
        final Iterator<Entry<String, DownloadLink>> it = finalSelectedQualityMap.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, DownloadLink> entry = it.next();
            final DownloadLink dl = entry.getValue();
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(url_videoid);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private void setDownloadLinkProperties(final DownloadLink dl, final VariantInfoMassengeschmackTv variant, String date, String channel, String episodename, String episodenumber, final String url_videoid_without_episodenumber, final String url_videoid, String description) {
        if (!StringUtils.isEmpty(episodename)) {
            episodename = Encoding.htmlDecode(episodename).trim();
            dl.setProperty("directepisodename", episodename);
        }
        if (!StringUtils.isEmpty(date)) {
            date = date.trim();
            dl.setProperty("directdate", date);
        }
        if (!StringUtils.isEmpty(channel)) {
            channel = Encoding.htmlDecode(channel).trim();
            /* Fix channel */
            if (!StringUtils.isEmpty(channel)) {
                if (!channel.toLowerCase().contains(url_videoid_without_episodenumber.toLowerCase())) {
                    channel = url_videoid_without_episodenumber;
                }
            }
            dl.setProperty("directchannel", channel);
        }
        if (!StringUtils.isEmpty(episodenumber)) {
            dl.setProperty("directepisodenumber", episodenumber);
        }
        if (!StringUtils.isEmpty(description)) {
            description = Encoding.htmlDecode(description);
            dl.setComment(description);
        }
        /* Set properties of variant on DownloadLink */
        final String qualityName = variant.getQualityName();
        final long filesizeCurrent = variant.getFilesizeLong();
        dl.setLinkID(url_videoid + "_" + qualityName);
        if (filesizeCurrent > 0) {
            dl.setDownloadSize(filesizeCurrent);
        }
        String filename_temp = MassengeschmackTv.getMassengeschmack_other_FormattedFilename(dl, variant);
        if (filename_temp == null) {
            /* TODO: Make sure this works and we never get any 'nulls' in our filename! */
            filename_temp = MassengeschmackTv.getFilenameLastChance(variant.getUrl(), url_videoid);
        }
        dl.setFinalFileName(filename_temp);
        dl.setAvailable(true);
    }

    private HashMap<String, DownloadLink> handleQualitySelection(final HashMap<String, DownloadLink> all_found_downloadlinks, final List<String> all_selected_qualities, final boolean grab_best, final boolean grab_best_out_of_user_selection, final boolean grab_unknown) {
        HashMap<String, DownloadLink> all_selected_downloadlinks = new HashMap<String, DownloadLink>();
        final Iterator<Entry<String, DownloadLink>> iterator_all_found_downloadlinks = all_found_downloadlinks.entrySet().iterator();
        if (grab_best) {
            for (final String possibleQuality : this.all_known_qualities) {
                if (all_found_downloadlinks.containsKey("LOAD_" + possibleQuality)) {
                    all_selected_downloadlinks.put(possibleQuality, all_found_downloadlinks.get(possibleQuality));
                    break;
                }
            }
            if (all_selected_downloadlinks.isEmpty()) {
                logger.info("Possible issue: Best selection found nothing --> Adding ALL");
                while (iterator_all_found_downloadlinks.hasNext()) {
                    final Entry<String, DownloadLink> dl_entry = iterator_all_found_downloadlinks.next();
                    all_selected_downloadlinks.put(dl_entry.getKey(), dl_entry.getValue());
                }
            }
        } else {
            boolean atLeastOneSelectedItemExists = false;
            while (iterator_all_found_downloadlinks.hasNext()) {
                final Entry<String, DownloadLink> dl_entry = iterator_all_found_downloadlinks.next();
                final String dl_quality_string = dl_entry.getKey();
                if (all_selected_qualities.contains(dl_quality_string)) {
                    atLeastOneSelectedItemExists = true;
                    all_selected_downloadlinks.put(dl_quality_string, dl_entry.getValue());
                } else if (!all_known_qualities.contains(dl_quality_string) && grab_unknown) {
                    logger.info("Found unknown quality: " + dl_quality_string);
                    if (grab_unknown) {
                        logger.info("Adding unknown quality: " + dl_quality_string);
                        all_selected_downloadlinks.put(dl_quality_string, dl_entry.getValue());
                    }
                }
            }
            if (!atLeastOneSelectedItemExists) {
                logger.info("Possible user error: User selected only qualities which are not available --> Adding ALL");
                while (iterator_all_found_downloadlinks.hasNext()) {
                    final Entry<String, DownloadLink> dl_entry = iterator_all_found_downloadlinks.next();
                    all_selected_downloadlinks.put(dl_entry.getKey(), dl_entry.getValue());
                }
            } else {
                if (grab_best_out_of_user_selection) {
                    all_selected_downloadlinks = findBESTInsideGivenMap(all_selected_downloadlinks);
                }
            }
        }
        return all_selected_downloadlinks;
    }

    private HashMap<String, DownloadLink> findBESTInsideGivenMap(final HashMap<String, DownloadLink> map_with_all_qualities) {
        HashMap<String, DownloadLink> newMap = new HashMap<String, DownloadLink>();
        DownloadLink keep = null;
        if (map_with_all_qualities.size() > 0) {
            for (final String quality : all_known_qualities) {
                keep = map_with_all_qualities.get(quality);
                if (keep != null) {
                    newMap.put(quality, keep);
                    break;
                }
            }
        }
        if (newMap.isEmpty()) {
            /* Failover in case of bad user selection or general failure! */
            newMap = map_with_all_qualities;
        }
        return newMap;
    }
}
