package org.jdownloader.captcha.v2.solver.captchabrotherhood;

import java.awt.Color;
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
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

public class ServicePanelCBHTooltip extends PanelToolTip {
    private Color        color;

    private ServicePanel owner;

    private JComponent   activeComponent;

    public Point getDesiredLocation(JComponent activeComponent, Point ttPosition) {
        if (activeComponent != null) {
            this.activeComponent = activeComponent;
        }

        ttPosition.y = this.activeComponent.getLocationOnScreen().y - getPreferredSize().height;
        ttPosition.x = this.activeComponent.getLocationOnScreen().x;

        return AbstractLocator.correct(ttPosition, getPreferredSize());
    }

    public ServicePanelCBHTooltip(ServicePanel owner, final CBSolver solver) {

        super(new TooltipPanel("ins 0,wrap 1", "[grow,fill]", "[grow,fill]"));
        this.owner = owner;
        color = (LAFOptions.getInstance().getColorForTooltipForeground());
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        panel.setLayout(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[]"));
        JLabel header = new JLabel("Captchabrotherhood Solver", NewTheme.I().getIcon(IconKey.ICON_CBH, 18), JLabel.LEFT);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LAFOptions.getInstance().getColorForTooltipForeground()));
        SwingUtils.toBold(header);
        header.setForeground(LAFOptions.getInstance().getColorForTooltipForeground());
        panel.add(header, "gapbottom 5,spanx");
        panel.add(progress);
        // panel.setPreferredSize(new Dimension(300, 100));
        new Thread() {
            public void run() {

                final CBHAccount account = solver.loadAccount();
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        panel.removeAll();
                        // panel.setPreferredSize(null);
                        if (!account.isValid()) {
                            panel.setLayout(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[]"));
                            JLabel header = new JLabel("captchabrotherhood.com Captcha Solver", NewTheme.I().getIcon(IconKey.ICON_CBH, 18), JLabel.LEFT);
                            header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LAFOptions.getInstance().getColorForTooltipForeground()));
                            SwingUtils.toBold(header);
                            header.setForeground(LAFOptions.getInstance().getColorForTooltipForeground());
                            panel.add(header, "gapbottom 5,spanx");
                            panel.add(lbl(_GUI._.ServicePanel9kwTooltip_runInEDT_error2(""), NewTheme.I().getIcon(IconKey.ICON_ERROR, 18), JLabel.LEFT));
                            panel.add(lbl(account.getError()), "gapleft 22");
                        } else {
                            panel.setLayout(new MigLayout("ins 0,wrap 2", "[][grow,align right]", "[]0"));
                            JLabel header = new JLabel("captchabrotherhood.com Captcha Solver", NewTheme.I().getIcon(IconKey.ICON_CBH, 18), JLabel.LEFT);
                            header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LAFOptions.getInstance().getColorForTooltipForeground()));
                            SwingUtils.toBold(header);
                            header.setForeground(LAFOptions.getInstance().getColorForTooltipForeground());
                            panel.add(header, "spanx,gapbottom 5,pushx,growx");

                            panel.add(lbl(_GUI._.ServicePanel9kwTooltip_runInEDT_credits_(), NewTheme.I().getIcon(IconKey.ICON_MONEY, 18), JLabel.LEFT));
                            panel.add(lbl(account.getBalance() + " Credits"));

                            panel.add(lbl("Requested:", NewTheme.I().getIcon(IconKey.ICON_QUESTION, 18), JLabel.LEFT));
                            panel.add(lbl(account.getRequests() + " Captcha(s)"));
                            panel.add(lbl("Solved:", NewTheme.I().getIcon(IconKey.ICON_OK, 18), JLabel.LEFT));
                            panel.add(lbl(account.getSolved() + " Captcha(s)"));
                            panel.add(lbl("Error:", NewTheme.I().getIcon(IconKey.ICON_ERROR, 18), JLabel.LEFT));
                            panel.add(lbl(account.getSkipped() + " Captcha(s)"));

                        }
                        // panel.revalidate();
                        // revalidate();
                        // repaint();
                        ToolTipController.getInstance().show(ServicePanelCBHTooltip.this);
                    }

                };

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
