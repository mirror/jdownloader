package jd.plugins.components.gopro;

import org.appwork.storage.StorableValidatorIgnoresMissingSetter;

@StorableValidatorIgnoresMissingSetter
public enum GoProType {
    Photo,
    Video,
    TimeLapse,
    TimeLapseVideo,
    BurstAsZipPackage("Burst"),
    BurstAsSingleImages("Burst"),
    Livestream,
    LoopedVideo,
    BurstVideo,
    Continuous,
    ExternalVideo,
    Session;

    public final String apiID;

    private GoProType() {
        apiID = name();
    }

    private GoProType(String apiID) {
        this.apiID = apiID;
    }
}
