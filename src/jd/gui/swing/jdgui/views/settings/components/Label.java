package jd.gui.swing.jdgui.views.settings.components;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class Label extends JLabel implements SettingsComponent {
    public Label(String txt, Icon icon) {
        super(htmlize(txt), icon, SwingConstants.LEFT);
    }

    public Label(String txt) {
        super(htmlize(txt));
    }

    private static String htmlize(String txt) {
        txt = txt.trim();
        if (!txt.startsWith("<html>")) {
            txt = "<html>" + txt;
        }
        if (!txt.endsWith("</html>")) {
            txt += "</html>";
        }
        txt = txt.replaceAll("[\r\n]{1,2}", "<br>");
        return txt;
    }

    @Override
    public void setText(String text) {
        super.setText(htmlize(text));
    }

    @Override
    public String getConstraints() {
        return null;
    }

    @Override
    public boolean isMultiline() {
        return false;
    }
}
