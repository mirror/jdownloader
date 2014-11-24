package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.dimensor.RememberLastDialogDimension;
import org.appwork.utils.swing.dialog.locator.RememberAbsoluteDialogLocator;
import org.jdownloader.captcha.v2.SolverService;
import org.jdownloader.gui.translate._GUI;

public class SolverPropertiesDialog extends AbstractDialog<Object> {

    private AbstractCaptchaSolverConfigPanel configPanel;

    public static class ScrollablePanel extends MigPanel implements Scrollable {

        private AbstractCaptchaSolverConfigPanel panel;
        private boolean                          vScrollbarVisible = false;

        public AbstractCaptchaSolverConfigPanel getPanel() {
            return panel;
        }

        public ScrollablePanel(AbstractCaptchaSolverConfigPanel panel) {
            super("ins 0 0 0 0", "[grow,fill]", "[grow,fill]");
            add(panel);
            setOpaque(false);
            // LAFOptions.getInstance().applyPanelBackground(this);
            this.panel = panel;
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return ((orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width) - 10;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        public void setVerticalScrollbarVisible(boolean visible) {
            if (vScrollbarVisible == visible) {
                return;
            }
            vScrollbarVisible = visible;

            if (vScrollbarVisible) {
                setLayout("ins 0 0 0 5", "[grow,fill]", "[grow,fill]");
            } else {
                setLayout("ins 0 0 0 0", "[grow,fill]", "[grow,fill]");
            }
        }

    }

    @Override
    public JComponent layoutDialogContent() {
        this.getDialog().setLayout(new MigLayout("ins 5 5 5 5,wrap 1", "[grow,fill]", "[grow,fill][]"));
        final ScrollablePanel sbp;
        final JScrollPane sp = new JScrollPane(sbp = new ScrollablePanel(configPanel));
        sp.setBorder(null);
        sp.getViewport().addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                // System.err.println("Change in " + e.getSource());
                // System.err.println("Vertical visible? " + );
                // System.err.println("Horizontal visible? " +
                // pane.getHopane.getVerticalScrollBar().isVisible()rizontalScrollBar().isVisible());
                // System.out.println(sp.getVerticalScrollBar().isVisible());
                sbp.setVerticalScrollbarVisible(sp.getVerticalScrollBar().isVisible());

            }
            //
        });
        setMinimumSize(new Dimension(200, 200));
        // sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        ;
        // sp.setPreferredSize(new Dimension(2000, 8000));
        return sp;
    }

    protected MigPanel createBottomPanel() {
        // TODO Auto-generated method stub
        return new MigPanel("ins 0 0 0 0", "[]20[grow,fill][]", "[]");
    }

    public SolverPropertiesDialog(SolverService editing, AbstractCaptchaSolverConfigPanel configPanel) {
        super(UIOManager.BUTTONS_HIDE_CANCEL | Dialog.STYLE_HIDE_ICON, _GUI._.SolverPropertiesDialog_SolverPropertiesDialog_object_(editing.getName(), editing.getType()), null, _GUI._.lit_close(), null);
        this.configPanel = configPanel;
        setLocator(new RememberAbsoluteDialogLocator(getClass().getSimpleName()));
        setDimensor(new RememberLastDialogDimension(getClass().getSimpleName()));
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }
}
