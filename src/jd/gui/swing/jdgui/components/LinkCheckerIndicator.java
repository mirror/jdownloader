package jd.gui.swing.jdgui.components;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;

import javax.swing.JPopupMenu;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerEvent;
import jd.controlling.linkchecker.LinkCheckerListener;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class LinkCheckerIndicator extends IconedProcessIndicator implements LinkCheckerListener {
    /**
     *
     */
    private static final long                   serialVersionUID = -7267364376253248300L;
    private final WeakReference<LinkChecker<?>> linkChecker;
    private final StatusBarImpl                 statusBar;

    public LinkCheckerIndicator(final StatusBarImpl statusBar, final LinkChecker<?> linkChecker) {
        super(new AbstractIcon(IconKey.ICON_SEARCH, 16));
        this.linkChecker = new WeakReference<LinkChecker<?>>(linkChecker);
        this.statusBar = statusBar;
        setTitle(_GUI.T.StatusBarImpl_initGUI_linkchecker());
        setDescription(_GUI.T.StatusBarImpl_initGUI_linkgrabber_desc_inactive());
        LinkChecker.getEventSender().addListener(this, true);
        setEnabled(true);
        if (linkChecker.isRunning()) {
            setIndeterminate(true);
            setDescription(_GUI.T.StatusBarImpl_initGUI_linkchecker_left());
            statusBar.addProcessIndicator(this);
        }
    }

    private LinkChecker<?> getLinkChecker() {
        return linkChecker.get();
    }

    @Override
    public void onLinkCheckerEvent(LinkCheckerEvent event) {
        final LinkChecker<?> linkChecker = getLinkChecker();
        final boolean stopEvent = linkChecker == null || (event.getCaller() == linkChecker && LinkCheckerEvent.Type.STOPPED.equals(event.getType()));
        if (stopEvent) {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    setIndeterminate(false);
                    statusBar.removeProcessIndicator(LinkCheckerIndicator.this);
                }
            };
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            final JPopupMenu popup = new JPopupMenu();
            popup.add(new AppAction() {
                /**
                 *
                 */
                private static final long serialVersionUID = -968768342263254431L;
                {
                    this.setIconKey(IconKey.ICON_CANCEL);
                    this.setName(_GUI.T.StatusBarImpl_initGUI_abort_linkchecker());
                    final LinkChecker<?> linkChecker = getLinkChecker();
                    this.setEnabled(linkChecker != null && linkChecker.isRunning());
                }

                public void actionPerformed(ActionEvent e) {
                    final LinkChecker<?> linkChecker = getLinkChecker();
                    if (linkChecker != null) {
                        linkChecker.stopChecking();
                    }
                }
            });
            popup.show(LinkCheckerIndicator.this, e.getPoint().x, 0 - popup.getPreferredSize().height);
        }
    }
}
