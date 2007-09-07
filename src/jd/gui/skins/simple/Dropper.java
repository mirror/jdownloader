package jd.gui.skins.simple;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

import jd.JDUtilities;
import jd.event.UIListener;
import jd.gui.skins.simple.components.DragNDrop;

/**
 * Die Dropperklasse zeigt einen DropTarget Dialog an. Zieht man links oder text darauf, so wird das eingefügt.
 * 
 * @author Tom
 */
public class Dropper extends JDialog {

    /**
     * 8764525546298642601L
     */
    private static final long serialVersionUID = 8764525546298642601L;
    
    private DragNDrop target;
    private JLabel label;
    public Dropper(JFrame owner) {
        super(owner);
        setModal(false);
        setLayout(new GridBagLayout());
        target = new DragNDrop();
        label= new JLabel("Ziehe Links auf mich!");
        JDUtilities.addToGridBag(this, target, 0, 0, 1, 1, 0, 0, null, GridBagConstraints.CENTER, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, label, 0, 1, 1, 1, 0, 0, null, GridBagConstraints.CENTER, GridBagConstraints.CENTER);
        
        setSize(40,60);
        this.setResizable(false);
        this.setTitle("JD-Drop");
    
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

}
