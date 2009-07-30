package jd.gui.swing.jdgui.maintab;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.gui.swing.jdgui.interfaces.View;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class ChangeHeader extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 4463352125800695922L;
    private JLabel change;
    private JButton closeIcon;
    private Action action;

    public ChangeHeader(View view) {
        setLayout(new MigLayout("ins 0", "[grow,fill]"));
        JLabel l1 = new JLabel(view.getTitle());
        change = new JLabel("*");
        closeIcon = new JButton(JDTheme.II("gui.tab.close", 12, 12));
       closeIcon.setContentAreaFilled(false);
//        closeIcon.setBorderPainted(false);

        closeIcon.setText(null);
        closeIcon.setVisible(false);
        putClientProperty("paintActive", Boolean.TRUE);
        l1.setIcon(view.getIcon());
        add(l1);
        add(change, "dock west,hidemode 3");
        add(closeIcon, "dock east, hidemode 3,gapleft 5,height 16!, width 16!");
        change.setVisible(false);
        setOpaque(false);
        l1.setOpaque(false);
        change.setOpaque(false);
    }

    /**
     * enables or disables the changed mark
     * 
     * @param b
     */
    public void setChanged(boolean b) {
        change.setVisible(b);
    }

    /**
     * ENables the close button by adding an action
     * 
     * @param a
     */
    public void setCloseEnabled(Action a) {
        if(action!=null){
            closeIcon.removeActionListener(action);
        }
        if (a != null) {
            action=a;
            closeIcon.addActionListener(a);
            closeIcon.setVisible(true);
        } else {
            action=a;
            closeIcon.setVisible(false);
        }

    }

}
