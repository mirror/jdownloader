package org.jdownloader.captcha.v2.solver.myjd;

import java.awt.Point;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import jd.gui.swing.jdgui.components.premiumbar.ServicePanel;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.components.tooltips.PanelToolTip;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.components.tooltips.TooltipPanel;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.locator.AbstractLocator;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

public class ServicePanelMyJDCESTooltip extends PanelToolTip {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JComponent        activeComponent;

    public Point getDesiredLocation(JComponent activeComponent, Point ttPosition) {
        if (activeComponent != null) {
            this.activeComponent = activeComponent;
        }

        ttPosition.y = this.activeComponent.getLocationOnScreen().y - getPreferredSize().height;
        ttPosition.x = this.activeComponent.getLocationOnScreen().x;

        return AbstractLocator.correct(ttPosition, getPreferredSize());
    }

    public ServicePanelMyJDCESTooltip(ServicePanel owner, final CaptchaMyJDSolver solver) {

        super(new TooltipPanel("ins 0,wrap 1", "[grow,fill]", "[grow,fill]"));

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        panel.setLayout(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[]"));
        JLabel header = new JLabel("My.JDownloader Captcha Solver", new AbstractIcon(IconKey.ICON_MYJDOWNLOADER, 18), JLabel.LEFT);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LAFOptions.getInstance().getColorForTooltipForeground()));
        SwingUtils.toBold(header);
        header.setForeground(LAFOptions.getInstance().getColorForTooltipForeground());
        panel.add(header, "gapbottom 5,spanx");
        panel.add(progress);
        // panel.setPreferredSize(new Dimension(300, 100));
        new Thread() {
            public void run() {

                try {
                    final MyJDCESInfo account = solver.loadInfo();
                    Thread.sleep(500);
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            if (!isShowing()) { return; }
                            panel.removeAll();
                            // panel.setPreferredSize(null);
                            panel.setLayout(new MigLayout("ins 0,wrap 2", "[][grow,align right]", "[]0"));

                            JLabel header = new JLabel("My.JDownloader Captcha Solver", new AbstractIcon(IconKey.ICON_MYJDOWNLOADER, 18), JLabel.LEFT);
                            header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LAFOptions.getInstance().getColorForTooltipForeground()));
                            SwingUtils.toBold(header);
                            header.setForeground(LAFOptions.getInstance().getColorForTooltipForeground());
                            panel.add(header, "spanx,gapbottom 5,pushx,growx");

                            if (account.isConnected()) {
                                panel.add(lbl("Connected:", NewTheme.I().getIcon(IconKey.ICON_OK, 18), JLabel.LEFT));
                                panel.add(lbl("Yes"));
                            } else {
                                panel.add(lbl("Connected:", NewTheme.I().getIcon(IconKey.ICON_ERROR, 18), JLabel.LEFT));
                                panel.add(lbl("No"));
                            }

                            // panel.revalidate();
                            // revalidate();
                            // repaint();
                            ToolTipController.getInstance().show(ServicePanelMyJDCESTooltip.this);
                        }

                    };
                } catch (Exception e) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            panel.removeAll();
                            panel.add(lbl("Error while loading My.JDownloader CES Information"));
                            panel.repaint();
                        }
                    };
                }

            }
        }.start();

    }

    private JLabel lbl(String string, ImageIcon icon, int left) {
        JLabel ret = new JLabel(string, icon, left);
        ret.setForeground(LAFOptions.getInstance().getColorForTooltipForeground());
        return ret;
    }

    private JLabel lbl(Object string) {
        return lbl(string + "", null, JLabel.LEADING);
    }
}
