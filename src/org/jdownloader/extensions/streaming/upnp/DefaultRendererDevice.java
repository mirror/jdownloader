package org.jdownloader.extensions.streaming.upnp;

import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.dlna.profiles.video.MPEG4Part2;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;

public class DefaultRendererDevice implements RendererDevice {

    @Override
    public Profile getBestProfile(MediaItem mediaItem) {
        try {
            String profileString = mediaItem.getDlnaProfiles()[0];
            return Profile.ALL_PROFILES_MAP.get(profileString).get(0);
        } catch (Exception e) {

        }
        return null;
    }

    @Override
    public String createDlnaOrgPN(Profile dlnaProfile, MediaItem mediaItem) {
        return dlnaProfile.toString();
    }

    @Override
    public Profile getBestTranscodeProfile(MediaItem mediaItem) {
        return MPEG4Part2.MPEG4_P2_TS_ASP_AAC;
    }

    @Override
    public String createDlnaOrgOP(Profile dlnaProfile, MediaItem mediaItem) {
        return DLNAOp.create(DLNAOp.RANGE_SEEK_SUPPORTED);
    }

    @Override
    public String createDlnaOrgFlags(Profile dlnaProfile, MediaItem mediaItem) {
        return DLNAOrg.create(DLNAOrg.STREAMING_TRANSFER_MODE);
    }

    @Override
    public String createContentType(Profile dlnaProfile, MediaItem mediaItem) {
        return dlnaProfile.getMimeType().getLabel();
    }

    @Override
    public String createHeaderAcceptRanges(Profile dlnaProfile, MediaItem mediaItem) {
        return "bytes";
    }

}
