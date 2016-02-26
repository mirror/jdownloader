package jd.gui.swing.dialog;

import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;

public class InputOKButtonAdapter {
    protected static final class MessageOnDisabledClickListener extends MouseAdapter {
        private AbstractDialog<?> dialog;

        public MessageOnDisabledClickListener(AbstractDialog<?> dialog) {
            this.dialog = dialog;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (dialog.getOkButton().isEnabled()) {
                dialog.getOkButton().removeMouseListener(this);
                return;
            }
            Toolkit.getDefaultToolkit().beep();
            Dialog.getInstance().showErrorDialog(_GUI.T.add_or_edit_account_dialog_ok_button_tooltip_bad_input());
        }
    }

    /**
     * @param addAccountDialog
     * @param accountBuilderUI
     */

    public static void register(final AbstractDialog<?> dialog, final AccountBuilderInterface accountBuilderUI) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (accountBuilderUI == null || dialog.getOkButton() == null) {
                    return;
                }
                boolean wasEnabled = dialog.getOkButton().isEnabled();
                dialog.getOkButton().setEnabled(accountBuilderUI.validateInputs());

                if (wasEnabled && !dialog.getOkButton().isEnabled()) {
                    dialog.getOkButton().setToolTipText(_GUI.T.add_or_edit_account_dialog_ok_button_tooltip_bad_input());
                    for (MouseListener ll : dialog.getOkButton().getMouseListeners()) {
                        if (ll instanceof MessageOnDisabledClickListener) {
                            dialog.getOkButton().removeMouseListener(ll);
                        }

                    }
                    dialog.getOkButton().addMouseListener(new MessageOnDisabledClickListener(dialog));
                } else if (!wasEnabled && dialog.getOkButton().isEnabled()) {
                    dialog.getOkButton().setToolTipText(null);
                    for (MouseListener ll : dialog.getOkButton().getMouseListeners()) {
                        if (ll instanceof MessageOnDisabledClickListener) {
                            dialog.getOkButton().removeMouseListener(ll);
                        }

                    }

                }
            }
        };
    }
}