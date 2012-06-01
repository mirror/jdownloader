package org.jdownloader.extensions;

import java.util.EventListener;

public interface ExtensionControllerListener extends EventListener {
    public void onUpdated();
}