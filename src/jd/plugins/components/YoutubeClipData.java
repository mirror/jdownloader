package jd.plugins.components;

public class YoutubeClipData {

    /**
     * 
     */

    public String  user;
    public String  channel;
    public long    date;
    public String  error;
    public boolean ageCheck;
    public String  title;
    public String  videoID;
    public int     playlistEntryNumber;
    public int     length;
    public String  category;
    public int     duration;
    public String  channelID;
    public long    dateUpdated;
    public String  userGooglePlusID;

    public YoutubeClipData(final String videoID) {
        this(videoID, -1);
    }

    @Override
    public String toString() {
        return videoID + "/" + title;
    }

    public YoutubeClipData(final String videoID, final int playlistEntryNumber) {

        this.videoID = videoID;
        this.playlistEntryNumber = playlistEntryNumber;
    }

}