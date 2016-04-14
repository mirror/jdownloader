package org.jdownloader.plugins.components.youtube.keepForCompatibilitye;

import java.io.File;

import org.appwork.utils.Application;
import org.appwork.utils.FileHandler;
import org.appwork.utils.Files;

public class YoutubeCompatibility {

    public static Object moveJSonFiles(final String newID) {
        File folder = Application.getResource("cfg/plugins/");
        if (folder.exists()) {
            // rename old config files
            Files.walkThroughStructure(new FileHandler<RuntimeException>() {

                @Override
                public void intro(File f) throws RuntimeException {
                }

                @Override
                public boolean onFile(File f, int depths) throws RuntimeException {
                    if (f.getName().startsWith("jd.plugins.hoster.YoutubeDashV2$YoutubeConfig")) {
                        File newFile = new File(f.getParentFile(), f.getName().replace("jd.plugins.hoster.YoutubeDashV2$YoutubeConfig", newID));
                        if (!newFile.exists()) {
                            newFile.getParentFile().mkdirs();
                            f.renameTo(newFile);

                        }
                    }
                    return true;
                }

                @Override
                public void outro(File f) throws RuntimeException {
                }
            }, folder);
        }

        return null;
    }

}
