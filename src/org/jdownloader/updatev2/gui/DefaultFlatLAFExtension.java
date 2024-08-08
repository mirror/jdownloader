package org.jdownloader.updatev2.gui;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.utils.os.CrossSystem;

public class DefaultFlatLAFExtension extends DefaultEmptyLookAndFeelExtension {
    public DefaultFlatLAFExtension(final String laf) {
        super(laf);
    }

    @Override
    @DefaultBooleanValue(true)
    public boolean isWindowDecorationEnabled() {
        if (CrossSystem.isWindows()) {
            return true;
        } else {
            return false;
        }
    }
}
