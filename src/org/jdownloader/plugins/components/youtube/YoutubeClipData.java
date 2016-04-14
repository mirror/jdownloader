package org.jdownloader.plugins.components.youtube;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;
import org.jdownloader.plugins.components.youtube.variants.YoutubeSubtitleStorable;

import jd.plugins.DownloadLink;

public class YoutubeClipData {

    /**
     *
     */

    public String                                    user;
    public String                                    channel;
    public long                                      date;
    public String                                    error;
    public boolean                                   ageCheck;
    public String                                    title;
    public String                                    videoID;
    public int                                       playlistEntryNumber;
    public int                                       length;
    public String                                    category;
    public int                                       duration;
    public String                                    channelID;
    public long                                      dateUpdated;
    public String                                    userGooglePlusID;
    public YoutubeITAG                               bestVideoItag;
    public String                                    description;
    public Map<YoutubeITAG, List<YoutubeStreamData>> streams;
    public ArrayList<YoutubeSubtitleStorable>        subtitles;
    public HashMap<String, String>                   keywords3D;
    public HashSet<String>                           keywords;
    public String                                    approxThreedLayout;

    public YoutubeClipData(final String videoID) {
        this(videoID, -1);
    }

    /**
     * Checks the keywords and guesses if this is a sbs or hou 3d video, but not declared as 3d by youtube
     *
     * @return
     */
    public boolean guessSBSorHOU3D() {
        if (keywords != null) {

            StringBuilder sb = new StringBuilder();
            for (String s : keywords) {
                sb.append(" ").append(s.toLowerCase(Locale.ENGLISH));

            }
            if (title != null) {
                sb.append(" ").append(title.toLowerCase(Locale.ENGLISH));
            }
            if (description != null) {
                sb.append(" ").append(description.toLowerCase(Locale.ENGLISH));
            }

            String str = sb.toString();
            if (str.contains("3d")) {
                if (str.contains("sbs")) {
                    return true;
                }
                if (str.contains("side") && str.contains("by")) {
                    return true;
                }
                if (str.contains(" ou")) {
                    return true;
                }
                if (str.contains("hou")) {
                    return true;
                }
                if (str.contains("cardboard")) {
                    return true;
                }
            }

        }
        return false;
    }

    public boolean is3D() {
        // from yt player js
        if ("1".equals(approxThreedLayout)) {
            return true;
        }
        if (keywords != null) {
            if (keywords.contains("3D")) {
                return true;
            }
            StringBuilder sb = new StringBuilder();
            for (String s : keywords) {
                sb.append(" ").append(s.toLowerCase(Locale.ENGLISH));

            }
            String str = sb.toString();
            if (str.contains("3d")) {
                if (str.contains("sbs")) {
                    return true;
                }
                if (str.contains("side") && str.contains("by")) {
                    return true;
                }
                if (str.contains(" ou")) {
                    return true;
                }
                if (str.contains("hou")) {
                    return true;
                }
            }

        }
        if (keywords3D != null) {
            if (StringUtils.equals(keywords3D.get("enable"), "true")) {
                return true;
            }
            if (StringUtils.equals(keywords3D.get("enable"), "LR")) {
                return true;
            }
            if (StringUtils.equals(keywords3D.get("enable"), "RL")) {
                return true;
            }
        }
        return guessSBSorHOU3D();
    }

    @Override
    public String toString() {
        return videoID + "/" + title;
    }

    public YoutubeClipData(final String videoID, final int playlistEntryNumber) {

        this.videoID = videoID;
        this.playlistEntryNumber = playlistEntryNumber;
    }

    public void copyToDownloadLink(final DownloadLink thislink) {
        thislink.setProperty(YoutubeHelper.YT_TITLE, title);
        thislink.setProperty(YoutubeHelper.YT_PLAYLIST_INT, playlistEntryNumber);

        thislink.setProperty(YoutubeHelper.YT_CHANNEL, channel);
        thislink.setProperty(YoutubeHelper.YT_USER, user);
        thislink.setProperty(YoutubeHelper.YT_BEST_VIDEO, bestVideoItag == null ? null : bestVideoItag.name());
        thislink.setProperty(YoutubeHelper.YT_DATE, date);
        thislink.setProperty(YoutubeHelper.YT_LENGTH_SECONDS, length);
        thislink.setProperty(YoutubeHelper.YT_GOOGLE_PLUS_ID, userGooglePlusID);
        thislink.setProperty(YoutubeHelper.YT_CHANNEL_ID, channelID);
        thislink.setProperty(YoutubeHelper.YT_DURATION, duration);
        thislink.setProperty(YoutubeHelper.YT_DATE_UPDATE, dateUpdated);
        thislink.getTempProperties().setProperty(YoutubeHelper.YT_DESCRIPTION, description);
        thislink.getTempProperties().setProperty(YoutubeHelper.YT_FULL_STREAM_INFOS, this);
    }

    public List<YoutubeStreamData> getStreams(YoutubeITAG itag) {
        if (itag == null) {
            return null;
        }
        List<YoutubeStreamData> ret = streams.get(itag);
        if (ret == null) {
            // check alternatives
            List<YoutubeITAG> alternatives = YoutubeITAG.getTagList(itag.getITAG());
            if (alternatives != null) {
                for (YoutubeITAG tag : alternatives) {
                    ret = streams.get(tag);
                    if (ret != null) {
                        break;
                    }
                }
            }
        }
        return ret;
    }

}