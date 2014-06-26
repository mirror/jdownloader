package org.jdownloader.gui.views.components;

import org.appwork.storage.config.annotations.EnumLabel;

public enum LocationInList {
    @EnumLabel("The end of the list")
    END_OF_LIST,
    @EnumLabel("The top of the list")
    TOP_OF_LIST,
    @EnumLabel("After selection")
    AFTER_SELECTION,
    @EnumLabel("Before selection")
    BEFORE_SELECTION;
}