package jd.gui.swing.jdgui.views;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.UIManager;

import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;
import jd.utils.locale.JDL;

abstract public class ClosableView extends View {

    private static final long serialVersionUID = 8698758386841005256L;
    private JMenuBar menubar;
    private CloseAction closeAction;

    public ClosableView() {
        super();
    }

    /**
     * has to be called to init the close menu
     */
    public void init() {
        menubar = new JMenuBar();

        initMenu(menubar);
        menubar.add(Box.createHorizontalGlue());

        closeAction = new CloseAction();
        Box panel = new Box(1);
        JButton bt;

        panel.add(bt = new JButton(closeAction));
        bt.setPreferredSize(new Dimension(20, 14));
        bt.setContentAreaFilled(false);
        bt.setToolTipText(JDL.LF("jd.gui.swing.jdgui.views.ClosableView.closebtn.tooltip", "Close %s", this.getTitle()));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 0));
        menubar.add(panel);

        add(menubar, "dock NORTH,height 16!,gapbottom 2");
    }

    /**
     * May be overridden to add some more menu Items
     * 
     * @param menubar
     */
    protected void initMenu(JMenuBar menubar) {
        // TODO Auto-generated method stub

    }

    /**
     * CLoses this view
     */
    public void close() {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                closeAction.actionPerformed(null);
                return null;
            }

        }.start();

    }

    public class CloseAction extends AbstractAction {
        private static final long serialVersionUID = -771203720364300914L;

        public CloseAction() {
            this.putValue(AbstractAction.SMALL_ICON, UIManager.getIcon("InternalFrame.closeIcon"));
        }

        public void actionPerformed(ActionEvent e) {
            MainTabbedPane.getInstance().remove(ClosableView.this);
        }
    }
}