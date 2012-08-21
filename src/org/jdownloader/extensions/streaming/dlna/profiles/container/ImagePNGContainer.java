package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class ImagePNGContainer extends AbstractImageContainer {
    public static final ImagePNGContainer INSTANCE = new ImagePNGContainer();

    protected ImagePNGContainer() {
        super(Extensions.IMAGE_PNG);
    }
}
