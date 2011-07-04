package org.jdownloader.extensions.translator.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public class NewTranslationAction extends AbstractAction {

    private TranslatorGui master;

    public NewTranslationAction(TranslatorGui translatorGui) {
        super("Create new!");
        master = translatorGui;
    }

    public void actionPerformed(ActionEvent e) {

    }

}
