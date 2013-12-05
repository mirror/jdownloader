package org.jdownloader.settings;

import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.gui.translate._GUI;

public enum IfFileExistsAction implements LabelInterface {
    OVERWRITE_FILE() {

        @Override
        public String getLabel() {
            return _GUI._.overwrite();
        }

    },
    SKIP_FILE() {

        @Override
        public String getLabel() {
            return _GUI._.skip_file();
        }

    },
    AUTO_RENAME() {

        @Override
        public String getLabel() {
            return _GUI._.auto_rename();
        }

    },
    ASK_FOR_EACH_FILE() {

        @Override
        public String getLabel() {
            return _GUI._.ask();
        }

    }
}
