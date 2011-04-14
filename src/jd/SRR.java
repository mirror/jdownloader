package jd;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class SRR extends JLabel implements TableCellRenderer {

    public SRR(TableCellRenderer tablecellrenderer) {
        setOpaque(true);
        // Object obj = tablecellrenderer != null ? ((Object) (((JComponent)
        // tablecellrenderer).getBorder())) : ((Object) (new EmptyBorder(0, 0,
        // 0, 0)));
        // Insets insets = obj != null ? ((Border) (obj)).getBorderInsets(null)
        // : new Insets(0, 0, 0, 0);
        // Border border =
        // UIManager.getBorder("Table.focusCellHighlightBorder");
        // Object obj1 = border != null ? ((Object) (border)) : ((Object) (new
        // EmptyBorder(0, 0, 0, 0)));
        // Insets insets1 = ((Border) (obj1)).getBorderInsets(null);
        // Insets insets2 = new Insets(insets.top - insets1.top, insets.left -
        // insets1.left, insets.bottom - insets1.bottom, insets.right -
        // insets1.right);
        // focusBorder = new CompoundBorder(((Border) (obj1)), new
        // EmptyBorder(insets2));
        // noFocusBorder = new CompoundBorder(new EmptyBorder(insets1), new
        // EmptyBorder(insets2));
        // alternateColor = UIManager.getColor("Table.alternateRowColor");
        // colorCachingEnabled =
        // SyntheticaLookAndFeel.getBoolean("Synthetica.table.cellRenderer.colorCache.enabled",
        // null, false);
    }

    // public String getName() {
    // String s = super.getName();
    // if (s == null) s = "Table.cellRenderer";
    // return s;
    // }

    // public void setForeground(Color color) {
    // super.setForeground(color);
    // if (colorCachingEnabled) unselectedForeground = color;
    // }
    //
    // public void setBackground(Color color) {
    // super.setBackground(color);
    // if (colorCachingEnabled) unselectedBackground = color;
    // }

    public void updateUI() {
        super.updateUI();
        setForeground(null);
        setBackground(null);
    }

    public Component getTableCellRendererComponent(JTable jtable, Object obj, boolean flag, boolean flag1, int i, int j) {

        super.setBackground(Color.BLUE);
        // if (flag1)
        // setBorder(focusBorder);
        // else
        // setBorder(noFocusBorder);
        setFont(jtable.getFont());
        setValue(obj);
        setIcon(null);
        // configureValue(obj, jtable.getColumnClass(j));
        return this;
    }

    protected void setValue(Object obj) {
        setText(obj != null ? obj.toString() : "");
    }

    // private void configureValue(Object obj, Class class1) {
    // if (class1 == java.lang.Object.class || class1 == null)
    // setHorizontalAlignment(10);
    // else if (class1 == java.lang.Float.class || class1 ==
    // java.lang.Double.class) {
    // if (numberFormat == null) numberFormat = NumberFormat.getInstance();
    // setHorizontalAlignment(11);
    // setText(obj != null ? numberFormat.format(obj) : "");
    // } else if (class1 == java.lang.Number.class)
    // setHorizontalAlignment(11);
    // else if (class1 == java.util.Date.class) {
    // if (dateFormat == null) dateFormat = DateFormat.getDateInstance();
    // setHorizontalAlignment(10);
    // setText(obj != null ? dateFormat.format(obj) : "");
    // } else if (class1 == javax.swing.Icon.class || class1 ==
    // javax.swing.ImageIcon.class) {
    // setHorizontalAlignment(0);
    // setIcon((obj instanceof Icon) ? (Icon) obj : null);
    // setText("");
    // } else {
    // configureValue(obj, class1.getSuperclass());
    // }
    // }

    // public boolean isOpaque() {
    // Color color = getBackground();
    // java.awt.Container container = getParent();
    // if (container != null) container = container.getParent();
    // boolean flag = color != null && container != null &&
    // color.equals(container.getBackground()) && container.isOpaque();
    // return !flag && super.isOpaque();
    // }

    // public void invalidate() {
    // }
    //
    // public void validate() {
    // }
    //
    // public void revalidate() {
    // }
    //
    // public void repaint(long l, int i, int j, int k, int i1) {
    // }
    //
    // public void repaint(Rectangle rectangle) {
    // }
    //
    // public void repaint() {
    // }

    // protected void firePropertyChange(String s, Object obj, Object obj1) {
    // if (s == "text" || s == "labelFor" || s == "displayedMnemonic" || (s ==
    // "font" || s == "foreground") && obj != obj1 && getClientProperty("html")
    // != null) super.firePropertyChange(s, obj, obj1);
    // }
    //
    // public void firePropertyChange(String s, boolean flag, boolean flag1) {
    // }

    private static final long serialVersionUID = 9059722839161202006L;
    // private static final boolean JAVA5 =
    // System.getProperty("java.version").startsWith("1.5.");
    // private Border noFocusBorder;
    // private Border focusBorder;
    // private NumberFormat numberFormat;
    // private DateFormat dateFormat;
    // private Color alternateColor;
    // private boolean colorCachingEnabled;
    // private Color unselectedForeground;
    // private Color unselectedBackground;
}
