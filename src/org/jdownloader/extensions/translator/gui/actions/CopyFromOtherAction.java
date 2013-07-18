package org.jdownloader.extensions.translator.gui.actions;

import java.awt.event.ActionEvent;

import org.appwork.utils.swing.dialog.ComboBoxDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.translator.TLocale;
import org.jdownloader.extensions.translator.TranslateEntry;
import org.jdownloader.extensions.translator.TranslatorExtension;
import org.jdownloader.images.NewTheme;

public class CopyFromOtherAction extends AppAction {

    private TranslatorExtension       owner;
    private java.util.List<TranslateEntry> selection;
    private static TLocale            PRE;

    public CopyFromOtherAction(TranslatorExtension owner, java.util.List<TranslateEntry> selection) {
        this.owner = owner;
        this.selection = selection;
        setName("Get Translation from existing Translation");
        setIconKey("copy");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final ComboBoxDialog d = new ComboBoxDialog(0, "Clone Translation", "Choose the Language you want to clone", owner.getTranslations().toArray(new TLocale[] {}), PRE == null ? 0 : owner.getTranslations().indexOf(PRE), NewTheme.I().getIcon("language", 32), null, null, null);

        int ret;
        try {
            ret = Dialog.getInstance().showDialog(d);

            if (ret >= 0) {
                TLocale sel = owner.getTranslations().get(ret);
                PRE = sel;

                owner.setTranslation(selection, sel);

            }
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }
}
