package jd.gui.swing.jdgui.views;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.UIManager;

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
        // SyntheticaBlackMoonLookAndFeel syn = new
        // SyntheticaBlackMoonLookAndFeel();

        panel.add(bt = new JButton(closeAction));
        // if (UIManager.getLookAndFeel() instanceof SyntheticaLookAndFeel) {
        // SyntheticaLookAndFeel synth = ((SyntheticaLookAndFeel)
        // UIManager.getLookAndFeel());
        // UIDefaults def = synth.getDefaults();
        //
        // // SynthContext reag = SyntheticaLookAndFeel.createContext(new
        // JInternalFrame(),
        // javax.swing.plaf.synth.Region.INTERNAL_FRAME_TITLE_PANE,
        // State.HOVER.toInt());
        // // Synthetica.docking.titlebar.close.hover
        // UIDefaults defs2 = UIManager.getDefaults();
        // ;
        // def = null;
        // }
        bt.setPreferredSize(new Dimension(20, 14));
        bt.setContentAreaFilled(false);
        bt.setToolTipText(JDL.LF("jd.gui.swing.jdgui.views.ClosableView.closebtn.tooltip", "Close %s", this.getTitle()));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 0));
        menubar.add(panel);
        // menubar.add(new JMenuItem(closeAction));

        // menubar.add(createMenu("X"));

        // JInternalFrame fm = new JInternalFrame("") {
        // public void setVisible(boolean aFlag) {
        // if (!aFlag) {
        //
        // MainTabbedPane.getInstance().remove(ClosableView.this);
        //
        // }
        // }
        //
        // };
        //   
        // fm.setIconifiable(false);
        // fm.setResizable(true);
        // fm.setClosable(true);
        // fm.setFrameIcon(null);
        // new JDesktopPane().add(fm);
        // BasicInternalFrameUI ui = (BasicInternalFrameUI) fm.getUI();
        // JComponent title = ui.getNorthPane();
        // title.setBorder(JDBorderFactory.createInsideShadowBorder(0, 0, 5,
        // 0));
        // fm.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

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

    public class CloseAction extends AbstractAction {
        /**
         * 
         */
        private static final long serialVersionUID = -771203720364300914L;

        public CloseAction() {

            // this.putValue(AbstractAction.SMALL_ICON,
            // UIManager.getIcon("Synthetica.docking.titlebar.close.hover"));

            this.putValue(AbstractAction.SMALL_ICON, UIManager.getIcon("InternalFrame.closeIcon"));
        }

        public void actionPerformed(ActionEvent e) {
            MainTabbedPane.getInstance().remove(ClosableView.this);

        }
    }
}