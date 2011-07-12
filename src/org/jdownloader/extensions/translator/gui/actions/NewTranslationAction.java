package org.jdownloader.extensions.translator.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.extensions.translator.gui.TranslatorGui;

public class NewTranslationAction extends AbstractAction {

    private TranslatorGui master;

    public NewTranslationAction(TranslatorGui translatorGui) {
        super("Create new!");
        master = translatorGui;
    }

    public void actionPerformed(ActionEvent e) {

    }

}
