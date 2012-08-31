package org.jdownloader.extensions.streaming.upnp;

import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;

public interface RendererDevice {

    public Profile getBestProfile(MediaItem mediaItem);

    public String createDlnaOrgPN(Profile dlnaProfile, MediaItem mediaItem);

    public Profile getBestTranscodeProfile(MediaItem mediaItem);

    public String createDlnaOrgOP(Profile dlnaProfile, MediaItem mediaItem);

    public String createDlnaOrgFlags(Profile dlnaProfile, MediaItem mediaItem);

    public String createContentType(Profile dlnaProfile, MediaItem mediaItem);

    public String createHeaderAcceptRanges(Profile dlnaProfile, MediaItem mediaItem);

}
