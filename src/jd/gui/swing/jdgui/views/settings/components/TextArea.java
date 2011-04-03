package jd.gui.swing.jdgui.views.settings.components;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class TextArea extends JScrollPane implements SettingsComponent {

    private static final long serialVersionUID = -5196573922145630308L;
    private JTextArea         txt;

    public TextArea(String txt) {
        super();
        setText(txt);
    }

    public void setText(String txt2) {
        txt.setText(txt2);
    }

    public TextArea() {
        this.txt = new JTextArea();
        this.txt.setLineWrap(true);
        this.txt.setWrapStyleWord(false);

        this.getViewport().setView(this.txt);
    }

    public String getConstraints() {
        return "wmin 10,height 60:n:n";
    }

    public String getText() {
        return txt.getText();
    }

    public boolean isMultiline() {
        return true;
    }

}
