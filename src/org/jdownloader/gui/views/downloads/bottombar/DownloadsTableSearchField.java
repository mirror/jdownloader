package org.jdownloader.gui.views.downloads.bottombar;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginProgress;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.swing.EDTHelper;
import org.jdownloader.extensions.extraction.ExtractionStatus;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.LinktablesSearchCategory;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelFilter;
import org.jdownloader.gui.views.components.packagetable.SearchField;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public final class DownloadsTableSearchField extends SearchField<LinktablesSearchCategory, FilePackage, DownloadLink> {
    protected static DownloadsTableSearchField INSTANCE;

    public DownloadsTableSearchField(PackageControllerTable<FilePackage, DownloadLink> table2Filter, LinktablesSearchCategory defCategory) {
        super(table2Filter, defCategory);
        setCategories(new LinktablesSearchCategory[] { LinktablesSearchCategory.FILENAME, LinktablesSearchCategory.HOSTER, LinktablesSearchCategory.PACKAGE, LinktablesSearchCategory.COMMENT, LinktablesSearchCategory.STATUS });
        setSelectedCategory(JsonConfig.create(GraphicalUserInterfaceSettings.class).getSelectedDownloadSearchCategory());

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

    protected PackageControllerTableModelFilter<FilePackage, DownloadLink> getFilter(final List<Pattern> pattern, LinktablesSearchCategory searchCat) {
        if (searchCat == null || pattern == null || pattern.size() == 0) {
            return null;
        }
        switch (searchCat) {
        case PACKAGE:
            return new PackageControllerTableModelFilter<FilePackage, DownloadLink>() {

                @Override
                public boolean isFilteringPackageNodes() {
                    return true;
                }

                @Override
                public boolean isFilteringChildrenNodes() {
                    return false;
                }

                @Override
                public boolean isFiltered(DownloadLink v) {
                    return false;
                }

                @Override
                public boolean isFiltered(FilePackage e) {
                    for (Pattern filterPattern : pattern) {
                        if (filterPattern.matcher(e.getName()).find()) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                public int getComplexity() {
                    return 0;
                }
            };
        case FILENAME:
            return new PackageControllerTableModelFilter<FilePackage, DownloadLink>() {

                @Override
                public boolean isFilteringPackageNodes() {
                    return false;
                }

                @Override
                public boolean isFilteringChildrenNodes() {
                    return true;
                }

                @Override
                public boolean isFiltered(DownloadLink v) {
                    for (Pattern filterPattern : pattern) {
                        if (filterPattern.matcher(v.getView().getDisplayName()).find()) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                public boolean isFiltered(FilePackage e) {
                    return false;
                }

                @Override
                public int getComplexity() {
                    return 0;
                }
            };
        case COMMENT:
            return new PackageControllerTableModelFilter<FilePackage, DownloadLink>() {

                @Override
                public boolean isFilteringPackageNodes() {
                    return true;
                }

                @Override
                public boolean isFilteringChildrenNodes() {
                    return true;
                }

                @Override
                public boolean isFiltered(DownloadLink v) {
                    for (Pattern filterPattern : pattern) {
                        if (v.getComment() != null && filterPattern.matcher(v.getComment()).find()) {
                            return false;
                        }
                    }

                    for (Pattern filterPattern : pattern) {
                        if (v.getParentNode().getComment() != null && filterPattern.matcher(v.getParentNode().getComment()).find()) {
                            return false;
                        }
                    }

                    return true;
                }

                @Override
                public boolean isFiltered(FilePackage fp) {
                    for (Pattern filterPattern : pattern) {
                        if (fp.getComment() != null && filterPattern.matcher(fp.getComment()).find()) {
                            return false;
                        }
                    }

                    boolean readL = fp.getModifyLock().readLock();

                    try {
                        for (DownloadLink dl : fp.getChildren()) {
                            for (Pattern filterPattern : pattern) {
                                if (dl.getComment() != null && filterPattern.matcher(dl.getComment()).find()) {
                                    return false;
                                }
                            }
                        }
                    } finally {
                        fp.getModifyLock().readUnlock(readL);
                    }

                    return true;
                }

                @Override
                public int getComplexity() {
                    return 1;
                }
            };
        case HOSTER:
            return new PackageControllerTableModelFilter<FilePackage, DownloadLink>() {

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
                public synchronized boolean isFiltered(DownloadLink v) {
                    String host = v.getDomainInfo().getTld();
                    Boolean ret = fastCheck.get(host);
                    if (ret != null) {
                        return ret.booleanValue();
                    }
                    for (Pattern filterPattern : pattern) {
                        if (filterPattern.matcher(host).find()) {
                            fastCheck.put(host, Boolean.FALSE);
                            return false;
                        }
                    }
                    fastCheck.put(host, Boolean.TRUE);
                    return true;
                }

                @Override
                public boolean isFiltered(FilePackage e) {
                    return false;
                }

                @Override
                public int getComplexity() {
                    return 0;
                }
            };
        case STATUS:
            return new PackageControllerTableModelFilter<FilePackage, DownloadLink>() {

                @Override
                public boolean isFilteringPackageNodes() {
                    return false;
                }

                @Override
                public boolean isFilteringChildrenNodes() {
                    return true;
                }

                @Override
                public synchronized boolean isFiltered(DownloadLink link) {

                    PluginProgress prog = link.getPluginProgress();
                    if (prog != null) {
                        String txt = prog.getMessage(DownloadsTableModel.getInstance().getTaskColumn());
                        for (Pattern filterPattern : pattern) {
                            if (filterPattern.matcher(txt).find()) {
                                return false;
                            }
                        }
                        return true;
                    }
                    ConditionalSkipReason conditionalSkipReason = link.getConditionalSkipReason();
                    if (conditionalSkipReason != null && !conditionalSkipReason.isConditionReached()) {

                        String txt = conditionalSkipReason.getMessage(DownloadsTableModel.getInstance().getTaskColumn(), null);
                        for (Pattern filterPattern : pattern) {
                            if (filterPattern.matcher(txt).find()) {
                                return false;
                            }
                        }
                        return true;
                    }
                    SkipReason skipReason = link.getSkipReason();
                    if (skipReason != null) {

                        String txt = skipReason.getExplanation(DownloadsTableModel.getInstance().getTaskColumn());

                        for (Pattern filterPattern : pattern) {
                            if (filterPattern.matcher(txt).find()) {
                                return false;
                            }
                        }
                        return true;
                    }
                    final FinalLinkState finalLinkState = link.getFinalLinkState();
                    if (finalLinkState != null) {
                        if (FinalLinkState.CheckFailed(finalLinkState)) {

                            String txt = finalLinkState.getExplanation(DownloadsTableModel.getInstance().getTaskColumn(), link);

                            for (Pattern filterPattern : pattern) {
                                if (filterPattern.matcher(txt).find()) {
                                    return false;
                                }
                            }
                            return true;
                        }
                        ExtractionStatus extractionStatus = link.getExtractionStatus();
                        if (extractionStatus != null) {
                            switch (extractionStatus) {
                            case ERROR:
                            case ERROR_PW:
                            case ERROR_CRC:
                            case ERROR_NOT_ENOUGH_SPACE:
                            case ERRROR_FILE_NOT_FOUND:

                                String txt = extractionStatus.getExplanation();
                                for (Pattern filterPattern : pattern) {
                                    if (filterPattern.matcher(txt).find()) {
                                        return false;
                                    }
                                }
                                return true;
                            case SUCCESSFUL:

                                txt = extractionStatus.getExplanation();
                                for (Pattern filterPattern : pattern) {
                                    if (filterPattern.matcher(txt).find()) {
                                        return false;
                                    }
                                }
                                return true;
                            case RUNNING:

                                txt = extractionStatus.getExplanation();
                                for (Pattern filterPattern : pattern) {
                                    if (filterPattern.matcher(txt).find()) {
                                        return false;
                                    }
                                }
                                return true;
                            }
                        }

                        String txt = finalLinkState.getExplanation(this, link);
                        for (Pattern filterPattern : pattern) {
                            if (filterPattern.matcher(txt).find()) {
                                return false;
                            }
                        }
                        return true;
                    }
                    if (link.getDownloadLinkController() != null) {
                        String txt = _GUI._.TaskColumn_fillColumnHelper_starting();
                        for (Pattern filterPattern : pattern) {
                            if (filterPattern.matcher(txt).find()) {
                                return false;
                            }
                        }
                        return true;

                    }

                    return true;
                }

                @Override
                public boolean isFiltered(FilePackage e) {
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

    public static DownloadsTableSearchField getInstance() {
        return new EDTHelper<DownloadsTableSearchField>() {

            @Override
            public DownloadsTableSearchField edtRun() {
                if (INSTANCE != null) {
                    return INSTANCE;
                }
                INSTANCE = new DownloadsTableSearchField(DownloadsTableModel.getInstance().getTable(), LinktablesSearchCategory.FILENAME);
                return INSTANCE;
            }

        }.getReturnValue();

    }

}