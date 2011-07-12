package org.jdownloader.extensions.translator.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.extensions.translator.TLocale;
import org.jdownloader.extensions.translator.gui.TranslatorGui;

public class LoadTranslationAction extends AbstractAction {

    private TranslatorGui master;
    private TLocale       locale;

    public LoadTranslationAction(TranslatorGui translatorGui, TLocale t) {
        super(t.toString());
        locale = t;
        master = translatorGui;
    }

    @Override
    public boolean isEnabled() {
        return master.getLoaded() != locale;
    }

    public void actionPerformed(ActionEvent e) {
        master.load(locale);
        master.refresh();
    }

}
