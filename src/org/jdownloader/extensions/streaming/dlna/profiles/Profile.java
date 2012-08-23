package org.jdownloader.extensions.streaming.dlna.profiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jdownloader.extensions.streaming.dlna.MimeType;
import org.jdownloader.extensions.streaming.dlna.profiles.audio.AACAudio;
import org.jdownloader.extensions.streaming.dlna.profiles.audio.AC3Audio;
import org.jdownloader.extensions.streaming.dlna.profiles.audio.AMRAudio;
import org.jdownloader.extensions.streaming.dlna.profiles.audio.LPCMAudio;
import org.jdownloader.extensions.streaming.dlna.profiles.audio.MP3Audio;
import org.jdownloader.extensions.streaming.dlna.profiles.audio.WMAAudio;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.image.JPEGImage;
import org.jdownloader.extensions.streaming.dlna.profiles.image.PNGImage;
import org.jdownloader.extensions.streaming.dlna.profiles.video.MPEG1Video;
import org.jdownloader.extensions.streaming.dlna.profiles.video.MPEG4Part10AVC;
import org.jdownloader.extensions.streaming.dlna.profiles.video.MPEG4Part2;
import org.jdownloader.extensions.streaming.dlna.profiles.video.Mpeg2;
import org.jdownloader.extensions.streaming.dlna.profiles.video.WMVVideo;

public abstract class Profile {
    protected MimeType                 mimeType;
    private String                     id;

    protected AbstractMediaContainer[] containers;
    protected String[]                 profileTags;

    public String[] getProfileTags() {
        return profileTags;
    }

    public static final HashMap<String, ArrayList<Profile>> ALL_PROFILES_MAP = new HashMap<String, ArrayList<Profile>>();
    public static final List<Profile>                       ALL_PROFILES     = new ArrayList<Profile>();

    public Profile(String id) {
        this.id = id;

        ArrayList<Profile> lst = ALL_PROFILES_MAP.get(id);
        if (lst == null) {
            lst = new ArrayList<Profile>();
            ALL_PROFILES_MAP.put(id, lst);
        }
        lst.add(this);
        ALL_PROFILES.add(this);

    }

    public MimeType getMimeType() {
        return mimeType;
    }

    public String getProfileID() {
        return id;
    }

    public AbstractMediaContainer[] getContainer() {
        return containers;
    }

    public String toString() {
        return getMimeType().getLabel() + ":DLNA.ORG_PN=" + getProfileID();
    }

    public static void init() {
        JPEGImage.init();
        PNGImage.init();

        AACAudio.init();
        AC3Audio.init();
        AMRAudio.init();
        LPCMAudio.init();
        MP3Audio.init();
        WMAAudio.init();

        MPEG1Video.init();
        Mpeg2.init();
        MPEG4Part2.init();
        MPEG4Part10AVC.init();
        WMVVideo.init();

    }

}
