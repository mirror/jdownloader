package jd.controlling.linkcollector;

import org.jdownloader.translate._JDT;

public enum LinkOrigin {
    ADD_LINKS_DIALOG(_JDT.T.LinkSource_ADD_LINKS_DIALOG()),
    CLIPBOARD(_JDT.T.LinkSource_CLIPBOARD()),
    ADD_CONTAINER_ACTION(_JDT.T.LinkSource_ADD_CONTAINER_ACTION()),
    START_PARAMETER(_JDT.T.LinkSource_START_PARAMETER()),
    MAC_DOCK(_JDT.T.LinkSource_MAC_DOCK()),
    MYJD(_JDT.T.LinkSource_MYJD()),
    CNL(_JDT.T.LinkSource_CNL()),
    FLASHGOT(_JDT.T.LinkSource_FLASHGOT()),
    TOOLBAR(_JDT.T.LinkSource_TOOLBAR()),
    DRAG_DROP_ACTION(_JDT.T.LinkSource_DRAG_DROP_ACTION()),
    PASTE_LINKS_ACTION(_JDT.T.LinkSource_PASTE_LINKS_ACTION()),
    DOWNLOADED_CONTAINER(_JDT.T.LinkSource_DOWNLOADED_CONTAINER()),
    EXTENSION(_JDT.T.LinkSource_EXTENSION());

    private final String            translation;
    private final LinkOriginDetails linkOriginDetails;

    private LinkOrigin(String translation) {
        this.translation = translation;
        this.linkOriginDetails = new LinkOriginDetails(this, null);
    }

    public final String getTranslation() {
        return translation;
    }

    public final LinkOriginDetails getLinkOriginDetails() {
        return linkOriginDetails;
    }

}
