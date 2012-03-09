package org.jdownloader.premium;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.Launcher;
import jd.gui.swing.dialog.AddAccountDialog;
import jd.gui.swing.laf.LookAndFeelController;
import jd.plugins.Account;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class BuyAndAddPremiumAccount extends AbstractDialog<Boolean> implements BuyAndAddPremiumDialogInterface {

    private DomainInfo       info;
    private ExtTextField     user;
    private ExtPasswordField pass;

    public BuyAndAddPremiumAccount(DomainInfo info) {
        super(0, "", null, null, null);
        this.info = info;
    }

    @Override
    protected Boolean createReturnValue() {
        return null;
    }

    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == this.okButton) {
            Log.L.fine("Answer: Button<OK:" + this.okButton.getText() + ">");

            Account ac = new Account(user.getText(), new String(pass.getPassword()));
            ac.setHoster(info.getTld());
            try {
                if (!AddAccountDialog.addAccount(ac)) {
                    user.requestFocus();
                    user.selectAll();
                    return;
                } else {
                    this.dispose();
                }
            } catch (DialogNoAnswerException e2) {
                this.dispose();
            }
            this.setReturnmask(true);
        } else if (e.getSource() == this.cancelButton) {
            Log.L.fine("Answer: Button<CANCEL:" + this.cancelButton.getText() + ">");
            this.setReturnmask(false);
        }
        this.dispose();
    }

    protected void layoutDialog() {

        final Image back = NewTheme.I().hasIcon("fav/footer." + info.getTld()) ? NewTheme.I().getIcon("fav/footer." + info.getTld(), -1).getImage() : null;
        super.layoutDialog();
        getDialog().setContentPane(new JPanel() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (back != null) {
                    final Graphics2D g2 = (Graphics2D) g;

                    double faktor = Math.max((double) back.getWidth(null) / getWidth(), (double) back.getHeight(null) / getHeight());
                    int width = Math.max((int) (back.getWidth(null) / faktor), 1);
                    int height = Math.max((int) (back.getHeight(null) / faktor), 1);
                    g2.drawImage(back, 0, getHeight() - height, width, getHeight(), 0, 0, back.getWidth(null), back.getHeight(null), getBackground(), null);
                }
            }
        });
    }

    public static void main(String[] args) {
        BuyAndAddPremiumAccount cp;
        try {
            Launcher.statics();

            cp = new BuyAndAddPremiumAccount(DomainInfo.getInstance("filesonic.com"));

            LookAndFeelController.getInstance().setUIManager();

            Dialog.getInstance().showDialog(cp);
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    @Override
    public JComponent layoutDialogContent() {

        final MigPanel ret = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[]");
        final Image logo = NewTheme.I().hasIcon("fav/large." + info.getTld()) ? NewTheme.I().getIcon("fav/large." + info.getTld(), -1).getImage() : null;

        if (logo != null) {
            JLabel ico = new JLabel(new ImageIcon(logo));
            ret.add(ico);
        }

        ret.add(header(_GUI._.BuyAndAddPremiumAccount_layoutDialogContent_get()), "gapleft 15,pushx,growx");
        ExtButton bt = new ExtButton(new OpenURLAction(info));
        ret.add(bt, "gapleft 27");
        ret.add(header(_GUI._.BuyAndAddPremiumAccount_layoutDialogContent_enter()), "gapleft 15,pushx,growx");
        user = new ExtTextField();
        pass = new ExtPasswordField();
        user.setHelpText(_GUI._.BuyAndAddPremiumAccount_layoutDialogContent_username(info.getName()));
        pass.setHelpText(_GUI._.BuyAndAddPremiumAccount_layoutDialogContent_pass());
        ret.add(user, "gapleft 27");
        ret.add(pass, "gapleft 27");
        return ret;
    }

    private JComponent header(String buyAndAddPremiumAccount_layoutDialogContent_get) {
        JLabel ret = SwingUtils.toBold(new JLabel(buyAndAddPremiumAccount_layoutDialogContent_get));
        ret.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ret.getForeground()));
        return ret;
    }
}
