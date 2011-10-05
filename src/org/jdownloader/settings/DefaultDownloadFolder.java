package org.jdownloader.settings;

import java.io.File;

import org.appwork.storage.config.defaults.AbstractDefaultFactory;
import org.appwork.utils.Application;

public class DefaultDownloadFolder extends AbstractDefaultFactory<String> {

    @Override
    public String getDefaultValue() {
        File home = new File(System.getProperty("user.home"));
        if (home.exists() && home.isDirectory()) {
            // new File(home, "downloads").mkdirs();
            return new File(home, "downloads").getAbsolutePath();

        } else {
            return Application.getResource("downloads").getAbsolutePath();

        }
    }

}
