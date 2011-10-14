package org.jdownloader.plugins.controller.crawler;

import org.appwork.storage.Storable;

public class AbstractCrawlerPlugin implements Storable {
    public AbstractCrawlerPlugin(/* STorable */) {
    }

    private String classname;
    private String displayName;

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    private String pattern;

    public AbstractCrawlerPlugin(String className) {
        this.classname = className;
    }

}
