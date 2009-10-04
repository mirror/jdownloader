package jd.plugins.optional.customizer;

import java.util.ArrayList;

import jd.gui.swing.components.table.JDTable;

public class CustomizerTable extends JDTable {

    private static final long serialVersionUID = 2767338885884748758L;

    public CustomizerTable(ArrayList<CustomizeSetting> settings) {
        super(new CustomizerTableModel("customizerview", settings));
    }

    @Override
    public CustomizerTableModel getModel() {
        return (CustomizerTableModel) super.getModel();
    }

}
