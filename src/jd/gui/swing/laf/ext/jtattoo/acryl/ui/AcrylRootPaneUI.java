package jd.gui.swing.laf.ext.jtattoo.acryl.ui;

import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.plaf.ComponentUI;

import jd.gui.swing.laf.ext.jtattoo.acryl.components.AcrylTitlePane;

import com.jtattoo.plaf.BaseRootPaneUI;
import com.jtattoo.plaf.BaseTitlePane;

/**
 * extended from JTattoo centers Title, smooth icon systemmenubar
 * 
 * @author Coalado
 * 
 */
public class AcrylRootPaneUI extends BaseRootPaneUI {
    public static ComponentUI createUI(JComponent c) {
        return new AcrylRootPaneUI();
    }

    public BaseTitlePane createTitlePane(JRootPane root) {
        return new AcrylTitlePane(root, this);
    };

}
