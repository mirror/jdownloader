package org.jdownloader.extensions.streaming.dlna.profiles.image;

import org.jdownloader.extensions.streaming.dlna.MimeType;
import org.jdownloader.extensions.streaming.dlna.profiles.IntRange;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.ImageJPEGContainer;

public class JPEGImage extends AbstractImageProfile {
    private static final AbstractMediaContainer[] _CONTAINER    = new AbstractMediaContainer[] { ImageJPEGContainer.INSTANCE };

    public final static JPEGImage                 JPEG_SM_ICO  = new JPEGImage("JPEG_SM_ICO", 48, 48);
    public final static JPEGImage                 JPEG_LRG_ICO = new JPEGImage("JPEG_LRG_ICO", 120, 120);
    public final static JPEGImage                 JPEG_TN      = new JPEGImage("JPEG_TN", new IntRange(1, 160), new IntRange(1, 160));
    public final static JPEGImage                 JPEG_SM      = new JPEGImage("JPEG_SM", new IntRange(1, 640), new IntRange(1, 480));
    public final static JPEGImage                 JPEG_MED     = new JPEGImage("JPEG_MED", new IntRange(1, 1024), new IntRange(1, 768));
    public final static JPEGImage                 JPEG_LRG     = new JPEGImage("JPEG_LRG", new IntRange(1, 4096), new IntRange(1, 4096));

    public JPEGImage(String id, int width, int height) {
        super(id, width, height);
        containers = _CONTAINER;
    }

    public JPEGImage(String id, IntRange width, IntRange height) {
        super(id, width, height);
        containers = _CONTAINER;

    }

    public static void init() {
    }

    @Override
    public MimeType getMimeType() {
        return MimeType.IMAGE_JPEG;
    }

}
