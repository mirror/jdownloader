package org.jdownloader.extensions.extraction;

import org.appwork.storage.config.ConfigUtils;

public class CFG_EXTRACTION {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(ExtractionConfig.class, "Application.getResource(\"cfg/\" + " + ExtractionConfig.class.getSimpleName() + ".class.getName())");
    }

}