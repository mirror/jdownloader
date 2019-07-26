package org.jdownloader.plugins.components.youtube.configpanel;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.JTableHeader;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.CounterMap;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.components.youtube.VariantIDStorable;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;

public class CollectionsTableModel extends ExtTableModel<YoutubeVariantCollection> implements GenericConfigEventListener<Object> {
    private abstract class AutoResizingTextColumn extends ExtTextColumn<YoutubeVariantCollection> {
        private AutoResizingTextColumn(String name) {
            super(name);
            this.setRowSorter(new ExtDefaultRowSorter<YoutubeVariantCollection>() {
                @Override
                public int compare(final YoutubeVariantCollection o1, final YoutubeVariantCollection o2) {
                    String o1s = getStringValue(o1);
                    String o2s = getStringValue(o2);
                    if (o1s == null) {
                        o1s = "";
                    }
                    if (o2s == null) {
                        o2s = "";
                    }
                    if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                        int ret = o1s.compareToIgnoreCase(o2s);
                        return ret;
                    } else {
                        int ret = o2s.compareToIgnoreCase(o1s);
                        return ret;
                    }
                }
            });
        }

        @Override
        public boolean isEnabled(YoutubeVariantCollection obj) {
            return obj.isEnabled();
        }

        @Override
        protected boolean isDefaultResizable() {
            return true;
        }

        @Override
        protected String getTooltipText(YoutubeVariantCollection obj) {
            return null;
        }

        @Override
        public boolean isResizable() {
            return true;
        }

        @Override
        public boolean isAutoWidthEnabled() {
            return true;
        }

        @Override
        public int getDefaultWidth() {
            return this.calculateMinimumHeaderWidth();
        }

        @Override
        public int getMinWidth() {
            return this.calculateMinimumHeaderWidth();
        }

        @Override
        protected int adjustWidth(int w) {
            return Math.max(w, this.calculateMinimumHeaderWidth());
        }
    }

    private abstract class AutoResizingIntColumn extends ExtTextColumn<YoutubeVariantCollection> {
        private AutoResizingIntColumn(String name) {
            super(name);
            rendererField.setHorizontalAlignment(SwingConstants.RIGHT);
            this.setRowSorter(new ExtDefaultRowSorter<YoutubeVariantCollection>() {
                @Override
                public int compare(final YoutubeVariantCollection o1, final YoutubeVariantCollection o2) {
                    final int _1 = AutoResizingIntColumn.this.getInt(o1);
                    final int _2 = AutoResizingIntColumn.this.getInt(o2);
                    int ret;
                    if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                        ret = _1 == _2 ? 0 : _1 < _2 ? -1 : 1;
                    } else {
                        ret = _1 == _2 ? 0 : _1 > _2 ? -1 : 1;
                    }
                    return ret;
                }
            });
        }

        @Override
        public boolean isEnabled(YoutubeVariantCollection obj) {
            return obj.isEnabled();
        }

        public abstract int getInt(YoutubeVariantCollection value);

        @Override
        public String getStringValue(YoutubeVariantCollection value) {
            int i = getInt(value);
            if (i <= 0) {
                return "";
            }
            return i + "";
        }

        @Override
        protected String getTooltipText(YoutubeVariantCollection obj) {
            return null;
        }

        @Override
        public boolean isAutoWidthEnabled() {
            return true;
        }

        @Override
        protected boolean isDefaultResizable() {
            return true;
        }

        @Override
        public boolean isResizable() {
            return true;
        }

        @Override
        public int getDefaultWidth() {
            return this.calculateMinimumHeaderWidth();
        }

        @Override
        public int getMinWidth() {
            return this.calculateMinimumHeaderWidth();
        }

        @Override
        protected int adjustWidth(int w) {
            return Math.max(w, this.calculateMinimumHeaderWidth());
        }
    }

    private class EnabledColumn extends ExtCheckColumn<YoutubeVariantCollection> {
        private EnabledColumn(String string) {
            super(string);
            this.setRowSorter(new ExtDefaultRowSorter<YoutubeVariantCollection>() {
                @Override
                public int compare(final YoutubeVariantCollection o1, final YoutubeVariantCollection o2) {
                    final boolean b1 = getBooleanValue(o1);
                    final boolean b2 = getBooleanValue(o2);
                    int ret;
                    if (b1 == b2) {
                        ret = 0;
                    } else {
                        if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                            ret = b1 && !b2 ? -1 : 1;
                        } else {
                            ret = !b1 && b2 ? -1 : 1;
                        }
                    }
                    return ret;
                }
            });
        }

        public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {
            final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {
                private final Icon        ok               = NewTheme.I().getIcon(IconKey.ICON_OK, 14);
                private static final long serialVersionUID = 3224931991570756349L;

                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    setIcon(ok);
                    setHorizontalAlignment(CENTER);
                    setText(null);
                    return this;
                }
            };
            return ret;
        }

        @Override
        public int getMaxWidth() {
            return 30;
        }

        @Override
        public boolean isEditable(YoutubeVariantCollection obj) {
            return true;
        }

        @Override
        protected boolean getBooleanValue(YoutubeVariantCollection value) {
            return value.isEnabled();
        }

        @Override
        protected void setBooleanValue(boolean value, YoutubeVariantCollection object) {
            object.setEnabled(value);
        }
    }

    private CounterMap<String> enabledMap;

    public CollectionsTableModel() {
        super("YoutubeLinkTableModel");
        // ensure Link and its statics are loaded
        CFG_YOUTUBE.COLLECTIONS.getEventSender().addListener(this, true);
    }

    @Override
    protected void initColumns() {
        addColumn(new EnabledColumn(_GUI.T.lit_enabled()) {
            @Override
            protected void setBooleanValue(boolean value, YoutubeVariantCollection object) {
                super.setBooleanValue(value, object);
                save();
            }
        });
        addColumn(new AutoResizingTextColumn(_GUI.T.lit_name()) {
            @Override
            public boolean isAutoWidthEnabled() {
                return false;
            }

            @Override
            public boolean isEditable(YoutubeVariantCollection obj) {
                return true;
            }

            @Override
            public boolean isEnabled(YoutubeVariantCollection obj) {
                return getEnabledCount(obj) > 0 && obj.isEnabled();
            }

            @Override
            protected void setStringValue(String value, YoutubeVariantCollection object) {
                object.setName(value);
                save();
            }

            @Override
            public String getStringValue(YoutubeVariantCollection value) {
                return value.getName();
            }
        });
        addColumn(new AutoResizingTextColumn(_GUI.T.youtube_collection_size()) {
            @Override
            public boolean isAutoWidthEnabled() {
                return true;
            }

            @Override
            public boolean isEnabled(YoutubeVariantCollection obj) {
                return getEnabledCount(obj) > 0 && obj.isEnabled();
            }

            @Override
            public String getStringValue(YoutubeVariantCollection value) {
                int i = getEnabledCount(value);
                if (value.getVariants() == null) {
                    return i + "";
                }
                return i + "/" + value.getVariants().size();
            }
        });
    }

    public void load() {
        onConfigValueModified(null, null);
    }

    protected void save() {
        CFG_YOUTUBE.CFG.setCollections(getTableData());
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                _fireTableStructureChanged(YoutubeVariantCollection.load(), true);
            }
        };
    }

    /**
     * @param enabledMap
     */
    public void onEnabledMapUpdate(CounterMap<String> enabledMap) {
        this.enabledMap = enabledMap;
        getTable().repaint();
    }

    protected int getEnabledCount(YoutubeVariantCollection value) {
        int i = 0;
        if (enabledMap == null) {
            return 0;
        }
        if (value.getGroupingID() != null) {
            return enabledMap.getInt(value.getGroupingID());
        }
        if (value.getVariants() != null) {
            for (VariantIDStorable v : value.getVariants()) {
                int vi = enabledMap.getInt(v.createUniqueID());
                i += vi;
            }
        }
        return i;
    }
}
