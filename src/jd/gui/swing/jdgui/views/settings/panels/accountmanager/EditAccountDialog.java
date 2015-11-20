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

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.controlling.TaskQueue;
import jd.controlling.accountchecker.AccountChecker;
import jd.plugins.Account;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.accounts.AccountFactory;
import org.jdownloader.plugins.accounts.EditAccountPanel;
import org.jdownloader.plugins.accounts.Notifier;

public class EditAccountDialog extends AbstractDialog<Integer> {

    private final Account    acc;

    private EditAccountPanel editAccountPanel;

    private JPanel           content;

    public EditAccountDialog(Account acc) {
        super(0, _GUI._.jd_gui_swing_components_AccountDialog_edit_title(), DomainInfo.getInstance(acc.getHoster()).getFavIcon(), _GUI._.lit_save(), null);
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
        final AccountFactory accountFactory = acc.getPlugin().getAccountFactory();
        editAccountPanel = accountFactory.getPanel();
        content.add(editAccountPanel.getComponent(), "gapleft 32,spanx");
        editAccountPanel.setAccount(acc);
        editAccountPanel.setNotifyCallBack(new Notifier() {

            @Override
            public void onNotify() {
                checkOK();
            }

        });
        checkOK();
        getDialog().pack();
        return content;
    }

    protected int getPreferredWidth() {
        return 450;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            if (editAccountPanel.validateInputs()) {
                final Account newAcc = editAccountPanel.getAccount();
                TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                    @Override
                    protected Void run() throws RuntimeException {
                        boolean changed = false;
                        if (!StringUtils.equals(newAcc.getUser(), acc.getUser())) {
                            acc.setUser(newAcc.getUser());
                            changed = true;
                        }
                        if (!StringUtils.equals(newAcc.getPass(), acc.getPass())) {
                            acc.setPass(newAcc.getPass());
                            changed = true;
                        }
                        if (changed) {
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

    private JComponent header(String buyAndAddPremiumAccount_layoutDialogContent_get) {
        JLabel ret = SwingUtils.toBold(new JLabel(buyAndAddPremiumAccount_layoutDialogContent_get));
        ret.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ret.getForeground()));
        return ret;
    }

    private void checkOK() {
        editAccountPanel.validateInputs();
    }

    @Override
    protected void packed() {
        super.packed();
    }

}