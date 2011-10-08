package org.jdownloader.premium;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class BuyAndAddPremiumAccount extends AbstractDialog<Boolean> implements BuyAndAddPremiumDialogInterface {

    private DomainInfo info;

    public BuyAndAddPremiumAccount(DomainInfo info) {
        super(0, "", null, null, null);
        this.info = info;
    }

    @Override
    protected Boolean createReturnValue() {
        return null;
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
                final Graphics2D g2 = (Graphics2D) g;

                double faktor = Math.max((double) back.getWidth(null) / getWidth(), (double) back.getHeight(null) / getHeight());
                int width = Math.max((int) (back.getWidth(null) / faktor), 1);
                int height = Math.max((int) (back.getHeight(null) / faktor), 1);
                if (back != null) g2.drawImage(back, 0, getHeight() - height, width, getHeight(), 0, 0, back.getWidth(null), back.getHeight(null), getBackground(), null);

            }
        });
    }

    @Override
    public JComponent layoutDialogContent() {

        final MigPanel ret = new MigPanel("ins 0,wrap 1", "[]", "[]");
        final Image logo = NewTheme.I().hasIcon("fav/large." + info.getTld()) ? NewTheme.I().getIcon("fav/large." + info.getTld(), -1).getImage() : null;

        if (logo != null) {
            JLabel ico = new JLabel(new ImageIcon(logo));
            ret.add(ico);
        }

        ret.add(header(_GUI._.BuyAndAddPremiumAccount_layoutDialogContent_get()), "gapleft 15,pushx,growx");
        ExtButton bt = new ExtButton(new OpenURLAction(info));
        ret.add(bt, "alignx center");
        ret.add(header(_GUI._.BuyAndAddPremiumAccount_layoutDialogContent_enter()), "gapleft 15,pushx,growx");

        return ret;
    }

    private JComponent header(String buyAndAddPremiumAccount_layoutDialogContent_get) {
        JLabel ret = SwingUtils.toBold(new JLabel(buyAndAddPremiumAccount_layoutDialogContent_get));
        ret.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ret.getForeground()));
        return ret;
    }
}
