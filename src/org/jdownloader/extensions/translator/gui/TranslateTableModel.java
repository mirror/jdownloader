package org.jdownloader.extensions.translator.gui;

import java.awt.Color;
import java.awt.Component;
import java.lang.reflect.Type;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.nutils.encoding.Encoding;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtIconColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.translator.TranslateEntry;
import org.jdownloader.extensions.translator.TranslationProblem;
import org.jdownloader.extensions.translator.TranslatorExtension;
import org.jdownloader.images.NewTheme;

/**
 * The Tablemodel defines all columns and renderers
 * 
 * @author thomas
 * 
 */
public class TranslateTableModel extends ExtTableModel<TranslateEntry> {

    private boolean markDefaults = false;
    private boolean markOK       = true;

    public TranslateTableModel() {
        // this is is used to store table states(sort,column positions,
        // properties)
        super("TranslateTableModel");
    }

    @Override
    protected void initColumns() {
        // we init and add all columns here.

        addColumn(new ExtIconColumn<TranslateEntry>("Status") {

            {
                setRowSorter(new ExtDefaultRowSorter<TranslateEntry>() {

                    public int getPriority(TranslateEntry e) {
                        int p = 100;
                        if (e.hasErrors())
                            p = 10;
                        else if (e.isMissing())
                            p = 11;
                        else if (e.isWrongLength())
                            p = 20;
                        else if (e.isDefault()) p = 30;
                        return p;
                    }

                    @Override
                    public int compare(final TranslateEntry o1, final TranslateEntry o2) {

                        final int w1 = getPriority(o1);
                        final int w2 = getPriority(o2);
                        if (w1 == w2) { return 0; }
                        if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                            return w1 > w2 ? -1 : 1;
                        } else {
                            return w2 > w1 ? -1 : 1;
                        }
                    }

                });

            }

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    private static final long serialVersionUID = 3224931991570756349L;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("info", 16));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getDefaultWidth() {

                return 28;
            }

            @Override
            protected int getMaxWidth() {
                return getDefaultWidth();
            }

            @Override
            public int getMinWidth() {
                return getDefaultWidth();
            }

            @Override
            public boolean matchSearch(TranslateEntry obj, Pattern pattern) {
                if (pattern.pattern().equals(".*?\\Q>w\\E.*?") && obj.isWrongLength()) return true;
                if (pattern.pattern().equals(".*?\\Q>e\\E.*?") && obj.hasErrors()) return true;
                if (pattern.pattern().equals(".*?\\Q>m\\E.*?") && obj.isMissing()) return true;
                if (pattern.pattern().equals(".*?\\Q>d\\E.*?") && obj.isDefault()) return true;
                return false;
            }

            @Override
            protected Icon getIcon(TranslateEntry obj) {
                if (obj.isMissing()) {
                    return NewTheme.I().getIcon("prio_0", 16);
                } else if (obj.hasErrors()) {
                    return NewTheme.I().getIcon("error", 16);
                } else if (obj.isWrongLength()) {
                    return NewTheme.I().getIcon("warning", 16);
                } else if (markDefaults && obj.isDefault()) {
                    return NewTheme.I().getIcon("flags/en", 16);
                } else if (markOK) {
                    return NewTheme.I().getIcon("ok", 16);
                } else {
                    return null;
                }
            }

            @Override
            protected String getTooltipText(TranslateEntry obj) {
                if (obj.hasErrors() || obj.isWrongLength() || obj.isMissing()) {
                    String ret = "<html>";
                    for (TranslationProblem e : obj.getErrors()) {
                        ret += e.toString() + "<br>";
                    }
                    ret += "</html>";
                    return ret;
                } else if (obj.getTranslation().equals(obj.getDefault())) {
                    return "[DEFAULT] Translation is equal to the default value.";
                } else {
                    return "[OK] Translation validated successfully.";
                }
            }

        });

        addColumn(new ExtTextColumn<TranslateEntry>("Category") {
            @Override
            public int getDefaultWidth() {
                return 100;
            }

            @Override
            protected int getMaxWidth() {
                return getDefaultWidth();
            }

            @Override
            public int getMinWidth() {
                return getDefaultWidth();
            }

            @Override
            public boolean matchSearch(TranslateEntry object, Pattern pattern) {
                return false;
            }

            @Override
            public String getStringValue(TranslateEntry value) {
                return value.getCategory();
            }
        });

        addColumn(new ExtTextColumn<TranslateEntry>("Key") {
            @Override
            public int getDefaultWidth() {
                return 200;
            }

            @Override
            public boolean matchSearch(TranslateEntry object, Pattern pattern) {
                return false;
            }

            @Override
            protected String getTooltipText(TranslateEntry value) {
                String ret = "<html><style>td.a{font-style:italic;}</style><table valign=top>";
                ret += "<tr><td class=a>Key:</td><td>" + value.getKey() + "</td></tr>";
                ret += "<tr><td class=a>Location:</td><td>" + value.getFullKey() + "</td></tr>";
                ret += "<tr><td class=a>Default:</td><td>" + Encoding.cdataEncode(value.getDefault()) + "</td></tr>";
                Type[] parameters = value.getParameters();
                ret += "<tr><td class=a>Parameters:</td>";
                if (parameters.length == 0) {
                    ret += "<td>none</td></tr>";
                } else {
                    ret += "<td>";
                    int i = 1;
                    for (Type t : parameters) {
                        ret += "   %s" + i + " (" + t + ")<br>";
                        i++;
                    }
                    ret += "</td>";
                }
                ret += "</tr>";
                return ret + "</table></html>";
            }

            @Override
            public String getStringValue(TranslateEntry value) {
                return value.getKey() + "";
            }
        });

        addColumn(new ExtTextColumn<TranslateEntry>("Translation") {

            private Color errorColor   = Color.RED;
            private Color warningColor = Color.BLUE;
            private Color defaultColor = Color.GRAY;

            @Override
            public boolean isEditable(TranslateEntry obj) {
                return true;
            }

            @Override
            public boolean matchSearch(TranslateEntry object, Pattern pattern) {
                boolean ret = super.matchSearch(object, pattern);
                if (!ret) ret |= pattern.matcher(object.getCategory()).matches();
                if (!ret) ret |= pattern.matcher(object.getFullKey()).matches();
                if (!ret) if (object.getDescription() != null) ret |= pattern.matcher(object.getDescription()).matches();
                return ret;
            }

            @Override
            public void setValue(Object value, TranslateEntry object) {
                object.setTranslation((String) value);
            }

            @Override
            public void configureRendererComponent(TranslateEntry value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.configureRendererComponent(value, isSelected, hasFocus, row, column);
                /*
                 * if (value.hasErrors()) { renderer.setForeground(errorColor);
                 * } else if (value.hasWarnings()) {
                 * renderer.setForeground(warningColor); } else if
                 * (value.isDefault()) { renderer.setForeground(defaultColor); }
                 */

            }

            @Override
            protected String getTooltipText(TranslateEntry value) {
                String ret = "<html><style>td.a{font-style:italic;}</style><table valign=top>";
                ret += "<tr><td class=a>Default:</td><td>" + Encoding.cdataEncode(value.getDefault()) + "</td></tr>";
                return ret + "</table></html>";
            }

            @Override
            public String getStringValue(TranslateEntry value) {
                return value.getTranslation();
            }
        });

    }

    /**
     * refresh the tablemodel. for example if we load a new language
     * 
     * @param extension
     */
    public void refresh(final TranslatorExtension extension) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                clear();
                addAllElements(extension.getTranslationEntries().toArray(new TranslateEntry[] {}));
            }
        };
    }

    public boolean isMarkDefaults() {
        return markDefaults;
    }

    public void setMarkDefaults(boolean markDefaults) {
        this.markDefaults = markDefaults;
    }

    public boolean isMarkOK() {
        return markOK;
    }

    public void setMarkOK(boolean markOK) {
        this.markOK = markOK;
    }
}
