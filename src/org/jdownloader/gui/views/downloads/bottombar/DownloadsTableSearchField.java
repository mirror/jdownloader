package org.jdownloader.gui.views.downloads.bottombar;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.swing.EDTHelper;
import org.jdownloader.gui.views.components.LinktablesSearchCategory;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.SearchField;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public final class DownloadsTableSearchField extends SearchField<LinktablesSearchCategory, FilePackage, DownloadLink> {
    protected static DownloadsTableSearchField INSTANCE;

    public DownloadsTableSearchField(PackageControllerTable<FilePackage, DownloadLink> table2Filter, LinktablesSearchCategory defCategory) {
        super(table2Filter, defCategory);
        setSelectedCategory(JsonConfig.create(GraphicalUserInterfaceSettings.class).getSelectedDownloadSearchCategory());
        setCategories(new LinktablesSearchCategory[] { LinktablesSearchCategory.FILENAME, LinktablesSearchCategory.HOSTER, LinktablesSearchCategory.PACKAGE });
        addKeyListener(new KeyListener() {

            public void keyTyped(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    setText("");

                }
            }

            public void keyPressed(KeyEvent e) {
            }
        });
    }

    @Override
    public void setSelectedCategory(LinktablesSearchCategory selectedCategory) {
        super.setSelectedCategory(selectedCategory);
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setSelectedDownloadSearchCategory(selectedCategory);
    }

    @Override
    public boolean isFiltered(FilePackage e) {
        if (LinktablesSearchCategory.PACKAGE == selectedCategory) {
            for (Pattern filterPattern : filterPatterns) {
                if (filterPattern.matcher(e.getName()).find()) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isFiltered(DownloadLink v) {
        switch (selectedCategory) {
        case FILENAME:
            for (Pattern filterPattern : filterPatterns) {
                if (filterPattern.matcher(v.getName()).find()) return false;
            }
            return true;
        case HOSTER:
            for (Pattern filterPattern : filterPatterns) {
                if (filterPattern.matcher(v.getHost()).find()) return false;
            }
            return true;
        }
        return false;
    }

    public static DownloadsTableSearchField getInstance() {
        return new EDTHelper<DownloadsTableSearchField>() {

            @Override
            public DownloadsTableSearchField edtRun() {
                if (INSTANCE != null) return INSTANCE;
                INSTANCE = new DownloadsTableSearchField(DownloadsTableModel.getInstance().getTable(), LinktablesSearchCategory.FILENAME);
                return INSTANCE;
            }

        }.getReturnValue();

    }

}