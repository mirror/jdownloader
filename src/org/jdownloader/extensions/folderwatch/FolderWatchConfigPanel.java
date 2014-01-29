package org.jdownloader.extensions.folderwatch;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JTextArea;

import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedConfigTableModel;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedTable;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;

public class FolderWatchConfigPanel extends ExtensionConfigPanel<FolderWatchExtension> {

    /**
     * 
     */
    private static final long        serialVersionUID = 1L;
    private AdvancedConfigTableModel model;

    public ArrayList<AdvancedConfigEntry> register() {
        ArrayList<AdvancedConfigEntry> configInterfaces = new ArrayList<AdvancedConfigEntry>();
        HashMap<KeyHandler, Boolean> map = new HashMap<KeyHandler, Boolean>();

        for (KeyHandler m : getExtension().getSettings()._getStorageHandler().getMap().values()) {

            if (map.containsKey(m)) continue;

            if (m.getAnnotation(AboutConfig.class) != null) {
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

    public FolderWatchConfigPanel(FolderWatchExtension extension) {
        super(extension);

        add(new AdvancedTable(model = new AdvancedConfigTableModel("FolderWatchConfig") {
            @Override
            public void refresh(String filterText) {
                _fireTableStructureChanged(register(), true);
            }
        }));
        model.refresh("FolderWatch");

        JTextArea txt = new JTextArea();
        txt.setText("[\r\n  {\r\n    \"chunks\": 0,\r\n    \"extractPasswords\": null,\r\n    \"enabled\": null,\r\n    \"text\": null,\r\n    \"packageName\": null,\r\n    \"autoStart\": \"UNSET\",\r\n    \"extractAfterDownload\": \"UNSET\",\r\n    \"downloadFolder\": null,\r\n    \"type\": \"NORMAL\",\r\n    \"priority\": \"DEFAULT\",\r\n    \"forcedStart\": \"UNSET\",\r\n    \"downloadPassword\": null,\r\n    \"filename\": null,\r\n    \"overwritePackagizerEnabled\": false,\r\n    \"comment\": null,\r\n    \"autoConfirm\": \"UNSET\",\r\n    \"deepAnalyseEnabled\": false\r\n  }\r\n]");
        add(txt);
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {

    }

}
