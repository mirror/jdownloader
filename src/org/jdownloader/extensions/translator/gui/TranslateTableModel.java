package org.jdownloader.extensions.translator.gui;

import java.awt.Color;
import java.util.regex.Pattern;

import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.extensions.translator.TranslateEntry;
import org.jdownloader.extensions.translator.TranslationError;
import org.jdownloader.extensions.translator.TranslatorExtension;

public class TranslateTableModel extends ExtTableModel<TranslateEntry> {

    public TranslateTableModel() {
        super("TranslateTableModel");
    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<TranslateEntry>("#") {

            @Override
            public void configureRendererComponent(TranslateEntry value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.configureRendererComponent(value, isSelected, hasFocus, row, column);
                this.renderer.setText(row + "");
            }

            @Override
            public int getDefaultWidth() {
                return 40;
            }

            @Override
            protected int getMaxWidth() {
                return getDefaultWidth();
            }

            @Override
            public boolean matchSearch(TranslateEntry object, Pattern pattern) {
                return false;
            }

            @Override
            public int getMinWidth() {
                return getDefaultWidth();
            }

            @Override
            public String getStringValue(TranslateEntry value) {
                return null;
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
            protected String getToolTip(TranslateEntry value) {
                return value.getFullKey();
            }

            @Override
            public String getStringValue(TranslateEntry value) {
                return value.getKey() + "";
            }
        });

        addColumn(new ExtTextColumn<TranslateEntry>("Translation") {

            private Color errorColor   = Color.RED;
            private Color warningColor = Color.ORANGE;

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
            protected String getToolTip(TranslateEntry obj) {
                StringBuilder sb = new StringBuilder();
                sb.append("<html>");
                for (TranslationError e : obj.getErrors()) {
                    sb.append(e.toString());
                    sb.append("<br>");
                }
                sb.append("</html>");
                return sb.toString();
            }

            @Override
            public void configureRendererComponent(TranslateEntry value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.configureRendererComponent(value, isSelected, hasFocus, row, column);
                boolean hasError = false;
                boolean hasWarnings = false;
                for (TranslationError e : value.getErrors()) {
                    switch (e.getType()) {
                    case ERROR:
                        hasError = true;
                        break;
                    case WARNING:
                        hasWarnings = true;
                    }

                }

                if (hasError) {
                    renderer.setForeground(errorColor);
                } else if (hasWarnings) {
                    renderer.setForeground(warningColor);
                }

            }

            @Override
            public String getStringValue(TranslateEntry value) {
                return value.getTranslation();
            }
        });

    }

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
