package jd.plugins.optional.awesomebar.awesome.gui;

import javax.swing.JPanel;

import jd.plugins.optional.awesomebar.AwesomebarExtension;
import net.miginfocom.swing.MigLayout;

public class AwesomeToolbarPanel extends JPanel {
    private static final long serialVersionUID = -4240120832785592416L;

    private final AwesomeTextField awesomeTextField;
    /**
     * @return the awesomeTextField
     */
    public AwesomeTextField getAwesomeTextField() {
        return awesomeTextField;
    }

    /**
     * @return the awesomeDescription
     */
    public AwesomeDescription getAwesomeDescription() {
        return awesomeDescription;
    }

    private final AwesomeDescription awesomeDescription;
    private final AwesomebarExtension awesomebar;
    public AwesomebarExtension getAwesomebar() {
        return awesomebar;
    }
    
    public AwesomeToolbarPanel(AwesomebarExtension awesomebar) {
        super(new MigLayout("ins 1"));

        this.setOpaque(false);
        
        this.awesomebar = awesomebar;
        this.awesomeTextField = new AwesomeTextField(this);
        this.awesomeDescription = new AwesomeDescription(this);

        // textfield
        this.add(this.awesomeTextField, "width 150px!, wrap");
        //this.add(this.awesomeDescription, "width 150px!"/* , height 9px!" */);
    }


}