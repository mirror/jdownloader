package org.jdownloader.extensions.translator.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.lang.reflect.Type;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.nutils.encoding.Encoding;

import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtIconColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.translator.TranslateEntry;
import org.jdownloader.extensions.translator.TranslatorExtension;
import org.jdownloader.images.NewTheme;

/**
 * The Tablemodel defines all columns and renderers
 * 
 * @author thomas
 * 
 */
public class TranslateTableModel extends ExtTableModel<TranslateEntry> {

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
                        if (e.isParameterInvalid())
                            p = 10;
                        else if (e.isMissing())
                            p = 11;

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
            public int getMaxWidth() {
                return getDefaultWidth();
            }

            @Override
            public int getMinWidth() {
                return getDefaultWidth();
            }

            @Override
            protected boolean onSingleClick(MouseEvent e, TranslateEntry obj) {

                ToolTipController.getInstance().show(createToolTip(e.getPoint(), obj));
                return true;
            }

            @Override
            public boolean matchSearch(TranslateEntry obj, Pattern pattern) {

                if (pattern.pattern().equals(".*?\\Q>e\\E.*?") && obj.isParameterInvalid()) return true;
                if (pattern.pattern().equals(".*?\\Q>m\\E.*?") && obj.isMissing()) return true;
                if (pattern.pattern().equals(".*?\\Q>d\\E.*?") && obj.isDefault()) return true;
                return false;
            }

            @Override
            protected Icon getIcon(TranslateEntry obj) {
                if (obj.isMissing()) {
                    return NewTheme.I().getIcon("stop", 16);
                } else if (obj.isParameterInvalid()) {
                    return NewTheme.I().getIcon("error", 16);

                } else {
                    return NewTheme.I().getIcon("ok", 16);
                }
            }

            @Override
            protected String getTooltipText(TranslateEntry value) {
                return createInfoHtml(value);
            }

        });

        addColumn(new ExtTextColumn<TranslateEntry>("Category") {
            @Override
            public int getDefaultWidth() {
                return 100;
            }

            @Override
            public int getMaxWidth() {
                return getDefaultWidth();
            }

            @Override
            public int getMinWidth() {
                return getDefaultWidth();
            }

            @Override
            protected String getTooltipText(TranslateEntry value) {
                return createInfoHtml(value);
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
                return createInfoHtml(value);
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

            }

            public void configureEditorComponent(final TranslateEntry value, final boolean isSelected, final int row, final int column) {
                super.configureEditorComponent(value, isSelected, row, column);
                if (value.isMissing()) editorField.setText("");

            }

            @Override
            protected String getTooltipText(TranslateEntry value) {

                return createInfoHtml(value);
            }

            @Override
            public String getStringValue(TranslateEntry value) {
                if (value.isMissing()) return " - - - - - missing - - - - - ";
                return value.getTranslation();
            }
        });

    }

    protected String createInfoHtml(TranslateEntry value) {
        String ret = "<html><style>td.a{font-style:italic;}</style><table valign=top>";
        ret += "<tr><td class=a>Key:</td><td>" + value.getKey() + "</td></tr>";

        ret += "<tr><td class=a>Location:</td><td>" + value.getFullKey() + "</td></tr>";
        ret += "<tr><td class=a>Source:</td><td>" + value.getSource() + "</td></tr>";
        ret += "<tr><td class=a>Default:</td><td>" + Encoding.cdataEncode(value.getDefault()) + "</td></tr>";
        ret += "<tr><td class=a>Translation:</td><td>" + Encoding.cdataEncode(value.getTranslation()) + "</td></tr>";
        if (value.isMissing()) {
            ret += "<tr><td class=a><font color='#ff0000' >Error:</font></td><td class=a><font color='#ff0000' >Not translated yet</font></td></tr>";

        }
        if (value.isDefault()) {
            ret += "<tr><td class=a><font color='#339900' >Warning:</font></td><td class=a><font color='#339900' >The translation equals the english default language.</font></td></tr>";
        }

        if (value.isParameterInvalid()) {
            ret += "<tr><td class=a><font color='#ff0000' >Error:</font></td><td class=a><font color='#ff0000' >Parameter Wildcards (%s*) do not match.</font></td></tr>";
        }

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

}
