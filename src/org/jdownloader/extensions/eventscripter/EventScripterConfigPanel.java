package org.jdownloader.extensions.eventscripter;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.views.settings.panels.proxy.ProxyDeleteAction;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DevConfig;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.AbstractAddAction;
import org.jdownloader.gui.views.components.AbstractRemoveAction;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;

public class EventScripterConfigPanel extends ExtensionConfigPanel<EventScripterExtension> {

    /**
     * 
     */
    private static final long       serialVersionUID = 1L;
    private EventScripterTableModel model;
    private ExtButton               btnAdd;
    private EventScripterTable      table;
    private AbstractRemoveAction    deleteSelectionAction;

    public ArrayList<AdvancedConfigEntry> register() {
        ArrayList<AdvancedConfigEntry> configInterfaces = new ArrayList<AdvancedConfigEntry>();
        HashMap<KeyHandler, Boolean> map = new HashMap<KeyHandler, Boolean>();

        for (KeyHandler m : getExtension().getSettings()._getStorageHandler().getMap().values()) {

            if (map.containsKey(m)) {
                continue;
            }

            if (m.getAnnotation(AboutConfig.class) != null && (m.getAnnotation(DevConfig.class) == null || !Application.isJared(null))) {
                if (m.getSetter() == null) {
                    throw new RuntimeException("Setter for " + m.getGetter().getMethod() + " missing");
                } else if (m.getGetter() == null) {
                    throw new RuntimeException("Getter for " + m.getSetter().getMethod() + " missing");
                } else {
                    synchronized (configInterfaces) {
                        configInterfaces.add(new AdvancedConfigEntry(getExtension().getSettings(), m));
                    }
                    map.put(m, true);
                }
            }

        }

        return configInterfaces;
    }

    public EventScripterConfigPanel(EventScripterExtension extension) {
        super(extension);

        JLabel lbl;

        add(new JScrollPane(table = new EventScripterTable(model = new EventScripterTableModel(extension)) {
            @Override
            protected boolean onShortcutDelete(List<ScriptEntry> selectedObjects, KeyEvent evt, boolean direct) {
                deleteSelectionAction.actionPerformed(null);
                return true;
            }
        }));
        MigPanel toolbar = new MigPanel("ins 0", "0[][][grow,fill]", "[]");
        toolbar.setOpaque(false);
        btnAdd = new ExtButton(new AbstractAddAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ScriptEntry newScript = new ScriptEntry();
                getExtension().addScriptEntry(newScript);
            }

        });

        ProxyDeleteAction dl;
        ExtButton btnRemove = new ExtButton(deleteSelectionAction = new AbstractRemoveAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                List<ScriptEntry> entries = model.getSelectedObjects();
                if (entries == null || entries.size() == 0) {
                    return;
                }
                try {
                    Dialog.getInstance().showConfirmDialog(0, _GUI._.lit_are_you_sure(), org.jdownloader.extensions.eventscripter.T._.sure_delete_entries(entries.size()), new AbstractIcon(IconKey.ICON_QUESTION, 32), null, null);
                    getExtension().removeScriptEntries(entries);
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }

            }

        });
        toolbar.add(btnAdd, "sg 1,height 26!");
        toolbar.add(btnRemove, "sg 1,height 26!");

        toolbar.add(Box.createHorizontalGlue(), "pushx,growx");
        //
        // toolbar.add(btImport, "sg 2,height 26!");
        // toolbar.add(impPopup, "height 26!,width 12!,aligny top");
        //
        // toolbar.add(btExport, "sg 2,height 26!");
        // toolbar.add(expPopup, "height 26!,width 12!,aligny top");

        add(toolbar, "gapleft 32,growx,spanx");

    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {

    }
}
