package org.jdownloader.captcha.v2.challenge.keycaptcha.dialog;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtIconColumn;
import org.jdownloader.captcha.v2.challenge.keycaptcha.CategoryData;

public class CatModel extends ExtTableModel<CatOption> {
    public class CatColum extends ExtIconColumn<CatOption> {
        private int catID;

        public CatColum(int i) {
            super("Column " + i);
            catID = i;
        }

        @Override
        public boolean isResizable() {
            return true;
        }

        public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

            final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    setIcon(cats[catID]);
                    setHorizontalAlignment(CENTER);
                    setText(null);
                    return this;
                }

            };

            return ret;
        }

        @Override
        public boolean onDoubleClick(MouseEvent e, CatOption obj) {
            return onSingleClick(e, obj);
        }

        @Override
        public boolean onSingleClick(MouseEvent e, CatOption obj) {
            obj.setSelected(catID);
            int row = getRowforObject(obj);
            getTable().scrollToRow(row, 0);
            getTable().repaint();

            // System.out.println(e);
            return true;
        }

        @Override
        public void configureRendererComponent(CatOption value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.configureRendererComponent(value, isSelected, hasFocus, row, column);

            if (getTable().getRowHeight(row) != value.getIcon(catID).getIconHeight()) {
                getTable().setRowHeight(row, value.getIcon(catID).getIconHeight());
            }
        }

        @Override
        protected Icon getIcon(CatOption value) {

            return value.getIcon(catID);
        }

        @Override
        public int getDefaultWidth() {

            return -1;
        }

        @Override
        protected boolean isDefaultResizable() {
            return true;
        }

        @Override
        public boolean isDefaultVisible() {
            return true;
        }

        @Override
        public boolean isSortable(CatOption obj) {
            return false;
        }

        @Override
        public boolean isHidable() {
            return false;
        }

        @Override
        public int getMaxWidth() {
            return -1;
        }

        @Override
        public int getMinWidth() {
            return -1;
        }
        // @Override
        // public String getStringValue(CatOption value) {
        // return "";
        // }
    }

    private ImageIcon[] cats;

    public CatModel(CategoryData data) {
        super("CatModel");
        int i = 0;
        cats = new ImageIcon[3];
        BufferedImage bg = data.getBackground();
        for (i = 0; i < 3; i++) {

            BufferedImage cats = new BufferedImage(bg.getWidth() / 3, bg.getHeight(), Transparency.TRANSLUCENT);

            Graphics2D g = (Graphics2D) cats.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            g.drawImage(bg, 0, 0, bg.getWidth() / 3, bg.getHeight(), i * bg.getWidth() / 3, 0, (i + 1) * bg.getWidth() / 3, bg.getHeight(), null);
            g.dispose();
            this.cats[i] = new ImageIcon(cats);
        }

        for (Image img : data.getImages()) {

            addElement(new CatOption(i++, img));
        }

    }

    @Override
    protected void initColumns() {
        //
        addColumn(new CatColum(0) {
        });
        addColumn(new CatColum(1) {
        });
        addColumn(new CatColum(2) {
        });

    }

}
