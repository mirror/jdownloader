package jd.gui.swing.jdgui.views;

import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicInternalFrameUI;

import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.borders.JDBorderFactory;
import jd.gui.swing.jdgui.interfaces.View;

abstract public class ClosableView extends View {

    private static final long serialVersionUID = 8698758386841005256L;

    public ClosableView() {
        super();

        JInternalFrame fm = new JInternalFrame("") {
            public void setVisible(boolean aFlag) {
                if (!aFlag) {

                    MainTabbedPane.getInstance().remove(ClosableView.this);

                }
            }

        };
        //jetzt du
        fm.setIconifiable(false);
        fm.setResizable(true);
        fm.setClosable(true);
        fm.setFrameIcon(null);
        new JDesktopPane().add(fm);
        BasicInternalFrameUI ui = (BasicInternalFrameUI) fm.getUI();
        JComponent title = ui.getNorthPane();
        title.setBorder(JDBorderFactory.createInsideShadowBorder(0, 0, 5, 0));
        fm.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        add(title, "dock NORTH,gapleft 0");

    }

}