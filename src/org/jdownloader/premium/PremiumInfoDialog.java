package org.jdownloader.premium;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.plugins.PluginForHost;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class PremiumInfoDialog extends AbstractDialog<Object> {
    private final DomainInfo info;
    private final String     id;

    public PremiumInfoDialog(DomainInfo hosterInfo, String title, String id) {
        super(0, title, null, _GUI.T.PremiumInfoDialog_layoutDialogContent_interested(), _GUI.T.literall_no_thanks());
        info = hosterInfo;
        this.id = id;
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    protected void setReturnmask(boolean b) {
        super.setReturnmask(b);
        if (b) {
            dispose();
            final BuyAndAddPremiumAccount buyAndAddPremiumAccount = new BuyAndAddPremiumAccount(info, id);
            buyAndAddPremiumAccount.getOpenURLAction().actionPerformed(null);
            buyAndAddPremiumAccount.show();
        }
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    protected void layoutDialog() {
        final Image back = NewTheme.I().hasIcon("fav/footer." + info.getTld()) ? NewTheme.I().getImage("fav/footer." + info.getTld(), -1) : null;
        super.layoutDialog();
        getDialog().setContentPane(new JPanel() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                final Graphics2D g2 = (Graphics2D) g;
                if (back != null) {
                    double faktor = Math.max((double) back.getWidth(null) / getWidth(), (double) back.getHeight(null) / getHeight());
                    int width = Math.max((int) (back.getWidth(null) / faktor), 1);
                    int height = Math.max((int) (back.getHeight(null) / faktor), 1);
                    g2.drawImage(back, 0, getHeight() - height, width, getHeight(), 0, 0, back.getWidth(null), back.getHeight(null), getBackground(), null);
                }
            }
        });
    }

    @Override
    public JComponent layoutDialogContent() {
        PluginForHost plg = null;
        try {
            plg = HostPluginController.getInstance().get(info.getTld()).getPrototype(null);
        } catch (UpdateRequiredClassNotFoundException e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
        if (plg != null) {
            // let's ask the plugin
            final JComponent plgPanel = plg.layoutPremiumInfoPanel(this);
            if (plgPanel != null) {
                return plgPanel;
            }
        }
        final MigPanel ret = new MigPanel("ins 0,wrap 2", "[][grow,fill]", "[]");
        ret.setOpaque(false);
        getDialog().setIconImage(IconIO.toBufferedImage(info.getFavIcon()));
        ExtTextArea explain = new ExtTextArea();
        explain.setLabelMode(true);
        explain.setText(getDescription(info));
        int h = explain.getPreferredSize().height;
        Icon icon = info.getIcon(h);
        // if (NewTheme.I().hasIcon("fav/large.wupload.com")) {
        // JLabel lbl = new JLabel(new AbstractIcon("fav/large.wupload.com",
        // -1));
        // lbl.setHorizontalAlignment(SwingConstants.LEFT);
        // lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new
        // Color(LAFOptions.getInstance().getPanelHeaderLineColor())));
        // ret.add(lbl);
        //
        // }
        if (icon != null) {
            ret.add(new JLabel(icon), "aligny top,gapright 10");
        }
        ret.add(explain, "spanx");
        // ret.add(new JSeparator(), "spanx,pushx,growx,gaptop 5");
        // lbl.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, new
        // Color(LAFOptions.getInstance().getPanelHeaderLineColor())));
        JLabel lbl = new JLabel(_GUI.T.PremiumInfoDialog_layoutDialogContent_advantages_header());
        ret.add(SwingUtils.toBold(lbl), "spanx,pushx,growx");
        ret.add(createAdvantages(), "spanx,gapleft " + (h - 24) + ", gaptop 10");
        // ret.add(new JSeparator(), "spanx,pushx,growx,gaptop 5");
        // ret.add(bt, "spanx,alignx center,gapbottom 15,gaptop 10");
        // ret.add(createHeader(new AbstractIcon("prio_3",
        // 20),"Premium Mode?"));
        return ret;
    }

    protected String getDescription(DomainInfo info2) {
        return _GUI.T.PremiumInfoDialog_layoutDialogContent_explain(info.getTld());
    }

    private Component createAdvantages() {
        MigPanel advantages = new MigPanel("ins 0,wrap 2", "[]13[grow,fill]", "[]0[]0[]0[]0[]0[]0[]0[]0");
        advantages.setOpaque(false);
        // for (PremiumFeature pf :
        // info.getAffiliateSettings().getPremiumfeatures()) {
        // if (pf == null) continue;
        // JLabel ico = new JLabel(new AbstractIcon(pf.getIconKey(), 24));
        // JLabel lbl = new JLabel(pf.getTranslation());
        // advantages.add(ico);
        // advantages.add((lbl));
        // }
        advantages.add(new JLabel(new AbstractIcon(IconKey.ICON_SPEED, 24)));
        advantages.add(new JLabel(_GUI.T.PremiumFeature_speed_()));
        advantages.add(new JLabel(new AbstractIcon(IconKey.ICON_BATCH, 24)));
        advantages.add(new JLabel(_GUI.T.PremiumFeature_bandwidth_()));
        advantages.add(new JLabel(new AbstractIcon(IconKey.ICON_PARALELL, 24)));
        advantages.add(new JLabel(_GUI.T.PremiumFeature_parallel_()));
        advantages.add(new JLabel(new AbstractIcon(IconKey.ICON_RESUME, 24)));
        advantages.add(new JLabel(_GUI.T.PremiumFeature_resume_()));
        advantages.add(new JLabel(new AbstractIcon(IconKey.ICON_CHUNKS, 24)));
        advantages.add(new JLabel(_GUI.T.PremiumFeature_chunkload_()));
        advantages.add(new JLabel(new AbstractIcon(IconKey.ICON_WAIT, 24)));
        advantages.add(new JLabel(_GUI.T.PremiumFeature_noWaittime_()));
        return advantages;
    }
}
