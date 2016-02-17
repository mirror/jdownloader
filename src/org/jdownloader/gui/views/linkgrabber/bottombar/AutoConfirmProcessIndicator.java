package org.jdownloader.gui.views.linkgrabber.bottombar;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.Timer;

import org.appwork.swing.components.tooltips.BasicExtTooltip;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.autostart.AutoStartManagerListener;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.components.IconedProcessIndicator;

public class AutoConfirmProcessIndicator extends IconedProcessIndicator implements AutoStartManagerListener {
    /**
     *
     */
    private static final long serialVersionUID = 8825003449642225290L;
    private final Timer       timer;

    public AutoConfirmProcessIndicator() {
        super(NewTheme.I().getIcon(org.jdownloader.gui.IconKey.ICON_GO_NEXT, 16));
        timer = new Timer(100, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                final int max = LinkCollector.getInstance().getAutoStartManager().getMaximum();
                final int value = LinkCollector.getInstance().getAutoStartManager().getValue();
                setMaximum(max);
                setValue(max - value);
            }
        });
        timer.setRepeats(true);
        setToolTipText(_GUI.T.AutoConfirmButton_AutoConfirmButton_tooltip_());
    }

    @Override
    public ExtTooltip createExtTooltip(Point mousePosition) {
        return new BasicExtTooltip(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        LinkCollector.getInstance().getAutoStartManager().interrupt();
    }

    public void onAutoStartManagerDone() {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                timer.stop();
                JDGui.getInstance().getStatusBar().removeProcessIndicator(AutoConfirmProcessIndicator.this);
            }
        };
    }

    @Override
    public void onAutoStartManagerReset() {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                JDGui.getInstance().getStatusBar().addProcessIndicator(AutoConfirmProcessIndicator.this);
                if (!timer.isRunning()) {
                    timer.start();
                }
            }
        };
    }

    @Override
    public void onAutoStartManagerRunning() {
    }
}
