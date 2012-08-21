package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class ImageJPEGContainer extends AbstractImageContainer {
    public static final ImageJPEGContainer INSTANCE = new ImageJPEGContainer();

    protected ImageJPEGContainer() {
        super(Extensions.IMAGE_JPG, Extensions.IMAGE_JPE, Extensions.IMAGE_JPEG);
    }

}
