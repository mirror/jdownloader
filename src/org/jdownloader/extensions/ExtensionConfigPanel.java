package org.jdownloader.extensions;

import java.awt.Dimension;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import jd.controlling.JDController;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.settings.SettingsComponent;
import org.jdownloader.translate.JDT;

public abstract class ExtensionConfigPanel<T extends PluginOptional> extends SwitchPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JPanel            panel;
    private T                 extension;

    public ExtensionConfigPanel(T plg) {
        super(new MigLayout("ins 10", "[fill,grow]", "[fill,grow]"));
        this.extension = plg;
        panel = new VerticalScrollPanel(new MigLayout("ins 5, wrap 2", "[][grow,fill]", "[]"));

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        // add(scroll);
        add(panel);
        panel.add(new Header(plg.getName(), plg.getIcon(32)), "spanx,growx,pushx");
        if (plg.getDescription() != null) {
            JTextArea txt = new JTextArea();
            txt.setEditable(false);
            txt.setLineWrap(true);

            txt.setFocusable(false);
            txt.setEnabled(false);

            txt.setText(plg.getDescription());
            panel.add(txt, "gaptop 0,spanx,growx,pushx,gapleft 37,gapbottom 5,wmin 10");
            panel.add(new JSeparator(), "spanx,growx,pushx,gapbottom 5");
        }

    }

    protected void showRestartRequiredMessage() {
        try {
            Dialog.getInstance().showConfirmDialog(0, JDT._.dialog_optional_showRestartRequiredMessage_title(), JDT._.dialog_optional_showRestartRequiredMessage_msg(), null, JDT._.basics_yes(), JDT._.basics_no());
            JDController.getInstance().exit();
        } catch (DialogClosedException e) {

        } catch (DialogCanceledException e) {

        }
    }

    public Dimension getPreferredSize() {
        Dimension ret = super.getPreferredSize();
        System.out.println(getClass() + "-" + ret);
        return ret;
    }

    public JPanel getPanel() {
        return panel;
    }

    public T getExtension() {
        return extension;
    }

    public void addPair(String name, SettingsComponent comp) {

        panel.add(createLabel(name), "gapleft 37,aligny top");

        String con = "pushx,growy";
        if (comp.getConstraints() != null) {
            con += "," + comp.getConstraints();
        }
        panel.add((JComponent) comp, con);

    }

    protected JLabel createLabel(String name) {
        return new JLabel(name);

    }

    protected void addHeader(String name, ImageIcon icon) {

        panel.add(new Header(name, icon), "spanx,newline");

    }
}
