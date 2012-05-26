package org.jdownloader.extensions.translator;

import org.appwork.utils.event.Eventsender;

public class TranslatorExtensionEventSender extends Eventsender<TranslatorExtensionListener, TranslatorExtensionEvent> {

    @Override
    protected void fireEvent(TranslatorExtensionListener listener, TranslatorExtensionEvent event) {
        switch (event.getType()) {
        case LOADED_TRANSLATION:
            listener.onLngRefresh(event);
            break;
        case REFRESH_DATA:
            listener.refresh();
            break;

        // fill
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}