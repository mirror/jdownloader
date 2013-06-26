package org.jdownloader.updatev2;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.appwork.resources.AWUTheme;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DefaultButtonPanel;

public class UpdateFoundDialog extends ConfirmDialog {

    private AbstractAction laterAction = null;
    private AbstractAction nowAction   = null;

    public UpdateFoundDialog(final Runnable later, final Runnable now) {
        super(UIOManager.LOGIC_COUNTDOWN | UIOManager.BUTTONS_HIDE_OK, _UPDATE._.update_dialog_title_updates_available(), _UPDATE._.update_dialog_msg_x_updates_available(), AWUTheme.I().getIcon("updatericon", 32), null, _UPDATE._.update_dialog_cancel());
        this.setCountdownTime(60);
        if (later != null) {
            this.laterAction = new AbstractAction(_UPDATE._.update_dialog_later()) {

                /**
             * 
             */
                private static final long serialVersionUID = 1L;

                public void actionPerformed(final ActionEvent e) {
                    UpdateFoundDialog.this.setReturnmask(true);
                    later.run();
                    UpdateFoundDialog.this.dispose();
                }

            };
        }
        if (now != null) {
            this.nowAction = new AbstractAction(_UPDATE._.update_dialog_yes()) {

                /**
             * 
             */
                private static final long serialVersionUID = 1L;

                public void actionPerformed(final ActionEvent e) {
                    UpdateFoundDialog.this.setReturnmask(true);
                    now.run();
                    UpdateFoundDialog.this.dispose();
                }

            };
        }
    }

    @Override
    protected DefaultButtonPanel getDefaultButtonPanel() {
        final DefaultButtonPanel ret = new DefaultButtonPanel("ins 0", "[]", "0[]0");

        if (this.laterAction != null) {
            ret.add(new JButton(this.laterAction), "alignx right,tag ok,sizegroup confirms,growx,pushx");
        }
        if (this.nowAction != null) {
            ret.add(new JButton(this.nowAction), "alignx right,tag ok,sizegroup confirms,growx,pushx");
        }

        return ret;

    }

    @Override
    public JComponent layoutDialogContent() {
        final JComponent txt = super.layoutDialogContent();
        final JPanel ret = new JPanel(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow, fill][]"));
        ret.add(txt);

        final JButton btn = new JButton(_UPDATE._.update_dialog_news_button());
        btn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                UpdateFoundDialog.this.cancel();
                CrossSystem.openURLOrShowMessage(_UPDATE._.update_dialog_news_button_url());

            }
        });
        btn.setContentAreaFilled(false);
        SwingUtils.toBold(btn);
        btn.setVisible(!StringUtils.isEmpty(_UPDATE._.update_dialog_news_button_url()));
        btn.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ret.getBackground().darker()));
        ret.add(Box.createHorizontalGlue(), "split 2,growx,pushx");
        ret.add(btn, "");
        btn.setFocusable(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return ret;
    }
}
