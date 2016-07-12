//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;

import jd.controlling.TaskQueue;
import jd.controlling.accountchecker.AccountChecker;
import jd.gui.swing.dialog.InputOKButtonAdapter;
import jd.plugins.Account;
import net.miginfocom.swing.MigLayout;

public class EditAccountDialog extends AbstractDialog<Integer> implements InputChangedCallbackInterface {
    private final Account           acc;
    private AccountBuilderInterface accountBuilderUI;
    private JPanel                  content;

    public EditAccountDialog(Account acc) {
        super(0, _GUI.T.jd_gui_swing_components_AccountDialog_edit_title(), DomainInfo.getInstance(acc.getHosterByPlugin()).getFavIcon(), _GUI.T.lit_save(), null);
        this.acc = acc;
    }

    @Override
    protected Integer createReturnValue() {
        return this.getReturnmask();
    }

    protected void initFocus(final JComponent focus) {
    }

    @Override
    public JComponent layoutDialogContent() {
        content = new JPanel(new MigLayout("ins 0, wrap 1", "[grow,fill]"));
        getDialog().addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowLostFocus(final WindowEvent windowevent) {
                // TODO Auto-generated method stub
            }

            @Override
            public void windowGainedFocus(final WindowEvent windowevent) {
                final Component focusOwner = getDialog().getFocusOwner();
                if (focusOwner != null) {
                    // dialog component has already focus...
                    return;
                }
                /* we only want to force focus on first window open */
                getDialog().removeWindowFocusListener(this);
            }
        });
        final AccountBuilderInterface accountFactory = acc.getPlugin().getAccountFactory(this);
        accountBuilderUI = accountFactory;
        content.add(accountBuilderUI.getComponent(), "gapleft 32,spanx,wmin 450");
        accountBuilderUI.setAccount(acc);
        onChangedInput(null);
        getDialog().pack();
        return content;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            if (accountBuilderUI.validateInputs()) {
                final Account newAcc = accountBuilderUI.getAccount();
                TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                    @Override
                    protected Void run() throws RuntimeException {
                        if (accountBuilderUI.updateAccount(newAcc, acc)) {
                            AccountChecker.getInstance().check(acc, true);
                        }
                        return null;
                    }
                });
                super.actionPerformed(e);
            }
        } else {
            super.actionPerformed(e);
        }
    }

    @Override
    protected void packed() {
        super.packed();
    }

    @Override
    public void onChangedInput(Object component) {
        InputOKButtonAdapter.register(this, accountBuilderUI);
    }
}