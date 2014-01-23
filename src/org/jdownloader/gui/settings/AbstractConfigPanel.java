package org.jdownloader.gui.settings;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.Header;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;

public abstract class AbstractConfigPanel extends SwitchPanel {

    private static final long       serialVersionUID = -8483438886830392777L;
    private java.util.List<Pair<?>> pairs;

    public AbstractConfigPanel() {
        this(15);
    }

    public AbstractConfigPanel(int insets) {
        super(new MigLayout("ins " + insets + ", wrap 2", "[][grow,fill]", "[]"));
        pairs = new ArrayList<Pair<?>>();
        setOpaque(false);
    }

    public JLabel addDescription(String description) {
        JLabel txt = addDescriptionPlain(description);
        add(new JSeparator(), "gapleft " + getLeftGap() + ",spanx,growx,pushx,gapbottom 5");
        return txt;
    }

    public JLabel addDescriptionPlain(String description) {

        if (!description.toLowerCase().startsWith("<html>")) {
            description = "<html>" + description.replace("\r\n", "<br>").replace("\r", "<br>").replace("\n", "<br>") + "<html>";
        }
        JLabel txt = new JLabel();
        SwingUtils.setOpaque(txt, false);
        txt.setEnabled(false);
        // txt.setEnabled(false);
        txt.setText(description);
        add(txt, "gaptop 0,spanx,growx,pushx,gapleft " + getLeftGap() + ",gapbottom 5,wmin 10");

        return txt;
    }

    public void addTopHeader(String name, ImageIcon icon) {
        add(new Header(name, icon), "spanx,growx,pushx");

    }

    protected void showRestartRequiredMessage() {
        try {
            Dialog.getInstance().showConfirmDialog(0, _JDT._.dialog_optional_showRestartRequiredMessage_title(), _JDT._.dialog_optional_showRestartRequiredMessage_msg(), null, _JDT._.basics_yes(), _JDT._.basics_no());
            RestartController.getInstance().exitAsynch(new SmartRlyExitRequest(true));
        } catch (DialogClosedException e) {
        } catch (DialogCanceledException e) {
        }
    }

    @Deprecated
    protected void addHeader(String name, String iconKey) {
        this.addHeader(name, NewTheme.I().getIcon(iconKey, 32));
    }

    public abstract ImageIcon getIcon();

    public abstract String getTitle();

    public <T extends SettingsComponent> Pair<T> addPair(String name, BooleanKeyHandler enabled, T comp) {
        String lblConstraints = (enabled == null ? "" : "split 3,") + "gapleft" + getLeftGap() + ",aligny " + (comp.isMultiline() ? "top" : "center");

        return addPair(name, lblConstraints, enabled, comp);

    }

    public <T extends SettingsComponent> Pair<T> addPair(String name, String lblConstraints, BooleanKeyHandler enabled, T comp) {
        JLabel lbl;
        add(lbl = createLabel(name), lblConstraints);
        if (enabled != null) {
            ExtCheckBox cb = new ExtCheckBox(enabled, lbl, (JComponent) comp);
            SwingUtils.setOpaque(cb, false);
            add(Box.createHorizontalGlue(), "pushx,growx");
            add(cb, "width " + cb.getPreferredSize().width + "!,aligny " + (comp.isMultiline() ? "top" : "center"));
            cb.setToolTipText(_GUI._.AbstractConfigPanel_addPair_enabled());
        }
        String con = "pushx,growy";
        if (comp.getConstraints() != null) {
            con += "," + comp.getConstraints();
        }
        add((JComponent) comp, con);
        Pair<T> p = new Pair<T>(lbl, comp);
        pairs.add(p);
        return p;
    }

    @Override
    protected void onShow() {
        updateContents();
        for (Pair p : pairs) {
            p.update();
        }
    }

    public abstract void save();

    public abstract void updateContents();

    @Override
    protected void onHide() {
        save();
    }

    protected JLabel createLabel(String name) {
        return new JLabel(name);

    }

    public Component add(Component comp) {
        if (comp instanceof SettingsComponent) {
            String con = "gapleft" + getLeftGap() + ",spanx,growx,pushx";
            if (((SettingsComponent) comp).getConstraints() != null) {
                con += "," + ((SettingsComponent) comp).getConstraints();
            }
            super.add(comp, con);
            return comp;

        } else if (comp instanceof JScrollPane) {
            super.add(comp, "gapleft" + getLeftGap() + ",spanx,growx,pushx,height 60:n:n,pushy,growy");
            return comp;
        } else {
            super.add(comp, "growx, pushx,spanx");
            return comp;
        }
    }

    protected String getLeftGap() {
        return "32";
    }

    protected void addHeader(String name, ImageIcon icon) {
        if (getComponentCount() == 0) {
            // first header
            add(new Header(name, icon), "spanx,growx,pushx");
        } else {
            add(new Header(name, icon), "spanx,newline,growx,pushx");
        }
    }

}
