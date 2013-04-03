package org.jdownloader.premium;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.SecondLevelLaunch;
import jd.gui.swing.laf.LookAndFeelController;
import jd.plugins.PluginForHost;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class PremiumInfoDialog extends AbstractDialog<Object> {
    public static void main(String[] args) {
        PremiumInfoDialog cp;
        try {
            SecondLevelLaunch.statics();

            cp = new PremiumInfoDialog(DomainInfo.getInstance("rapidshare.com"));

            LookAndFeelController.getInstance().setUIManager();

            Dialog.getInstance().showDialog(cp);
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private DomainInfo info;
    private String     id;

    public PremiumInfoDialog(DomainInfo hosterInfo, String title, String id) {
        super(0, title, null, _GUI._.PremiumInfoDialog_layoutDialogContent_interested(), _GUI._.literall_no_thanks());
        info = hosterInfo;
        this.id = id;
    }

    public PremiumInfoDialog(DomainInfo hosterInfo) {
        this(hosterInfo, _GUI._.PremiumInfoDialog_PremiumInfoDialog_(hosterInfo.getTld()), "PremiumInfoDialog");
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
            new BuyPremiumAction(info, id).actionPerformed(null);
        }
    }

    @Override
    protected boolean isResizable() {
        return true;
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
            Log.exception(e);
        }
        if (plg != null) {
            // let's ask the plugin
            JComponent plgPanel = plg.layoutPremiumInfoPanel(this);
            if (plgPanel != null) return plgPanel;
        }
        final MigPanel ret = new MigPanel("ins 0,wrap 2", "[][grow,fill]", "[]");

        ret.setOpaque(false);
        getDialog().setIconImage(info.getFavIcon().getImage());

        ExtTextArea explain = new ExtTextArea();
        explain.setLabelMode(true);
        explain.setText(getDescription(info));
        int h = explain.getPreferredSize().height;
        ImageIcon icon = info.getIcon(h);

        // if (NewTheme.I().hasIcon("fav/large.wupload.com")) {
        // JLabel lbl = new JLabel(NewTheme.I().getIcon("fav/large.wupload.com",
        // -1));
        // lbl.setHorizontalAlignment(SwingConstants.LEFT);
        // lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new
        // Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderLineColor())));
        // ret.add(lbl);
        //
        // }
        if (icon != null) {
            ret.add(new JLabel(icon), "aligny top,gapright 10");
        }
        ret.add(explain, "spanx");
        // ret.add(new JSeparator(), "spanx,pushx,growx,gaptop 5");

        // lbl.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, new
        // Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderLineColor())));

        JLabel lbl = new JLabel(_GUI._.PremiumInfoDialog_layoutDialogContent_advantages_header());
        ret.add(SwingUtils.toBold(lbl), "spanx,pushx,growx");
        ret.add(createAdvantages(), "spanx,gapleft " + (h - 24) + ", gaptop 10");
        // ret.add(new JSeparator(), "spanx,pushx,growx,gaptop 5");

        // ret.add(bt, "spanx,alignx center,gapbottom 15,gaptop 10");
        // ret.add(createHeader(NewTheme.I().getIcon("prio_3",
        // 20),"Premium Mode?"));

        return ret;
    }

    protected String getDescription(DomainInfo info2) {
        return _GUI._.PremiumInfoDialog_layoutDialogContent_explain(info.getTld());
    }

    private Component createAdvantages() {
        MigPanel advantages = new MigPanel("ins 0,wrap 2", "[]13[grow,fill]", "[]0[]0[]0[]0[]0[]0[]0[]0");
        advantages.setOpaque(false);
        // for (PremiumFeature pf :
        // info.getAffiliateSettings().getPremiumfeatures()) {
        // if (pf == null) continue;
        // JLabel ico = new JLabel(NewTheme.I().getIcon(pf.getIconKey(), 24));
        // JLabel lbl = new JLabel(pf.getTranslation());
        // advantages.add(ico);
        // advantages.add((lbl));
        // }
        advantages.add(new JLabel(NewTheme.I().getIcon("speed", 24)));
        advantages.add(new JLabel(_GUI._.PremiumFeature_speed_()));

        advantages.add(new JLabel(NewTheme.I().getIcon("batch", 24)));
        advantages.add(new JLabel(_GUI._.PremiumFeature_bandwidth_()));

        advantages.add(new JLabel(NewTheme.I().getIcon("paralell", 24)));
        advantages.add(new JLabel(_GUI._.PremiumFeature_parallel_()));

        advantages.add(new JLabel(NewTheme.I().getIcon("resume", 24)));
        advantages.add(new JLabel(_GUI._.PremiumFeature_resume_()));

        advantages.add(new JLabel(NewTheme.I().getIcon("chunks", 24)));
        advantages.add(new JLabel(_GUI._.PremiumFeature_chunkload_()));

        advantages.add(new JLabel(NewTheme.I().getIcon("wait", 24)));
        advantages.add(new JLabel(_GUI._.PremiumFeature_noWaittime_()));

        return advantages;
    }

    private Component createHeader(ImageIcon icon, String string) {
        return null;
    }

}
