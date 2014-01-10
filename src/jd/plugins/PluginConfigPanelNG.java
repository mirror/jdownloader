package jd.plugins;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.extensions.Header;
import org.jdownloader.gui.settings.AbstractConfigPanel;

public abstract class PluginConfigPanelNG extends AbstractConfigPanel {

    public PluginConfigPanelNG() {
        super(0);
    }

    protected String getLeftGap() {
        return "0";
    }

    @Override
    protected void addHeader(String name, ImageIcon icon) {

        Header header;
        add(header = new Header(name, icon), "spanx,newline,growx,pushx");
        header.setLayout(new MigLayout("ins 0", "[18!]20[]10[grow,fill]"));

    }

    public void addHtmlDescription(String description, boolean underlined) {
        if (description.toLowerCase().startsWith("<html>")) {
            JLabel txt = new JLabel();
            SwingUtils.setOpaque(txt, false);
            txt.setEnabled(false);
            // txt.setEnabled(false);
            txt.setText(description);
            add(txt, "gaptop 0,spanx,growx,pushx,gapleft " + getLeftGap() + ",gapbottom 5,wmin 10");

            if (underlined) add(new JSeparator(), "gapleft " + getLeftGap() + ",spanx,growx,pushx,gapbottom 5");

        } else {
            addDescription(description);
        }
    }

    public JTextArea addStartDescription(String description) {
        JTextArea txt = new JTextArea();
        SwingUtils.setOpaque(txt, false);
        txt.setEditable(false);
        txt.setLineWrap(true);
        txt.setWrapStyleWord(true);
        txt.setFocusable(false);
        // txt.setEnabled(false);
        txt.setText(description);
        add(txt, "gaptop 0,spanx,growx,pushx,gapleft 0,gapbottom 5,wmin 10");

        add(new JSeparator(), "gapleft 0,spanx,growx,pushx,gapbottom 5");
        return txt;
    }

    public JTextArea addDescriptionPlain(String description) {
        JTextArea txt = new JTextArea();
        SwingUtils.setOpaque(txt, false);
        txt.setEditable(false);
        txt.setLineWrap(true);
        txt.setWrapStyleWord(true);
        txt.setFocusable(false);
        txt.setEnabled(false);
        txt.setText(description);
        add(txt, "gaptop 0,spanx,growx,pushx,gapleft " + getLeftGap() + ",gapbottom 5,wmin 10");

        return txt;
    }

    public abstract void reset();

    @Override
    public ImageIcon getIcon() {
        return null;
    }

    @Override
    public String getTitle() {
        return null;
    }
}
