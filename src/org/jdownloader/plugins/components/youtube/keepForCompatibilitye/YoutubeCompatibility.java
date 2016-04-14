package org.jdownloader.plugins.components.youtube.keepForCompatibilitye;

import java.io.File;

import org.appwork.utils.Application;
import org.appwork.utils.FileHandler;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.youtube.keepForCompatibility.YoutubeVariantOld;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.VariantBase;

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

    public static String getTypeID(String oldID) {
        for (YoutubeVariantOld v : YoutubeVariantOld.values()) {
            if (StringUtils.equals(oldID, v.getTypeId())) {
                VariantBase ret = getBaseVariant(v);
                if (ret != null) {
                    return AbstractVariant.get(ret).getTypeId();
                }

            }
        }
        return null;
    }

    public static VariantBase getBaseVariant(String v) {
        return VariantBase.COMPATIBILITY_MAP.get(v);
    }

    public static VariantBase getBaseVariant(YoutubeVariantOld v) {
        return VariantBase.COMPATIBILITY_MAP.get(v.name());
    }

}
