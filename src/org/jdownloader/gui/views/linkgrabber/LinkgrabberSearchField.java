package org.jdownloader.gui.views.linkgrabber;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.regex.Pattern;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.swing.EDTHelper;
import org.jdownloader.gui.views.components.LinktablesSearchCategory;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.SearchField;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public final class LinkgrabberSearchField extends SearchField<LinktablesSearchCategory, CrawledPackage, CrawledLink> {
    private static LinkgrabberSearchField INSTANCE;

    public static LinkgrabberSearchField getInstance() {
        return new EDTHelper<LinkgrabberSearchField>() {

            @Override
            public LinkgrabberSearchField edtRun() {
                if (INSTANCE != null) return INSTANCE;
                INSTANCE = new LinkgrabberSearchField(LinkGrabberTableModel.getInstance().getTable(), LinktablesSearchCategory.FILENAME);
                return INSTANCE;
            }

        }.getReturnValue();

    }

    private LinkgrabberSearchField(PackageControllerTable<CrawledPackage, CrawledLink> packageControllerTable, LinktablesSearchCategory categories) {
        super(packageControllerTable, categories);

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
        setSelectedCategory(JsonConfig.create(GraphicalUserInterfaceSettings.class).getSelectedLinkgrabberSearchCategory());
        setCategories(new LinktablesSearchCategory[] { LinktablesSearchCategory.FILENAME, LinktablesSearchCategory.HOSTER, LinktablesSearchCategory.PACKAGE });
    }

    @Override
    public void setSelectedCategory(LinktablesSearchCategory selectedCategory) {
        super.setSelectedCategory(selectedCategory);
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setSelectedLinkgrabberSearchCategory(selectedCategory);
    }

    @Override
    public boolean isFiltered(CrawledPackage e) {
        if (LinktablesSearchCategory.PACKAGE == selectedCategory) {

            for (Pattern filterPattern : filterPatterns) {
                if (filterPattern.matcher(e.getName()).find()) return false;
            }
            return true;

        }
        return false;
    }

    @Override
    public boolean isFiltered(CrawledLink v) {

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
}