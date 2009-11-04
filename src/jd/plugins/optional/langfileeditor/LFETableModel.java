//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.langfileeditor;

import java.util.Collections;
import java.util.Comparator;

import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.langfileeditor.columns.EnglishColumn;
import jd.plugins.optional.langfileeditor.columns.KeyColumn;
import jd.plugins.optional.langfileeditor.columns.LanguageColumn;
import jd.plugins.optional.langfileeditor.columns.SourceColumn;
import jd.utils.locale.JDL;

public class LFETableModel extends JDTableModel {

    public static final int SORT_KEY = 0;
    public static final int SORT_SOURCE = 1;
    public static final int SORT_LANGUAGE = 2;
    public static final int SORT_ENGLISH = 3;

    private static final long serialVersionUID = -1775792404758292253L;
    private static final String LOCALE_PREFIX = "plugins.optional.langfileeditor.";

    private Integer sorting = SORT_KEY;
    private Boolean toggle = true;

    private LFEGui gui;

    public LFETableModel(LFEGui gui) {
        super("lfetable");
        this.gui = gui;
    }

    public void setSorting(int sorting, boolean toggle) {
        this.sorting = sorting;
        this.toggle = toggle;
        refreshModel();
        fireTableDataChanged();
    }

    public LFEGui getGui() {
        return gui;
    }

    @Override
    protected void initColumns() {
        this.addColumn(new KeyColumn(JDL.L(LOCALE_PREFIX + "key", "Key"), this));
        this.addColumn(new SourceColumn(JDL.L(LOCALE_PREFIX + "sourceValue", "Default Value"), this));
        this.addColumn(new EnglishColumn(JDL.L(LOCALE_PREFIX + "english", "en.loc"), this));
        this.addColumn(new LanguageColumn(JDL.L(LOCALE_PREFIX + "languageFileValue", "Language File Value"), this));
    }

    @Override
    public void refreshModel() {
        synchronized (list) {
            list.clear();
            if (sorting != null && toggle != null) {
                Collections.sort(gui.getData(), new Comparator<KeyInfo>() {

                    public int compare(KeyInfo o1, KeyInfo o2) {
                        if (!toggle) return compareInner(o2, o1);
                        return compareInner(o1, o2);
                    }

                    private int compareInner(KeyInfo o1, KeyInfo o2) {
                        switch (sorting) {
                        case SORT_KEY:
                            return o1.getKey().compareToIgnoreCase(o2.getKey());
                        case SORT_SOURCE:
                            return o1.getSource().compareToIgnoreCase(o2.getSource());
                        case SORT_LANGUAGE:
                            return o1.getLanguage().compareToIgnoreCase(o2.getLanguage());
                        case SORT_ENGLISH:
                            return o1.getEnglish().compareToIgnoreCase(o2.getEnglish());
                        }
                        return 0;
                    }

                });
            }
            list.addAll(gui.getData());
        }
    }
}
