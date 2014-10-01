package org.jdownloader.extensions.schedulerV2.gui;

import org.appwork.swing.components.SizeSpinner;
import org.appwork.utils.formatter.SizeFormatter;

public class SpeedSpinner extends SizeSpinner {

    public SpeedSpinner(long min, long max, long steps) {
        super(min, max, steps);
    }

    @Override
    protected String longToText(final long longValue) {
        return SizeFormatter.formatBytes(longValue) + "/s";
    }

    @Override
    protected Object textToObject(final String text) {
        return SizeFormatter.getSize(text.replace("/s", ""), true, true);
    }

}
