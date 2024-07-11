package org.jdownloader.settings;

import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.gui.translate._GUI;

public enum IfFilenameTooLongAction implements LabelInterface {
    OVERWRITE_FILE() {
        @Override
        public String getLabel() {
            return _GUI.T.overwrite();
        }
    },
    SKIP_FILE() {
        @Override
        public String getLabel() {
            return _GUI.T.skip_file();
        }
    },
    AUTO_SHORTEN() {
        @Override
        public String getLabel() {
            return _GUI.T.auto_shorten();
        }
    },
    ASK_FOR_EACH_FILE() {
        @Override
        public String getLabel() {
            return _GUI.T.ask();
        }
    }
}
