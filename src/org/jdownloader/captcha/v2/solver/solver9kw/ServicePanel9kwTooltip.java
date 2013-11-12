package org.jdownloader.captcha.v2.solver.solver9kw;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;

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
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

public class ServicePanel9kwTooltip extends PanelToolTip {
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

    public ServicePanel9kwTooltip(ServicePanel owner, final Captcha9kwSolver captcha9kwSolver) {

        super(new TooltipPanel("ins 0,wrap 1", "[grow,fill]", "[grow,fill]"));
        this.owner = owner;
        color = (LAFOptions.getInstance().getColorForTooltipForeground());
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        panel.setLayout(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[]"));
        JLabel header = new JLabel("9kw Captcha Solver", DomainInfo.getInstance("9kw.eu").getFavIcon(), JLabel.LEFT);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LAFOptions.getInstance().getColorForTooltipForeground()));
        SwingUtils.toBold(header);
        header.setForeground(LAFOptions.getInstance().getColorForTooltipForeground());
        panel.add(header, "gapbottom 5,spanx");
        panel.add(progress);
        // panel.setPreferredSize(new Dimension(300, 100));
        new Thread() {
            public void run() {

                try {
                    final NineKWAccount account = captcha9kwSolver.loadAccount();
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            panel.removeAll();
                            // panel.setPreferredSize(null);
                            if (!account.isValid()) {
                                panel.setLayout(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[]"));
                                JLabel header = new JLabel("9kw Captcha Solver", DomainInfo.getInstance("9kw.eu").getFavIcon(), JLabel.LEFT);
                                header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LAFOptions.getInstance().getColorForTooltipForeground()));
                                SwingUtils.toBold(header);
                                header.setForeground(LAFOptions.getInstance().getColorForTooltipForeground());
                                panel.add(header, "gapbottom 5,spanx");
                                panel.add(lbl(_GUI._.ServicePanel9kwTooltip_runInEDT_error2(""), NewTheme.I().getIcon(IconKey.ICON_ERROR, 18), JLabel.LEFT));
                                panel.add(lbl(account.getError()), "gapleft 22");
                            } else {
                                panel.setLayout(new MigLayout("ins 0,wrap 2", "[][grow,align right]", "[]0"));
                                JLabel header = new JLabel("9kw Captcha Solver", DomainInfo.getInstance("9kw.eu").getFavIcon(), JLabel.LEFT);
                                header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LAFOptions.getInstance().getColorForTooltipForeground()));
                                SwingUtils.toBold(header);
                                header.setForeground(LAFOptions.getInstance().getColorForTooltipForeground());
                                panel.add(header, "spanx,gapbottom 5,pushx,growx");
                                panel.add(lbl(_GUI._.ServicePanel9kwTooltip_runInEDT_credits_(), NewTheme.I().getIcon(IconKey.ICON_MONEY, 18), JLabel.LEFT));
                                panel.add(lbl(account.getCreditBalance() + ""));
                                panel.add(lbl(_GUI._.ServicePanel9kwTooltip_runInEDT_solved(), NewTheme.I().getIcon(IconKey.ICON_LOGIN, 18), JLabel.LEFT));
                                panel.add(lbl(account.getSolved() + ""));
                                panel.add(lbl(_GUI._.ServicePanel9kwTooltip_runInEDT_answered(), NewTheme.I().getIcon(IconKey.ICON_LOGOUT, 18), JLabel.LEFT));
                                panel.add(lbl(account.getAnswered() + ""));

                                panel.add(lbl(_GUI._.ServicePanel9kwTooltip_runInEDT_avgsolvetime(), NewTheme.I().getIcon(IconKey.ICON_TRUE, 18), JLabel.LEFT));
                                panel.add(lbl(account.getAvgSolvtime() + "s"));

                                panel.add(lbl(_GUI._.ServicePanel9kwTooltip_runInEDT_queue(), NewTheme.I().getIcon(IconKey.ICON_TRUE, 18), JLabel.LEFT));
                                panel.add(lbl(account.getQueue() + "|" + account.getQueue1() + "|" + account.getQueue2()));

                                panel.add(lbl(_GUI._.ServicePanel9kwTooltip_runInEDT_worker(), NewTheme.I().getIcon(IconKey.ICON_TRUE, 18), JLabel.LEFT));
                                panel.add(lbl(account.getWorker()));
                                panel.add(lbl(_GUI._.ServicePanel9kwTooltip_runInEDT_worker_mouse(), NewTheme.I().getIcon(IconKey.ICON_TRUE, 18), JLabel.LEFT));
                                panel.add(lbl(account.getWorkerMouse()));
                                panel.add(lbl(_GUI._.ServicePanel9kwTooltip_runInEDT_worker_confirm(), NewTheme.I().getIcon(IconKey.ICON_TRUE, 18), JLabel.LEFT));
                                panel.add(lbl(account.getWorkerConfirm()));

                                panel.add(lbl(_GUI._.ServicePanel9kwTooltip_runInEDT_inwork(), NewTheme.I().getIcon(IconKey.ICON_TRUE, 18), JLabel.LEFT));
                                panel.add(lbl(account.getInWork()));
                            }
                            // panel.revalidate();
                            // revalidate();
                            // repaint();
                            ToolTipController.getInstance().show(ServicePanel9kwTooltip.this);
                        }

                    };
                } catch (IOException e) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            panel.removeAll();
                            panel.add(lbl(_GUI._.ServicePanel9kwTooltip_runInEDT_error()));
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
