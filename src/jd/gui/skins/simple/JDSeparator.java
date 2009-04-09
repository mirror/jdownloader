package jd.gui.skins.simple;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;

import jd.utils.JDLocale;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXCollapsiblePane;

public class JDSeparator extends JXCollapsiblePane implements PropertyChangeListener {

    private static final long serialVersionUID = 3007033193590223026L;
    private JButton minimize;
    private JButton maximize;

    public JDSeparator() {

        setLayout(new MigLayout("ins 0,wrap 1"));

        //      
        // add(dlLIst = new JButton(JDTheme.II("gui.list", 5, 10)),
        // "width 4!,gapright 1");
        //
        // dlLIst.setToolTipText(JDLocale.L("gui.sidebar.splitbuttons.download.tooltip",
        // "Switch to Downloadlist"));
        // dlLIst.setBorderPainted(false);
        // dlLIst.setOpaque(false);
        // dlLIst.setContentAreaFilled(false);
        // dlLIst.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // dlLIst.setFocusPainted(false);
        // dlLIst.setBorderPainted(false);
        // dlLIst.addActionListener(new ActionListener() {
        //
        // public void actionPerformed(ActionEvent e) {
        // SimpleGUI.CURRENTGUI.getContentPane().display(SimpleGUI.CURRENTGUI.getDownloadPanel());
        // }
        //
        // });

        add(minimize = new JButton(JDTheme.II("gui.images.minimize.left", 5, 10)), "width 4!,gapright 1");

        minimize.setToolTipText(JDLocale.L("gui.sidebar.splitbuttons.hide.tooltip", "Hide the Sidebar"));
        minimize.setBorderPainted(false);
        minimize.setOpaque(false);
        minimize.setContentAreaFilled(false);
        minimize.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        minimize.setFocusPainted(false);
        minimize.setBorderPainted(false);
        minimize.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SimpleGUI.CURRENTGUI.hideSideBar(true);
            }

        });
        add(maximize = new JButton(JDTheme.II("gui.images.minimize.right", 5, 10)), "width 4!,gapright 1");

        maximize.setBorderPainted(false);
        maximize.setToolTipText(JDLocale.L("gui.sidebar.splitbuttons.show.tooltip", "Show the Sidebar"));
        maximize.setOpaque(false);
        maximize.setContentAreaFilled(false);
        maximize.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        maximize.setFocusPainted(false);
        maximize.setBorderPainted(false);
        maximize.setEnabled(false);
        maximize.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SimpleGUI.CURRENTGUI.hideSideBar(false);
            }

        });
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() instanceof JXCollapsiblePane && evt.getPropertyName().equals("collapsed")) {
            if (((JXCollapsiblePane) evt.getSource()).isCollapsed()) {
                maximize.setEnabled(true);
                minimize.setEnabled(false);
            } else {
                maximize.setEnabled(false);
                minimize.setEnabled(true);
            }

        }

    }

}
