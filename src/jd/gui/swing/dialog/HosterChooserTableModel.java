package jd.gui.swing.dialog;

import java.awt.Component;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtIconColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.CompareUtils;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.extmanager.Log;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

public class HosterChooserTableModel extends ExtTableModel<LazyHostPlugin> {

    /**
     *
     */
    private static final long    serialVersionUID = 1L;
    private List<LazyHostPlugin> allPlugins;
    private String               text;

    public HosterChooserTableModel(List<LazyHostPlugin> plugins) {
        super("HosterChooserTableModel");
        this.allPlugins = plugins;

    }

    @Override
    protected int[] guessSelectedRows(List<LazyHostPlugin> oldTableData, int leadIndex, int anchorIndex, BitSet selectedRowsBitSet) {
        return new int[] { 0 };
    }

    @Override
    public void _fireTableStructureChanged(java.util.List<LazyHostPlugin> newtableData, boolean refreshSort) {
        final String ltext = text;
        if (!StringUtils.isEmpty(ltext)) {
            try {
                final String p = toRegex(ltext);
                final Pattern pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);
                main: for (final Iterator<LazyHostPlugin> it = newtableData.iterator(); it.hasNext();) {
                    final LazyHostPlugin next = it.next();
                    if (pattern.matcher(clean(next.getHost())).find()) {
                        continue;
                    }
                    final FEATURE[] features = next.getFeatures();
                    if (features != null) {
                        for (final FEATURE f : features) {
                            if (pattern.matcher(clean(f.getLabel())).find()) {
                                continue main;
                            }
                        }
                    }
                    it.remove();
                }
            } catch (Throwable e) {
                Log.log(e);
            }
        }
        super._fireTableStructureChanged(newtableData, refreshSort);
    }

    @Override
    public List<LazyHostPlugin> getSelectedObjects(int maxItems, BitSet selectedRows) {
        final List<LazyHostPlugin> ret = super.getSelectedObjects(maxItems, selectedRows);
        if (ret == null || ret.size() == 0) {
            final List<LazyHostPlugin> ltableData = getTableData();
            if (ltableData != null && ltableData.size() > 0) {
                final List<LazyHostPlugin> ret2 = new ArrayList<LazyHostPlugin>();
                ret2.add(ltableData.get(0));
                return ret2;
            }
        }
        return ret;
    }

    @Override
    public List<LazyHostPlugin> sort(List<LazyHostPlugin> data, ExtColumn<LazyHostPlugin> column) {
        if (StringUtils.isEmpty(text)) {
            final Comparator<LazyHostPlugin> compar = new Comparator<LazyHostPlugin>() {

                @Override
                public int compare(LazyHostPlugin o1, LazyHostPlugin o2) {
                    return o1.getHost().compareToIgnoreCase(o2.getHost());

                }
            };
            Collections.sort(data, compar);
            return data;
        } else {
            final Pattern patternStarts = Pattern.compile("^" + toRegex(text) + ".*$", Pattern.CASE_INSENSITIVE);
            final Comparator<LazyHostPlugin> compar = new Comparator<LazyHostPlugin>() {
                @Override
                public int compare(LazyHostPlugin o1, LazyHostPlugin o2) {
                    int ret = CompareUtils.compare(patternStarts.matcher(clean(o2.getHost())).matches(), patternStarts.matcher(clean(o1.getHost())).matches());
                    if (ret == 0) {
                        ret = o1.getHost().compareToIgnoreCase(o2.getHost());
                    }
                    return ret;
                }
            };
            Collections.sort(data, compar);
            return data;
        }
    }

    protected String toRegex(String text) {
        return clean(text).replaceAll("\\W", ".*");
    }

    public void refresh(final String filterText) {
        this.text = clean(filterText);
        _fireTableStructureChanged(new ArrayList<LazyHostPlugin>(allPlugins), true);
    }

    private String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ENGLISH).replaceAll("\\W", "").replace("z", "s").replace("b", "p").replace("y", "i").replace("j", "i");
    }

    @Override
    protected void initColumns() {
        addColumn(new ExtIconColumn<LazyHostPlugin>(_GUI.T.HosterChooserTableModel_column_icon()) {
            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    private static final long serialVersionUID = 3938290423337000265L;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(new AbstractIcon(IconKey.ICON_IMAGE, 16));
                        // defaultProxy
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        setToolTipText(_GUI.T.HosterChooserTableModel_column_icon());
                        return this;
                    }
                };
                return ret;
            }

            @Override
            public boolean isResizable() {
                return false;
            }

            @Override
            public boolean isSortable(LazyHostPlugin obj) {
                return false;
            }

            @Override
            protected boolean isDefaultResizable() {
                return false;
            }

            @Override
            protected Icon getIcon(LazyHostPlugin value) {
                return DomainInfo.getInstance(value.getHost()).getIcon(16);
            }
        });
        addColumn(new ExtTextColumn<LazyHostPlugin>(_GUI.T.HosterChooserTableModel_column_domain()) {
            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public boolean isSortable(LazyHostPlugin obj) {
                return false;
            }

            @Override
            public String getStringValue(LazyHostPlugin value) {
                return value.getDisplayName();
            }
        });

        addColumn(new ExtTextColumn<LazyHostPlugin>(_GUI.T.HosterChooserTableModel_column_features()) {
            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public boolean isSortable(LazyHostPlugin obj) {
                return false;
            }

            @Override
            protected String getTooltipText(LazyHostPlugin value) {
                StringBuilder sb = new StringBuilder();
                FEATURE[] features = value.getFeatures();
                if (features != null) {
                    for (FEATURE f : features) {
                        if (sb.length() > 0) {
                            sb.append("\r\n");
                        }
                        sb.append(f.getTooltip());
                    }
                }
                return sb.toString();
            }

            @Override
            public String getStringValue(LazyHostPlugin value) {
                StringBuilder sb = new StringBuilder();
                FEATURE[] features = value.getFeatures();
                if (features != null) {
                    for (FEATURE f : features) {
                        if (sb.length() > 0) {
                            sb.append("; ");
                        }
                        sb.append(f.getLabel());
                    }
                }
                return sb.toString();
            }
        });
    }

}
