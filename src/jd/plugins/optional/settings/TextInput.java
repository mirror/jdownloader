package jd.plugins.optional.settings;

import javax.swing.JTextField;

public class TextInput extends JTextField implements SettingsComponent {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public TextInput(String nick) {
        super(nick);
    }

    public TextInput() {
        super();
    }

    public String getConstraints() {
        return null;
    }

}
