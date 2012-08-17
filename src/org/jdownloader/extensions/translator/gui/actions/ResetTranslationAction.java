package org.jdownloader.extensions.translator.gui.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.translator.TranslateEntry;
import org.jdownloader.extensions.translator.TranslatorExtension;
import org.jdownloader.extensions.translator.TranslatorExtensionEvent;

public class ResetTranslationAction extends AppAction {

    private TranslatorExtension       owner;
    private java.util.List<TranslateEntry> selection;

    public ResetTranslationAction(TranslatorExtension owner, java.util.List<TranslateEntry> selection) {
        this.owner = owner;
        this.selection = selection;
        setName("Reset Changes");
        setIconKey("reset");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (TranslateEntry te : selection) {
            owner.reset(te);
        }
        owner.getEventSender().fireEvent(new TranslatorExtensionEvent(owner, org.jdownloader.extensions.translator.TranslatorExtensionEvent.Type.REFRESH_DATA));

    }

}
