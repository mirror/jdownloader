package org.jdownloader.settings;

import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.gui.translate._GUI;

public enum IfFileExistsAction implements LabelInterface {
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
    AUTO_RENAME() {

        @Override
        public String getLabel() {
            return _GUI.T.auto_rename();
        }

    },
    ASK_FOR_EACH_FILE() {

        @Override
        public String getLabel() {
            return _GUI.T.ask();
        }

    }
}
