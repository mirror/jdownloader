package org.jdownloader.extensions.translator.gui;

import java.lang.reflect.Type;
import java.util.ArrayList;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.appwork.app.gui.MigPanel;
import org.jdownloader.extensions.translator.TranslateEntry;

/**
 * Infopanel at the gui bottom
 * 
 * @author thomas
 * 
 */
public class EntryInfoPanel extends MigPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JTextArea         txt;

    public EntryInfoPanel() {
        super("ins 0", "[grow,fill]", "[grow,fill]");
        txt = new JTextArea();
        add(new JScrollPane(txt), "height 120!");
    }

    /**
     * Is called when selection in the table changes
     * 
     * @param selectedObjects
     */
    public void setEntries(ArrayList<TranslateEntry> selectedObjects) {
        if (selectedObjects == null || selectedObjects.size() == 0) {
            txt.setText("");
        } else {
            TranslateEntry obj = selectedObjects.get(0);
            StringBuilder sb = new StringBuilder();
            sb.append(obj.getFullKey());
            sb.append("\r\n");
            String d = obj.getDescription();
            if (d != null) {
                sb.append("\r\n");
                sb.append(d);
            }
            sb.append("\r\n");
            Type[] parameters = obj.getParameters();
            if (parameters.length == 0) {
                sb.append("Parameters: NONE\r\n");
            } else {
                sb.append("Parameter(s): ");
                sb.append(parameters.length);
                sb.append("\r\n");
                int i = 1;
                for (Type t : parameters) {
                    sb.append("   %s" + i + " (" + t + ")\r\n");
                    i++;
                }
            }
            sb.append("\r\n");

            sb.append("Default: \r\n");
            sb.append(obj.getDefault());
            txt.setText(sb.toString());
        }
    }

}
