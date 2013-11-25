package org.jdownloader.settings.advanced;

import org.appwork.storage.Storable;

public interface AdvancedAction extends Storable {
    public String getName();

    public void actionPerformed();
}
