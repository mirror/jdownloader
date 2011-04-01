package org.jdownloader.extensions;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import jd.controlling.JDController;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.translate.JDT;

public abstract class AbstractConfigPanel extends SwitchPanel {

    public AbstractConfigPanel() {
        super(new MigLayout("ins 15, wrap 2", "[][grow,fill]", "[]"));
    }

    protected void addDescription(String description) {
        JTextArea txt = new JTextArea();
        txt.setEditable(false);
        txt.setLineWrap(true);

        txt.setFocusable(false);
        txt.setEnabled(false);

        txt.setText(description);
        add(txt, "gaptop 0,spanx,growx,pushx,gapleft 37,gapbottom 5,wmin 10");
        add(new JSeparator(), "gapleft 37,spanx,growx,pushx,gapbottom 5");
    }

    protected void addTopHeader(String name, ImageIcon icon) {
        add(new Header(name, icon), "spanx,growx,pushx");

    }

    protected void showRestartRequiredMessage() {
        try {
            Dialog.getInstance().showConfirmDialog(0, JDT._.dialog_optional_showRestartRequiredMessage_title(), JDT._.dialog_optional_showRestartRequiredMessage_msg(), null, JDT._.basics_yes(), JDT._.basics_no());
            JDController.getInstance().exit();
        } catch (DialogClosedException e) {

        } catch (DialogCanceledException e) {

        }
    }

    @Deprecated
    protected void addHeader(String name, String iconKey) {
        this.addHeader(name, JDTheme.II(iconKey, 32, 32));
    }

    public abstract ImageIcon getIcon();

    public abstract String getTitle();

    public void addPair(String name, SettingsComponent comp) {

        add(createLabel(name), "gapleft 37,aligny " + (comp.isMultiline() ? "top" : "center"));

        String con = "pushx,growy";
        if (comp.getConstraints() != null) {
            con += "," + comp.getConstraints();
        }
        add((JComponent) comp, con);

    }

    public Dimension getPreferredScrollableViewportSize() {

        return this.getPreferredSize();
    }

    public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
        return Math.max(visibleRect.height * 9 / 10, 1);
    }

    public boolean getScrollableTracksViewportHeight() {

        return false;
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
        return Math.max(visibleRect.height / 10, 1);
    }

    protected JLabel createLabel(String name) {
        return new JLabel(name);

    }

    public Component add(Component comp) {
        if (comp instanceof SettingsComponent) {
            String con = "gapleft 37,spanx,growx,pushx";
            if (((SettingsComponent) comp).getConstraints() != null) {
                con += "," + ((SettingsComponent) comp).getConstraints();
            }
            super.add(comp, con);
            return comp;
        } else {
            super.add(comp, "growx, pushx,spanx");
            return comp;
        }
    }

    protected void addHeader(String name, ImageIcon icon) {

        add(new Header(name, icon), "spanx,newline,growx,pushx");

    }
}
