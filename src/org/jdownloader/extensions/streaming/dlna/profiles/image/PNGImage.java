package org.jdownloader.extensions.streaming.dlna.profiles.image;

import org.jdownloader.extensions.streaming.dlna.MimeType;
import org.jdownloader.extensions.streaming.dlna.profiles.IntRange;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.ImagePNGContainer;

public class PNGImage extends AbstractImageProfile {
    // container List musst be initialized first. We need to avoid the autosorter to FU%/& up this so we used this _ Naming scheme
    private static final AbstractMediaContainer[] _CONTAINER  = new AbstractMediaContainer[] { ImagePNGContainer.INSTANCE };
    public final static PNGImage                  PNG_LRG     = new PNGImage("PNG_LRG", new IntRange(1, 4096), new IntRange(1, 4096)) {
                                                                  {
                                                                      depths = new int[] { 1, 2, 4, 8, 16, 24, 32 };
                                                                  }
                                                              };
    public final static PNGImage                  PNG_LRG_ICO = new PNGImage("PNG_LRG_ICO", 120, 120);

    public final static PNGImage                  PNG_SM_ICO  = new PNGImage("PNG_SM_ICO", 48, 48);

    public static void init() {
    }

    protected int[] depths;

    public PNGImage(String id, int width, int height) {
        super(id, width, height);
        containers = _CONTAINER;

    }

    public PNGImage(String id, IntRange width, IntRange height) {
        super(id, width, height);
        containers = _CONTAINER;

    }

    public int[] getDepths() {
        return depths;
    }

    @Override
    public MimeType getMimeType() {
        return MimeType.IMAGE_PNG;
    }

}
