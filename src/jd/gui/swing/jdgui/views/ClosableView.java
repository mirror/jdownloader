package jd.gui.swing.jdgui.views;

import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicInternalFrameUI;

import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.interfaces.View;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

abstract public class ClosableView extends View {

    private static final long serialVersionUID = 8698758386841005256L;

    public ClosableView() {
        super();

     
      
        JInternalFrame fm = new JInternalFrame(""){
            public void setVisible(boolean aFlag) {
                if(!aFlag){
                   
                      MainTabbedPane.getInstance().remove(ClosableView.this);
                    
                }
            }

        };
        fm.setIconifiable(false);
        fm.setResizable(true);
        fm.setClosable(true);
        fm.setFrameIcon(null);
        new JDesktopPane().add(fm);
        BasicInternalFrameUI ui = (BasicInternalFrameUI) fm.getUI();
        JComponent title = ui.getNorthPane();
        title.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, getBackground().darker()));
        fm.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

      
        add(title, "dock NORTH,gapleft 0");

    }



}