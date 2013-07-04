package org.jdownloader.api.myjdownloader;

import java.io.File;

import org.appwork.storage.config.annotations.InitHookInterface;

public class MyJDownloaderSettingsInitHook implements InitHookInterface {

    @Override
    public void doHook(File file, Class<?> configInterface) {

        // rename the old jsonfile. because we moved the INterface, the name changed
        File jsonFile = new File(file.getAbsolutePath() + ".json");
        if (jsonFile.exists()) return;
        File oldFile = new File(jsonFile.getParentFile(), "org.jdownloader.extensions.myjdownloader.MyJDownloaderExtension.json");
        if (oldFile.exists()) {
            oldFile.renameTo(jsonFile);
        }
    }

    @Override
    public void doHook(String classPath, Class<?> configInterface) {
        System.out.println(1);
    }

    // ""
}
