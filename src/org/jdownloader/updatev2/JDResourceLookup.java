package org.jdownloader.updatev2;

import java.io.File;
import java.net.URL;

import org.appwork.utils.Application;
import org.appwork.utils.Application.ResourceLookup;

public class JDResourceLookup implements ResourceLookup {
    @Override
    public File getResource(String relative) {
        System.out.println("getResource:" + relative);
        return Application.getHomeResource(relative);
    }

    @Override
    public URL getRessourceURL(String relative, boolean preferClasspath) {
        System.out.println("getRessourceURL:" + relative + "|" + preferClasspath);
        return Application.getHomeRessourceURL(relative, preferClasspath);
    }
}
