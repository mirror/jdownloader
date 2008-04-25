//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.gui.skins.simple;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import jd.event.UIListener;
import jd.gui.skins.simple.components.DragNDrop;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Die Dropperklasse zeigt einen DropTarget Dialog an. Zieht man links oder text darauf, so wird das eingefügt.
 * 
 * @author Tom
 */
public class Dropper extends JDialog implements MouseListener, MouseMotionListener,WindowListener {

    /**
     * 8764525546298642601L
     */
    private static final long serialVersionUID = 8764525546298642601L;
    
    private DragNDrop target;
    private JLabel label;
  //  private Logger logger;

  //  private Point point;
    /**
     * @param owner  Owner ist der Parent Frame
     */
    public Dropper(JFrame owner) {
        super(owner);
        setModal(false);
        setLayout(new GridBagLayout());
        JPanel p= new JPanel(new GridBagLayout());
        p.addMouseListener(this);
        p.addMouseMotionListener(this);
        this.addWindowListener(this);
        target = new DragNDrop();
       // logger= JDUtilities.getLogger();
        label= new JLabel(JDLocale.L("gui.droptarget.label","Ziehe Links auf mich!"));
        JDUtilities.addToGridBag(p, target, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.NORTH);
        JDUtilities.addToGridBag(p, label, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.SOUTH);
        JDUtilities.addToGridBag(this, p, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
       // p.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(50,70);
        this.setResizable(false);
        this.setUndecorated(false);
      //this.setBackground(new Color(255,255,255,100));
      p.setBackground(new Color(255,255,255,100)); 
        this.setTitle(JDLocale.L("gui.droptarget.title","Linksammler aktiv (D&D + Clipboard)"));
    
        setLocation(20,20);
        this.setAlwaysOnTop(true);
      
        pack();
    }
    /**
     * Setzt den Südlichen text im Target
     * @param text
     */
    public void setText(String text){
        label.setText(text);
        this.pack();
    }
    /**
     * Deligiert den UILIstener zur Targetkomponente
     * @param listener
     */
    public void addUIListener(UIListener listener) {
        target.addUIListener(listener);
    }
/**
 * Entfernt die Targetkomponente als Listener
 * @param listener
 */
    public void removeUIListener(UIListener listener) {
        target.removeUIListener(listener);
    }
public void mouseClicked(MouseEvent e) {
    JDUtilities.getLogger().info("click");
    
}
public void mouseEntered(MouseEvent e) {
    JDUtilities.getLogger().info("enter");
    
}
public void mouseExited(MouseEvent e) {
    JDUtilities.getLogger().info("exit");
    
}
public void mousePressed(MouseEvent e) {
  //  this.point=e.getPoint();
    
}
public void mouseReleased(MouseEvent e) {
    JDUtilities.getLogger().info("release");
    
}
public void mouseDragged(MouseEvent e) {
   //this.setLocation(e.getXOnScreen()-point.x,e.getYOnScreen()-point.y);
    
}
public void mouseMoved(MouseEvent e) {
    //JDUtilities.getLogger().info("move");
    
}
public void windowActivated(WindowEvent e) {
    // TODO Auto-generated method stub
    
}
public void windowClosed(WindowEvent e) {
    // TODO Auto-generated method stub
    
}
public void windowClosing(WindowEvent e) {
    this.setVisible(false);
    
}
public void windowDeactivated(WindowEvent e) {
    // TODO Auto-generated method stub
    
}
public void windowDeiconified(WindowEvent e) {
    // TODO Auto-generated method stub
    
}
public void windowIconified(WindowEvent e) {
    // TODO Auto-generated method stub
    
}
public void windowOpened(WindowEvent e) {
    // TODO Auto-generated method stub
    
}

}
