package org.jdownloader.extensions.translator.gui.actions;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.translator.TLocale;
import org.jdownloader.extensions.translator.TranslateEntry;
import org.jdownloader.extensions.translator.TranslatorExtension;

public class UseDefaultAction extends AppAction {

    private TranslatorExtension       owner;
    private java.util.List<TranslateEntry> selection;
    private static TLocale            PRE;

    public UseDefaultAction(TranslatorExtension owner, java.util.List<TranslateEntry> selection) {
        this.owner = owner;
        this.selection = selection;
        setName("Use Original/Default Translation");
        setIconKey("copy");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        owner.setDefault(selection);

    }
}
