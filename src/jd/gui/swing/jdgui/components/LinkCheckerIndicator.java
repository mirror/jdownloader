package jd.gui.swing.jdgui.components;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerEvent;
import jd.controlling.linkchecker.LinkCheckerListener;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class LinkCheckerIndicator extends IconedProcessIndicator implements LinkCheckerListener {

    /**
     *
     */
    private static final long    serialVersionUID = -7267364376253248300L;
    private final LinkChecker<?> linkChecker;
    private final StatusBarImpl  statusBar;

    public LinkCheckerIndicator(final StatusBarImpl statusBar, final LinkChecker<?> linkChecker) {
        super(NewTheme.I().getIcon("search", 16));
        this.linkChecker = linkChecker;
        this.statusBar = statusBar;
        setTitle(_GUI._.StatusBarImpl_initGUI_linkchecker());
        setDescription(_GUI._.StatusBarImpl_initGUI_linkgrabber_desc_inactive());
        LinkChecker.getEventSender().addListener(this, true);
        setEnabled(true);
        if (linkChecker.isRunning()) {
            setIndeterminate(true);
            setDescription(_GUI._.StatusBarImpl_initGUI_linkchecker_left());
            statusBar.addProcessIndicator(this);
        }
    }

    @Override
    public void onLinkCheckerEvent(LinkCheckerEvent event) {
        if (event.getCaller() == linkChecker) {
            if (LinkCheckerEvent.Type.STOPPED.equals(event.getType())) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        setIndeterminate(false);
                        statusBar.removeProcessIndicator(LinkCheckerIndicator.this);
                    }
                };
            }
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
                    this.setIconKey("cancel");
                    this.setName(_GUI._.StatusBarImpl_initGUI_abort_linkchecker());
                    this.setEnabled(linkChecker.isRunning());
                }

                public void actionPerformed(ActionEvent e) {
                    linkChecker.stopChecking();
                }

            });

            popup.show(LinkCheckerIndicator.this, e.getPoint().x, 0 - popup.getPreferredSize().height);
        }
    }
}
