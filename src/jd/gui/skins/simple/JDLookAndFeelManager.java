package jd.gui.skins.simple;

import java.io.Serializable;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager.LookAndFeelInfo;

import jd.config.SubConfiguration;

import jd.utils.JDUtilities;

public class JDLookAndFeelManager implements Serializable{
    /**
     * 
     */
    private static final long serialVersionUID = -8056003135389551814L;
    public static final String PARAM_PLAF = "PLAF";
    private String ClassName;
    private static boolean uiInitated = false;
    private static SubConfiguration config = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
    public String getClassName() {
        return ClassName;
    }
    public void setClassName(String className) {
        ClassName = className;
    }
    public JDLookAndFeelManager(String ClassName) {
        this.ClassName = ClassName;
    }
    public JDLookAndFeelManager(LookAndFeelInfo lafi) {
        this.ClassName = lafi.getClassName();
    }
    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return ClassName.substring(ClassName.lastIndexOf(".")+1, ClassName.length()-11);
    }

    public static JDLookAndFeelManager getPlaf() {
        Object plaf = config.getProperty(PARAM_PLAF, null);
        if(plaf==null)
        return new JDLookAndFeelManager(UIManager.getSystemLookAndFeelClassName());
        if(plaf instanceof JDLookAndFeelManager)
            return (JDLookAndFeelManager) plaf;
        else if(plaf instanceof String)
        {
            for (LookAndFeelInfo lafi : UIManager.getInstalledLookAndFeels()) {
                if(lafi.getName().equals(plaf))
                {
                    plaf=new JDLookAndFeelManager(lafi);
                    config.setProperty(PARAM_PLAF, plaf);
                    config.save();
                    return (JDLookAndFeelManager) plaf;
                }
            }
        }
        return new JDLookAndFeelManager(UIManager.getSystemLookAndFeelClassName());

    }
    public static JDLookAndFeelManager[] getInstalledLookAndFeels() {
        LookAndFeelInfo[] lafis = UIManager.getInstalledLookAndFeels();
        JDLookAndFeelManager[] ret = new JDLookAndFeelManager[lafis.length];
        for (int i = 0; i < lafis.length; i++) {
            ret[i]=new JDLookAndFeelManager(lafis[i]);
        }
        return ret;
    }
    public static void setUIManager() {
        if (uiInitated) { return; }
        uiInitated = true;
        try {
            UIManager.setLookAndFeel(getPlaf().ClassName);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
