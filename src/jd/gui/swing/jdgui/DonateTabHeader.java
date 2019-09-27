package jd.gui.swing.jdgui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Icon;
import javax.swing.Timer;

import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.maintab.TabHeader;

import org.appwork.utils.images.IconIO;
import org.jdownloader.donate.DONATE_EVENT;
import org.jdownloader.gui.mainmenu.DonateAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class DonateTabHeader extends TabHeader implements PromotionTabHeader {
    /**
     *
     */
    private static final long                   serialVersionUID = 1L;
    private final AtomicReference<DONATE_EVENT> event            = new AtomicReference<DONATE_EVENT>(null);
    private final AtomicBoolean                 flashFlag        = new AtomicBoolean(false);

    public DonateTabHeader(final View view) {
        super(view);
        updateEvent();
        final Timer updateTimer = new Timer(60 * 60 * 1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateEvent();
            }
        });
        updateTimer.setRepeats(true);
        updateTimer.start();
    }

    private void updateEvent() {
        final DONATE_EVENT now = DONATE_EVENT.getNow();
        if (event.getAndSet(now) != now) {
            final Icon icon = now.getIcon();
            labelIcon.setIcon(icon);
            setToolTipText(now.getToolTipText());
            flashFlag.set(!now.matchesID(CFG_GUI.CFG.getDonationNotifyID()));
            if (isFlashing()) {
                final Timer blinker = new Timer(1500, new ActionListener() {
                    private long       i               = 0;
                    private final Icon iconTransparent = IconIO.getTransparentIcon(IconIO.toBufferedImage(icon), 0.5f);

                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        if (!isFlashing() || event.get() != now) {
                            final DONATE_EVENT donate = event.get();
                            labelIcon.setIcon(donate.getIcon());
                            setToolTipText(donate.getToolTipText());
                            ((Timer) e.getSource()).stop();
                        } else {
                            if (i++ % 2 == 0) {
                                labelIcon.setIcon(icon);
                            } else {
                                labelIcon.setIcon(iconTransparent);
                            }
                        }
                    }
                });
                blinker.setRepeats(true);
                blinker.start();
            }
        }
    }

    private boolean isFlashing() {
        return flashFlag.get();
    }

    @Override
    protected void initMouseForwarder() {
        addMouseListener(new JDMouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, MainTabbedPane.getInstance());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, MainTabbedPane.getInstance());
            }

            @Override
            public void mouseClicked(MouseEvent me) {
                new DonateAction(event.get()).actionPerformed(new ActionEvent(me.getSource(), me.getID(), me.paramString()));
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, MainTabbedPane.getInstance());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, MainTabbedPane.getInstance());
            }

            @Override
            public void mousePressed(MouseEvent me) {
                flashFlag.set(false);
                final DONATE_EVENT donate = event.get();
                CFG_GUI.CFG.setDonationNotifyID(donate.getID());
                new DonateAction(donate).actionPerformed(new ActionEvent(me.getSource(), me.getID(), me.paramString()));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        });
    }

    public void onClick() {
    }
}
