package org.jdownloader.gui.views.linkgrabber;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.swing.EDTHelper;
import org.jdownloader.gui.views.components.LinktablesSearchCategory;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelFilter;
import org.jdownloader.gui.views.components.packagetable.SearchField;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public final class LinkgrabberSearchField extends SearchField<LinktablesSearchCategory, CrawledPackage, CrawledLink> {
    private static LinkgrabberSearchField INSTANCE;

    public static LinkgrabberSearchField getInstance() {
        return new EDTHelper<LinkgrabberSearchField>() {

            @Override
            public LinkgrabberSearchField edtRun() {
                if (INSTANCE != null) {
                    return INSTANCE;
                }
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
        setCategories(new LinktablesSearchCategory[] { LinktablesSearchCategory.FILENAME, LinktablesSearchCategory.HOSTER, LinktablesSearchCategory.PACKAGE, LinktablesSearchCategory.COMMENT, LinktablesSearchCategory.COMMENT_PACKAGE });
        setSelectedCategory(JsonConfig.create(GraphicalUserInterfaceSettings.class).getSelectedLinkgrabberSearchCategory());

    }

    @Override
    public void setSelectedCategory(LinktablesSearchCategory selectedCategory) {
        super.setSelectedCategory(selectedCategory);
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setSelectedLinkgrabberSearchCategory(selectedCategory);
    }

    @Override
    protected PackageControllerTableModelFilter<CrawledPackage, CrawledLink> getFilter(final List<Pattern> pattern, LinktablesSearchCategory searchCat) {
        if (searchCat == null || pattern == null || pattern.size() == 0) {
            return null;
        }
        switch (searchCat) {
        case PACKAGE:
            return new PackageControllerTableModelFilter<CrawledPackage, CrawledLink>() {

                @Override
                public boolean isFilteringPackageNodes() {
                    return true;
                }

                @Override
                public boolean isFilteringChildrenNodes() {
                    return false;
                }

                @Override
                public boolean isFiltered(CrawledLink v) {
                    return false;
                }

                @Override
                public boolean isFiltered(CrawledPackage e) {
                    final String name = e.getName();
                    if (isMatching(pattern, name)) {
                        return false;
                    }
                    return true;
                }

                @Override
                public int getComplexity() {
                    return 0;
                }
            };
        case FILENAME:
            return new PackageControllerTableModelFilter<CrawledPackage, CrawledLink>() {

                @Override
                public boolean isFilteringPackageNodes() {
                    return false;
                }

                @Override
                public boolean isFilteringChildrenNodes() {
                    return true;
                }

                @Override
                public boolean isFiltered(CrawledLink v) {
                    final String name = v.getName();
                    if (isMatching(pattern, name)) {
                        return false;
                    }
                    return true;
                }

                @Override
                public boolean isFiltered(CrawledPackage e) {
                    return false;
                }

                @Override
                public int getComplexity() {
                    return 0;
                }
            };

        case COMMENT:
            return new PackageControllerTableModelFilter<CrawledPackage, CrawledLink>() {

                @Override
                public boolean isFilteringPackageNodes() {
                    return false;
                }

                @Override
                public boolean isFilteringChildrenNodes() {
                    return true;
                }

                @Override
                public boolean isFiltered(CrawledLink v) {
                    String comment = v.getDownloadLink().getComment();
                    if (comment == null) {
                        comment = "";
                    }
                    if (isMatching(pattern, comment)) {
                        return false;
                    }
                    return true;
                }

                @Override
                public boolean isFiltered(CrawledPackage fp) {
                    return false;
                }

                @Override
                public int getComplexity() {
                    return 1;
                }
            };
        case COMMENT_PACKAGE:
            return new PackageControllerTableModelFilter<CrawledPackage, CrawledLink>() {

                @Override
                public boolean isFilteringPackageNodes() {
                    return true;
                }

                @Override
                public boolean isFilteringChildrenNodes() {
                    return false;
                }

                @Override
                public boolean isFiltered(CrawledLink v) {
                    return false;
                }

                @Override
                public boolean isFiltered(CrawledPackage fp) {
                    String comment = fp.getComment();
                    if (comment == null) {
                        comment = "";
                    }
                    if (isMatching(pattern, comment)) {
                        return false;
                    }
                    return true;
                }

                @Override
                public int getComplexity() {
                    return 1;
                }
            };
        case HOSTER:
            return new PackageControllerTableModelFilter<CrawledPackage, CrawledLink>() {
                private HashMap<String, Boolean> fastCheck = new HashMap<String, Boolean>();

                @Override
                public boolean isFilteringPackageNodes() {
                    return false;
                }

                @Override
                public boolean isFilteringChildrenNodes() {
                    return true;
                }

                @Override
                public synchronized boolean isFiltered(CrawledLink v) {
                    final String host = v.getDomainInfo().getTld();
                    Boolean ret = fastCheck.get(host);
                    if (ret != null) {
                        return ret.booleanValue();
                    }
                    if (isMatching(pattern, host)) {
                        fastCheck.put(host, Boolean.FALSE);
                        return false;
                    }
                    fastCheck.put(host, Boolean.TRUE);
                    return true;
                }

                @Override
                public boolean isFiltered(CrawledPackage e) {
                    return false;
                }

                @Override
                public int getComplexity() {
                    return 0;
                }
            };
        }
        return null;
    }
}