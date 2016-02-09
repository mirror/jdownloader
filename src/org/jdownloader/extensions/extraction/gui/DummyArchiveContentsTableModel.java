package org.jdownloader.extensions.extraction.gui;

import javax.swing.Icon;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.DummyArchiveFile;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFile;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

import jd.plugins.DownloadLink.AvailableStatus;

public class DummyArchiveContentsTableModel extends ExtTableModel<DummyArchiveFile> {

    private ExtTextColumn<DummyArchiveFile> local;
    private ExtTextColumn<DummyArchiveFile> linkStatus;
    private ExtTextColumn<DummyArchiveFile> name;

    public ExtTextColumn<DummyArchiveFile> getLocal() {
        return local;
    }

    public DummyArchiveContentsTableModel(DummyArchive da) {
        super("DummyArchiveContentsTableModel");
        _fireTableStructureChanged(da.getList(), true);
    }

    @Override
    protected void initColumns() {
        addColumn(name = new ExtTextColumn<DummyArchiveFile>(T.T.filename()) {

            @Override
            public String getStringValue(DummyArchiveFile value) {
                return value.getName();
            }

            @Override
            protected String getTooltipText(DummyArchiveFile value) {
                if (value.getArchiveFile() instanceof FileArchiveFile) {
                    if (((FileArchiveFile) value.getArchiveFile()).getFile().exists()) {
                        return T.T.file_exists();
                    } else {
                        return T.T.file_exists_not();
                    }
                } else {
                    if (value.getArchiveFile() == null) {
                        if (value.isMissing() || Boolean.TRUE.equals(value.isIncomplete())) {
                            return T.T.file_exists_not();
                        }
                        return T.T.unknown_tt();
                    } else {
                        if (value.isMissing() || Boolean.TRUE.equals(value.isIncomplete())) {
                            return T.T.offline_tt();
                        }
                        if (value.getOnlineStatus() == AvailableStatus.TRUE) {
                            return T.T.online_tt();
                        }
                        return T.T.unknown_tt();
                    }
                }
            }

        });

        addColumn(linkStatus = new ExtTextColumn<DummyArchiveFile>(T.T.exists()) {
            private Icon unknown;
            private Icon online;

            private Icon offline;

            {

                unknown = new AbstractIcon(IconKey.ICON_HELP, 16);
                online = new AbstractIcon(IconKey.ICON_TRUE, 16);

                offline = new AbstractIcon(IconKey.ICON_ERROR, 16);
            }

            @Override
            protected Icon getIcon(DummyArchiveFile value) {
                if (value.getOnlineStatus() == AvailableStatus.TRUE) {
                    return online;
                }
                if (value.isMissing() || Boolean.TRUE.equals(value.isIncomplete())) {
                    return offline;
                }
                return unknown;
            }

            @Override
            public String getStringValue(DummyArchiveFile value) {
                if (value.getOnlineStatus() == AvailableStatus.TRUE) {
                    return T.T.online();
                }
                if (value.isMissing() || Boolean.TRUE.equals(value.isIncomplete())) {
                    return T.T.offline();
                }
                return T.T.unknown();
            }

            @Override
            protected String getTooltipText(DummyArchiveFile value) {
                if (value.getOnlineStatus() == AvailableStatus.TRUE) {
                    return T.T.online_tt();
                }
                if (value.isMissing() || Boolean.TRUE.equals(value.isIncomplete())) {
                    return T.T.offline_tt();
                }
                return T.T.unknown_tt();
            }

        });

        addColumn(local = new ExtTextColumn<DummyArchiveFile>(T.T.local()) {

            @Override
            protected Icon getIcon(DummyArchiveFile value) {
                if (value.isLocalFileAvailable()) {
                    return new AbstractIcon(IconKey.ICON_TRUE, 16);
                }
                return new AbstractIcon(IconKey.ICON_FALSE, 16);

            }

            @Override
            public String getStringValue(DummyArchiveFile value) {
                if (value.isLocalFileAvailable()) { //
                    return T.T.downloadedok();
                }
                return T.T.downloadedbad();
            }

            @Override
            protected String getTooltipText(DummyArchiveFile value) {
                return getStringValue(value);
            }

        });
    }

    public ExtTextColumn<DummyArchiveFile> getLinkStatus() {
        return linkStatus;
    }

    public ExtTextColumn<DummyArchiveFile> getName() {
        return name;
    }

}
