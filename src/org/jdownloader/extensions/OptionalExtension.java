package org.jdownloader.extensions;

import org.jdownloader.images.AbstractIcon;

public class OptionalExtension {
    private final String extensionID;

    public String getExtensionID() {
        return extensionID;
    }

    public String getIconKey() {
        return iconKey;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isInstalled() {
        return getLazyExtension() != null;
    }

    private final String        iconKey;
    private final String        name;
    private final String        description;
    private final LazyExtension lazyExtension;
    private final AbstractIcon  defaultIcon;
    private volatile boolean    restartRequired = false;

    public boolean isRestartRequired() {
        return restartRequired;
    }

    public void setRestartRequired(boolean restartRequired) {
        this.restartRequired = restartRequired;
    }

    public final LazyExtension getLazyExtension() {
        return lazyExtension;
    }

    public AbstractIcon getIcon() {
        return defaultIcon;
    }

    public AbstractIcon getIcon(int size) {
        return new AbstractIcon(getIconKey(), size);
    }

    public OptionalExtension(final String extensionID, final String iconKey, final String name, final String description, final LazyExtension lazyExtension) {
        this.extensionID = extensionID;
        this.iconKey = iconKey;
        this.name = name;
        this.description = description;
        this.lazyExtension = lazyExtension;
        this.defaultIcon = getIcon(16);
    }
}
