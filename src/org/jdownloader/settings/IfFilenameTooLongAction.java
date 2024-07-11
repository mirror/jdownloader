package org.jdownloader.settings;

import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.gui.translate._GUI;

public enum IfFilenameTooLongAction implements LabelInterface {
    SKIP_FILE() {
        @Override
        public String getLabel() {
            return _GUI.T.skip_file();
        }
    },
    RENAME_FILE() {
        @Override
        public String getLabel() {
            return _GUI.T.too_long_filename_use_shortened_filename();
        }
    }
}
