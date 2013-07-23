package org.jdownloader.updatev2;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;


import org.appwork.utils.swing.dialog.DefaultButtonPanel;
import org.jdownloader.actions.AppAction;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

public class ConfirmUpdateDialog extends org.appwork.utils.swing.dialog.ConfirmDialog {

    protected boolean closedBySkipUntilNextRestart = false;

    public ConfirmUpdateDialog(int flags, String title, String message, ImageIcon ic, String ok, String no) {
        super(flags, title, message, ic, ok, no);
    }

    protected DefaultButtonPanel createBottomButtonPanel() {

        return new DefaultButtonPanel("ins 0", "[]", "0[grow,fill]0") {

            @Override
            public void addCancelButton(final JButton cancelButton) {
                super.addCancelButton(cancelButton);

                final JButton bt = new JButton(NewTheme.I().getIcon("popdownButton", -1)) {

                    public void setBounds(int x, int y, int width, int height) {
                        int delta = 5;
                        super.setBounds(x - delta, y, width + delta, height);
                    }

                };

                bt.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        createPopup();
                    }

                });
                super.add(bt, "gapleft 0,width 8!");
            }

        };
    }

    private void createPopup() {
        final JPopupMenu popup = new JPopupMenu();
        JMenuItem mi = new JMenuItem(new AppAction() {
            {
                setName(_UPDATE._.update_in_next_session());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                closedBySkipUntilNextRestart = true;
                ConfirmUpdateDialog.this.dispose();
            }

        });

        popup.add(mi);

        int[] insets = LAFOptions.getInstance().getPopupBorderInsets();

        Dimension pref = popup.getPreferredSize();
        pref.height = 24 + insets[0] + insets[2];

        popup.setPreferredSize(pref);
        popup.show(cancelButton, +insets[1] - pref.width + cancelButton.getWidth() + 8 + 5, +cancelButton.getHeight());
    }

    public boolean isClosedBySkipUntilNextRestart() {
        return closedBySkipUntilNextRestart;
    }

    public void setClosedBySkipUntilNextRestart(boolean closedBySkipUntilNextRestart) {
        this.closedBySkipUntilNextRestart = closedBySkipUntilNextRestart;
    }

}
