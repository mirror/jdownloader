package org.jdownloader.plugins.components.youtube.choosevariantdialog;

import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;

public interface Filter {

    boolean isBlacklisted(AbstractVariant variant);

}
