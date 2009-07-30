package jd.gui.swing;

import java.awt.Color;

import javax.swing.table.TableModel;

public abstract class RowHighlighter<T extends TableModel> {
   
    protected T model;

    protected Color color;
    
    public RowHighlighter(T model, Color color) {
        this.model = model;
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public abstract boolean doHighlight(int row);

}
