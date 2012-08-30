package org.jdownloader.extensions.streaming.dlna.profiles.image;

import org.jdownloader.extensions.streaming.dlna.profiles.IntRange;
import org.jdownloader.extensions.streaming.dlna.profiles.Profile;

public abstract class AbstractImageProfile extends Profile {
    private final IntRange width;

    public IntRange getWidth() {
        return width;
    }

    public IntRange getHeight() {
        return height;
    }

    private final IntRange height;

    public AbstractImageProfile(String id, int width, int height) {
        this(id, new IntRange(width), new IntRange(height));
    }

    public AbstractImageProfile(String id, IntRange width, IntRange height) {
        super(id);

        this.width = width;
        this.height = height;

    }

    public boolean checkResolution(int width, int height) {
        return this.width.contains(width) && this.height.contains(height);
    }

}
