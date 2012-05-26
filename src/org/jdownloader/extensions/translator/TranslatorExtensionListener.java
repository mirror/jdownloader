package org.jdownloader.extensions.translator;

import java.util.EventListener;

public interface TranslatorExtensionListener extends EventListener {

    void onLngRefresh(TranslatorExtensionEvent event);

    void refresh();

}