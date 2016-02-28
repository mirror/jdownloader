package jd.gui.swing.dialog;

import org.appwork.swing.exttable.ExtTableModel;
import org.jdownloader.credits.Credit;
import org.jdownloader.credits.CreditsManager;

public class CreditsTableModel extends ExtTableModel<Credit> {

    public CreditsTableModel() {
        super("CreditsTableModel");
        setTableData(CreditsManager.getInstance().list());
    }

    @Override
    protected void initColumns() {
    }

}
