package jd.controlling.linkcollector;

import org.jdownloader.translate._JDT;

public enum LinkOrigin {
    ADD_LINKS_DIALOG(_JDT._.LinkSource_ADD_LINKS_DIALOG()),
    CLIPBOARD(_JDT._.LinkSource_CLIPBOARD()),
    ADD_CONTAINER_ACTION(_JDT._.LinkSource_ADD_CONTAINER_ACTION()),
    START_PARAMETER(_JDT._.LinkSource_START_PARAMETER()),
    MAC_DOCK(_JDT._.LinkSource_MAC_DOCK()),
    MYJD(_JDT._.LinkSource_MYJD()),
    CNL(_JDT._.LinkSource_CNL()),
    FLASHGOT(_JDT._.LinkSource_FLASHGOT()),
    TOOLBAR(_JDT._.LinkSource_TOOLBAR()),
    PASTE_LINKS_ACTION(_JDT._.LinkSource_PASTE_LINKS_ACTION()),
    DOWNLOADED_CONTAINER(_JDT._.LinkSource_DOWNLOADED_CONTAINER()),
    EXTENSION(_JDT._.LinkSource_EXTENSION());

    private String translation;

    private LinkOrigin(String translation) {
        this.translation = translation;
    }

    public String getTranslation() {
        return translation;
    }

}
