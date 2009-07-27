package jd.gui.userio;

import java.awt.Image;
import java.util.ArrayList;

import javax.swing.JFrame;

import jd.gui.skins.SwingGui;
import jd.nutils.JDImage;
import jd.utils.JDUtilities;

/**
 * Dumme JFRame from which dialogs can inherit the icon. workaround for 1.5
 * 
 * @author Coalado
 */
public class DummyFrame extends JFrame {

    public static JFrame getDialogParent() {
        if (SwingGui.getInstance() != null) return SwingGui.getInstance().getMainFrame();
        return new DummyFrame();
    }

    private static final long serialVersionUID = 5729536627803588177L;

    private DummyFrame() {
        super();
        ArrayList<Image> list = new ArrayList<Image>();

        list.add(JDImage.getImage("logo/logo_14_14"));
        list.add(JDImage.getImage("logo/logo_15_15"));
        list.add(JDImage.getImage("logo/logo_16_16"));
        list.add(JDImage.getImage("logo/logo_17_17"));
        list.add(JDImage.getImage("logo/logo_18_18"));
        list.add(JDImage.getImage("logo/logo_19_19"));
        list.add(JDImage.getImage("logo/logo_20_20"));
        list.add(JDImage.getImage("logo/jd_logo_64_64"));
        if (JDUtilities.getJavaVersion() >= 1.6) {
            this.setIconImages(list);
        } else {
            this.setIconImage(list.get(3));
        }

    }

}
