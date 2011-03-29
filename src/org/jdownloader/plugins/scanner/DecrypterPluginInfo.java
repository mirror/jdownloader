package org.jdownloader.plugins.scanner;

import java.io.File;

import jd.plugins.DecrypterPlugin;

public class DecrypterPluginInfo extends PluginInfo {
    private DecrypterPlugin annotation;

    public DecrypterPlugin getAnnotation() {
        return annotation;
    }

    public void setAnnotation(DecrypterPlugin annotation) {
        this.annotation = annotation;
    }

    public DecrypterPluginInfo(File file, Class<?> clazz, DecrypterPlugin a) {
        super(file, clazz);

        annotation = a;

    }
}
