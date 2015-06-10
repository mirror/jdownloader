package org.jdownloader.extensions;

import org.jdownloader.controlling.contextmenu.ClassCurrentlyNotAvailableException;

public class ExtensionNotLoadedException extends ClassCurrentlyNotAvailableException {

    public ExtensionNotLoadedException(String className) {
        super("Cannot load class. Extension not loaded: " + className);
    }

}
