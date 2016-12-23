package jd.gui.swing.jdgui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final long   serialVersionUID = 1L;
    private final DONATE_EVENT  event;
    private final Icon          eventIcon;
    private final AtomicBoolean flashFlag        = new AtomicBoolean(false);

    public DonateTabHeader(final View view) {
        super(view);
        event = DONATE_EVENT.getNow();
        eventIcon = event.getIcon();
        labelIcon.setIcon(eventIcon);
        flashFlag.set(!event.matchesID(CFG_GUI.CFG.getDonationNotifyID()));
        if (isFlashing()) {
            final Timer blinker = new Timer(1500, new ActionListener() {
                private int        i               = 0;
                private final Icon iconTransparent = IconIO.getTransparentIcon(IconIO.toBufferedImage(eventIcon), 0.5f);

                @Override
                public void actionPerformed(final ActionEvent e) {
                    if (!isFlashing()) {
                        labelIcon.setIcon(eventIcon);
                        ((Timer) e.getSource()).stop();
                    } else {
                        if (i++ % 2 == 0) {
                            labelIcon.setIcon(eventIcon);
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
            public void mouseClicked(MouseEvent e) {
                new DonateAction().actionPerformed(null);
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
            public void mousePressed(MouseEvent e) {
                setShown();
                flashFlag.set(false);
                CFG_GUI.CFG.setDonationNotifyID(event.getID());
                new DonateAction().actionPerformed(null);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                setHidden();
            }

        });
    }

    public void onClick() {

    }

}
