package org.jdownloader.extensions.streaming.mediaarchive.prepare;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

import javax.imageio.ImageIO;

import jd.plugins.DownloadLink;

import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.images.IconIO;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.mediaarchive.ImageMediaItem;

public class ImageHandler extends ExtensionHandler<ImageMediaItem> {

    @Override
    public ImageMediaItem handle(StreamingExtension extension, DownloadLink dl) {
        ImageMediaItem ret = new ImageMediaItem(dl);

        String id = new UniqueAlltimeID().toString();
        String streamurl = extension.createStreamUrl(id, "imagereader", null);
        FileOutputStream fos = null;
        try {
            extension.addDownloadLink(id, dl);

            BufferedImage image = ImageIO.read(new URL(streamurl).openStream());
            System.out.println(image.getWidth() + " - " + image.getHeight());
            ret.setWidth(image.getWidth());
            ret.setHeight(image.getHeight());

            File thumb = Application.getResource("tmp/streaming/thumbs/" + dl.getUniqueID().toString() + ".png");
            thumb.getParentFile().mkdirs();
            thumb.delete();
            fos = new FileOutputStream(thumb);
            ImageIO.write(IconIO.getScaledInstance(image, 300, 300), "png", fos);
            ret.setThumbnailPath(Files.getRelativePath(Application.getResource("tmp").getParentFile(), thumb));
            return ret;
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (final Throwable e) {
            }
            extension.removeDownloadLink(id);
        }
        return null;

    }

}
