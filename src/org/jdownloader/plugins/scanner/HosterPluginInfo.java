package org.jdownloader.plugins.scanner;

import java.io.File;

import jd.plugins.HostPlugin;

public class HosterPluginInfo extends PluginInfo {
    private HostPlugin annotation;

    public HostPlugin getAnnotation() {
        return annotation;
    }

    public void setAnnotation(HostPlugin annotation) {
        this.annotation = annotation;
    }

    public HosterPluginInfo(File file, Class<?> clazz, HostPlugin a) {
        super(file, clazz);

        annotation = a;

    }
}
