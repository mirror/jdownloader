package org.jdownloader.extensions.translator.gui;

import java.awt.Color;
import java.util.regex.Pattern;

import javax.swing.Icon;

import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtIconColumn;
import org.appwork.utils.swing.table.columns.ExtTextColumn;
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

    public TranslateTableModel() {
        // this is is used to store table states(sort,column positions,
        // properties)
        super("TranslateTableModel");
    }

    @Override
    protected void initColumns() {
        // we init and add all columns here.

        addColumn(new ExtIconColumn<TranslateEntry>("!") {

            @Override
            public int getDefaultWidth() {
                return 24;
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
                if (pattern.pattern().equals(".*?\\Q>w\\E.*?") && obj.hasWarnings() && !obj.isMissing()) return true;
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
                } else if (obj.hasWarnings()) {
                    return NewTheme.I().getIcon("warning_blue", 16);
                    // } else if (obj.isDefault()) {
                    // return NewTheme.I().getIcon("flags/en", 16);
                } else {
                    return null;
                    // return NewTheme.I().getIcon("ok", 16);
                }
            }

            @Override
            protected String getTooltipText(TranslateEntry obj) {
                if (obj.hasErrors() || obj.hasWarnings()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("<html>");
                    for (TranslationProblem e : obj.getErrors()) {
                        sb.append(e.toString());
                        sb.append("<br>");
                    }
                    sb.append("</html>");
                    return sb.toString();
                } else if (obj.getTranslation().equals(obj.getDefault())) {
                    return "[DEFAULT] Translation is equal to the default value.";
                } else {
                    return "[OK] Translation validated successfully.";
                }
            }

        });

        /*
         * addColumn(new ExtTextColumn<TranslateEntry>("#") {
         * 
         * @Override public void configureRendererComponent(TranslateEntry
         * value, boolean isSelected, boolean hasFocus, int row, int column) {
         * super.configureRendererComponent(value, isSelected, hasFocus, row,
         * column); this.renderer.setText(row + ""); }
         * 
         * @Override public int getDefaultWidth() { return 40; }
         * 
         * @Override protected int getMaxWidth() { return getDefaultWidth(); }
         * 
         * @Override public boolean matchSearch(TranslateEntry object, Pattern
         * pattern) { return false; }
         * 
         * @Override public int getMinWidth() { return getDefaultWidth(); }
         * 
         * @Override public String getStringValue(TranslateEntry value) { return
         * null; } });
         */

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
                return value.getFullKey();
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
                if (value.hasErrors()) {
                    renderer.setForeground(errorColor);
                } else if (value.hasWarnings()) {
                    renderer.setForeground(warningColor);
                } else if (value.isDefault()) {
                    renderer.setForeground(defaultColor);
                }

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
}
