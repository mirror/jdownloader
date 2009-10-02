package jd.plugins.optional.customizer;

import java.util.ArrayList;

import jd.gui.swing.components.table.JDTable;

public class CustomizerTable extends JDTable {

    private static final long serialVersionUID = 2767338885884748758L;

    private CustomizerGui gui;

    public CustomizerTable(CustomizerGui gui, ArrayList<CustomizeSetting> settings) {
        super(new CustomizerTableModel("customizerview", settings));

        this.gui = gui;
    }

    public CustomizerGui getGui() {
        return gui;
    }

    @Override
    public CustomizerTableModel getModel() {
        return (CustomizerTableModel) super.getModel();
    }

}
