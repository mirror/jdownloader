package jd.gui.skins.simple.config;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.utils.JDUtilities;

/**
 * Parentklasse für ein 2. Popupfenster. Wird momentan zur Konfiguration der Interactions verwendet
 * @author coalado
 *
 */
public class ConfigurationSubPanel extends JPanel implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 3815946152967454931L;

    @SuppressWarnings("unused")
    private Configuration configuration;

    private ConfigPanel   panel;

    private JButton       btnSave;

    private JButton       btnCancel;

    @SuppressWarnings("unused")
    private UIInterface   uiinterface;
/**
 * Erstellt einen Neuen Dialog
 * @param parent (Parent FEnster)
 * @param panel   (ConfigPanel (panel inkl. ok/close buttons etc)
 * @param jpanel  (panel des eigentlichen Konfigfenster)
 * @param uiinterface
 * @param configuration
 */
    public ConfigurationSubPanel(ConfigPanel panel,JPanel jpanel, UIInterface uiinterface, Configuration configuration) {
      
        this.uiinterface = uiinterface;
   
   
        setLayout(new GridBagLayout());
        this.configuration = configuration;

        this.panel = panel;
        btnSave = new JButton("OK");
        btnSave.addActionListener(this);
        btnCancel = new JButton("Abbrechen");
        btnCancel.addActionListener(this);

        Insets insets = new Insets(5, 5, 5, 5);

        JDUtilities.addToGridBag(this, jpanel, 0, 0, 2, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, btnSave, 0, 1, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, btnCancel, 1, 1, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
      
       
      
        
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSave) {
            if(panel!=null)panel.save();
        }
        setVisible(false);
    }

}
