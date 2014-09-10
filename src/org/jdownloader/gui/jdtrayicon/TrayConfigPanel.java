package org.jdownloader.gui.jdtrayicon;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.PasswordInput;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.ConstraintParser;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.Header;
import org.jdownloader.gui.jdtrayicon.translate._TRAY;
import org.jdownloader.gui.settings.Pair;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class TrayConfigPanel extends ExtensionConfigPanel<TrayExtension> {

    public TrayConfigPanel(TrayExtension trayExtension) {

        super(trayExtension, true);

        /*
         * Override default implementation of MigLayout layout manager and use one more suitable to this panel
         * 
         * Useful resources: http://www.migcalendar.com/miglayout/mavensite/apidocs/index.html
         * http://www.migcalendar.com/miglayout/mavensite/docs/cheatsheet.pdf
         */

        // Layout constraints
        LC layCons = new LC();
        // Apply layout rules
        layCons.setInsets(ConstraintParser.parseInsets("15", true));
        layCons.wrapAfter(3);

        // Axis constraints
        AC axiCons = new AC();
        // Column 1
        // Default
        // Column 2
        axiCons.index(1).shrink();
        // Column 3
        axiCons.index(2).fill();
        axiCons.index(2).grow();

        // Override default layout

        setLayout(new MigLayout(layCons, axiCons));

        /* Hookup gui components to extension configuration */

        this.extension = trayExtension;
        BooleanKeyHandler keyHandlerEnabled = trayExtension.getSettings()._getStorageHandler().getKeyHandler("enabled", BooleanKeyHandler.class);
        final Header header = new Header(trayExtension.getName(), NewTheme.I().getIcon(extension.getIconKey(), 32), keyHandlerEnabled, extension.getVersion());

        keyHandlerEnabled.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                try {
                    extension.setEnabled(header.isHeaderEnabled());
                    updateHeaders(header.isHeaderEnabled());
                } catch (Exception e1) {
                    Log.exception(e1);
                    Dialog.getInstance().showExceptionDialog("Error", e1.getMessage(), e1);
                }
            }

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        });
        trayExtension.getSettings()._getStorageHandler().getEventSender().addListener(this);

        add(header, "spanx,growx,pushx");

        header.setEnabled(trayExtension.isEnabled());
        if (trayExtension.getDescription() != null) {
            addDescription(trayExtension.getDescription());
        }

        /* Tray configuration */

        @SuppressWarnings("unchecked")
        KeyHandler<OnCloseAction> keyHandler = CFG_TRAY_CONFIG.SH.getKeyHandler("OnCloseAction", KeyHandler.class);
        addPair(_TRAY._.plugins_optional_JDLightTray_closetotray2(), null, new ComboBox<OnCloseAction>(keyHandler, new OnCloseAction[] { OnCloseAction.ASK, OnCloseAction.TO_TRAY, OnCloseAction.TO_TASKBAR, OnCloseAction.EXIT }, new String[] { OnCloseAction.ASK.getTranslation(), OnCloseAction.TO_TRAY.getTranslation(), OnCloseAction.TO_TASKBAR.getTranslation(), OnCloseAction.EXIT.getTranslation() }));

        KeyHandler<OnMinimizeAction> keyHandler2 = CFG_TRAY_CONFIG.SH.getKeyHandler("OnMinimizeAction", KeyHandler.class);
        addPair(_TRAY._.plugins_optional_JDLightTray_minimizetotray(), null, new ComboBox<OnMinimizeAction>(keyHandler2, new OnMinimizeAction[] { OnMinimizeAction.TO_TRAY, OnMinimizeAction.TO_TASKBAR }, new String[] { OnMinimizeAction.TO_TRAY.getTranslation(), OnMinimizeAction.TO_TASKBAR.getTranslation() }));
        addPair(_TRAY._.plugins_optional_JDLightTray_startMinimized(), null, new Checkbox(CFG_TRAY_CONFIG.START_MINIMIZED_ENABLED));
        addPair(_TRAY._.plugins_optional_JDLightTray_singleClick(), null, new Checkbox(CFG_TRAY_CONFIG.TOOGLE_WINDOW_STATUS_WITH_SINGLE_CLICK_ENABLED));

        addPair(_TRAY._.plugins_optional_JDLightTray_tooltip(), null, new Checkbox(CFG_TRAY_CONFIG.TOOL_TIP_ENABLED));
        addPair(_TRAY._.plugins_optional_JDLightTray_hideifframevisible(), null, new Checkbox(CFG_TRAY_CONFIG.TRAY_ONLY_VISIBLE_IF_WINDOW_IS_HIDDEN_ENABLED));
        addPair(_TRAY._.plugins_optional_JDLightTray_passwordRequired(), CFG_GUI.PASSWORD_PROTECTION_ENABLED, new PasswordInput(CFG_GUI.PASSWORD));
    }

    @Override
    public <T extends SettingsComponent> Pair<T> addPair(String name, BooleanKeyHandler enabled, T comp) {
        String lblConstraints = "gapleft: " + getLeftGap();
        return addPair(name, lblConstraints, enabled, comp);
    }

    @Override
    public <T extends SettingsComponent> Pair<T> addPair(String name, String lblConstraints, BooleanKeyHandler enabled, T comp) {

        String con = "pushx,growy";
        if (comp.getConstraints() != null) {
            con += "," + comp.getConstraints();
        }

        // COL 1: Label

        JLabel lbl;
        ExtCheckBox cb = null;

        lbl = createLabel(name);
        add(lbl, lblConstraints);

        // COL 2/3: If T component enabled state is defined, add a checkbox to col 2 to toggle its state
        // and add the T component to col 3
        if (enabled != null) {
            cb = new ExtCheckBox(enabled, lbl, (JComponent) comp);
            cb.setToolTipText(_GUI._.AbstractConfigPanel_addPair_enabled());
            SwingUtils.setOpaque(cb, false);
            add(cb, "width " + cb.getPreferredSize().width + "!, aligny " + (comp.isMultiline() ? "top" : "center"));
            add((JComponent) comp, con);
        }
        // If the T component is only a checkbox, put it in col 2 and fill col 3 with glue
        else if (comp instanceof JCheckBox) {
            add((JComponent) comp, con);
            add(Box.createHorizontalGlue(), "");
        }
        // If the T component has no enabled state, fill col 2 with glue and put the T component in col 3
        else {
            add(Box.createHorizontalGlue(), "");
            add((JComponent) comp, con);
        }

        Pair<T> p = new Pair<T>(lbl, comp, cb);
        pairs.add(p);
        return p;
    }

    @Override
    public void save() {
    }

    private void updateHeaders(boolean b) {

    }

    @Override
    public void updateContents() {

    }

}
