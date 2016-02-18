package jd.gui.swing.jdgui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.Timer;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.mainmenu.DonateAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.maintab.TabHeader;

public class DonateTabHeader extends TabHeader implements PromotionTabHeader {

    private static final String ELV_CC = "elv&cc";
    private Timer               blinker;

    public DonateTabHeader(final View view) {
        super(view);

        if (doFlash()) {
            blinker = new Timer(1500, new ActionListener() {
                private int i = 0;

                @Override
                public void actionPerformed(ActionEvent e) {

                    if (!doFlash()) {
                        labelIcon.setIcon(view.getIcon());
                        blinker.stop();
                        return;
                    }
                    if (i++ % 2 == 0) {
                        labelIcon.setIcon(view.getIcon());
                    } else {
                        labelIcon.setIcon(IconIO.getTransparentIcon(IconIO.toBufferedImage(view.getIcon()), 0.5f));
                    }
                }
            });
            blinker.setRepeats(true);
            blinker.start();
        }

    }

    private boolean doFlash() {
        return !StringUtils.equalsIgnoreCase(CFG_GUI.CFG.getDonationNotifyID(), ELV_CC);
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
                if (doFlash()) {
                    ConfirmDialog d = new ConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _GUI.T.donation_news_title(), _GUI.T.donation_news(), new AbstractIcon(IconKey.ICON_BOTTY_ROBOT_INFO, -1), _GUI.T.lit_continue(), null);
                    UIOManager.I().show(ConfirmDialogInterface.class, d);
                }
                CFG_GUI.CFG.setDonationNotifyID(ELV_CC);
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
