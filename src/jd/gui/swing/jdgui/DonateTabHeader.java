package jd.gui.swing.jdgui;

import java.awt.event.MouseEvent;

import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.maintab.TabHeader;

import org.jdownloader.gui.mainmenu.DonateAction;

public class DonateTabHeader extends TabHeader implements PromotionTabHeader {

    public DonateTabHeader(View view) {
        super(view);

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
