package org.jdownloader.extensions.extraction;

import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.extensions.extraction.translate.T;

public enum CPUPriority implements LabelInterface {
    HIGH(0) {
        @Override
        public String getLabel() {

            return T._.settings_cpupriority_high();
        }
    },
    MIDDLE(5) {
        @Override
        public String getLabel() {

            return T._.settings_cpupriority_middle();
        }
    },
    LOW(10) {
        @Override
        public String getLabel() {

            return T._.settings_cpupriority_low();
        }
    };

    private int time = 0;

    private CPUPriority(int time) {
        this.time = Math.max(time, 0);
    }

    public int getTime() {
        return time;
    }
}