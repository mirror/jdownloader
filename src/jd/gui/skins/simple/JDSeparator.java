package jd.gui.skins.simple;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JToggleButton;

import jd.utils.JDLocale;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class JDSeparator extends JToggleButton implements ActionListener  {

    private static final long serialVersionUID = 3007033193590223026L;

    private ImageIcon left;
    private ImageIcon right;



    // private boolean mouseover;

    public JDSeparator() {
        setLayout(new MigLayout("ins 0,wrap 1"));
   

        // SimpleGUI.CURRENTGUI.getLeftcolPane().addMouseListener(this);
        left = JDTheme.II("gui.images.minimize.left", 5, 10);
        right = JDTheme.II("gui.images.minimize.right", 5, 10);
        this.setIcon(left);
        this.setFocusable(false);
        
        this.setSelectedIcon(right);
    
        // setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMinimized(false);
   
         addActionListener(this);
         
    }

    public void setMinimized(boolean b) {
     
      setSelected(b);
      
      if(b){
          this.setToolTipText(JDLocale.L("gui.tooltips.jdseparator","Open sidebar"));
      }else{
          this.setToolTipText(JDLocale.L("gui.tooltips.jdseparator","Close sidebar")); 
          
      }
    }

 
    public void actionPerformed(ActionEvent e) {
        SimpleGUI.CURRENTGUI.hideSideBar(isSelected());
        
    }

}
