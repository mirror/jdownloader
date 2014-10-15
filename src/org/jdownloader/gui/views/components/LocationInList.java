package org.jdownloader.gui.views.components;

import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.translate._JDT;

public enum LocationInList implements LabelInterface {

    END_OF_LIST {
        @Override
        public String getLabel() {
            return _JDT._.LocationInList_getLabel_END_OF_LIST();
        }
    },

    TOP_OF_LIST {
        @Override
        public String getLabel() {
            return _JDT._.LocationInList_getLabel_TOP_OF_LIST();
        }
    },

    AFTER_SELECTION {
        @Override
        public String getLabel() {
            return _JDT._.LocationInList_getLabel_AFTER_SELECTION();
        }
    },

    BEFORE_SELECTION {
        @Override
        public String getLabel() {
            return _JDT._.LocationInList_getLabel_BEFORE_SELECTION();
        }
    };

}