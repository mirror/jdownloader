package jd.gui.swing.dialog;

import org.jdownloader.credits.Credit;

import jd.gui.swing.jdgui.BasicJDTable;

public class CreditsTable extends BasicJDTable<Credit> {

    public CreditsTable() {
        super(new CreditsTableModel());
    }

}
