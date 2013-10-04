package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.AccountInterface;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.AccountWrapper;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.GroupWrapper;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.swing.exttable.renderercomponents.RendererTextField;
import org.appwork.swing.exttable.tree.ExtTreeTableModel;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.renderer.RendererCheckBox;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.hosterrule.FreeAccountReference;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

import sun.swing.SwingUtilities2;

public class PackageColumn extends ExtTextColumn<AccountInterface> {

    /**
     * 
     */
    private static final long serialVersionUID       = -2963955407564917958L;
    protected Border          leftGapBorder;

    protected Border          normalBorder;
    private boolean           selectAll              = false;
    private RendererCheckBox  rendererBox;
    private JCheckBox         editorBox;
    private RendererTextField counterField;
    private JTextField        editorCounterField;
    private JLabel            editorLabel;
    private AccountInterface  editing;
    private boolean           programaticallyStarted = false;

    @Override
    public void focusGained(final FocusEvent e) {

    }

    public PackageColumn() {
        super(_GUI._.premiumaccounttablemodel_column_hoster());
        leftGapBorder = BorderFactory.createEmptyBorder(0, 32, 0, 0);
        normalBorder = BorderFactory.createEmptyBorder(0, 6, 0, 0);

        setClickcount(1);
        // Avoids focus flickering when clicking the boxes

    }

    // public boolean isCellEditable(final EventObject evt) {
    //
    // if (evt instanceof MouseEvent) {
    // AccountInterface object = getModel().getObjectbyRow(getModel().getTable().getRowIndexByPoint(((MouseEvent) evt).getPoint()));
    // if (object instanceof AccountWrapper) {
    // return ((MouseEvent) evt).getClickCount() >= 1;
    // } else {
    // return false;
    // }
    // }
    // return true;
    // }

    protected void layoutEditor(final MigPanel editor, final RenderLabel editorIconLabel, final JTextField editorField) {
        this.editorBox = new JCheckBox();

        editorBox.setFocusable(false);
        this.editorCounterField = new JTextField();
        editorCounterField.setEditable(false);
        // editorField.setHighlighter(null);
        // editorField.setEditable(false);
        editorCounterField.setBorder(null);
        editorCounterField.setHighlighter(null);
        editorCounterField.setOpaque(false);
        editorCounterField.setBackground(null);
        editorLabel = new JLabel();
        editor.setLayout(new MigLayout("ins 0", "[][][]5[grow,fill]", "[grow,fill]"));
        editor.add(editorCounterField, "hidemode 2");
        editor.add(editorBox, "hidemode 2");
        editor.add(editorIconLabel, "hidemode 2");
        editor.add(editorField, "hidemode 3,gapleft 1");
        editor.add(editorLabel, "hidemode 3,gapleft 0");
    }

    /**
     * @param rendererField
     * @param rendererIco
     * @param renderer2
     */
    protected void layoutRenderer(final MigPanel renderer, final RenderLabel rendererIcon, final RenderLabel rendererField) {
        this.rendererBox = new RendererCheckBox();
        this.counterField = new RendererTextField();
        counterField.setFocusable(false);
        counterField.setBorder(null);
        counterField.setOpaque(false);
        counterField.setHighlighter(null);
        rendererBox.setFocusable(false);
        renderer.setLayout(new MigLayout("ins 0", "[][][]0[grow,fill]", "[grow,fill]"));
        renderer.add(counterField, "hidemode 2");
        renderer.add(rendererBox, "hidemode 2");
        renderer.add(rendererIcon, "hidemode 2");
        renderer.add(rendererField);

    }

    @Override
    public boolean onDoubleClick(MouseEvent e, AccountInterface contextObject) {

        if (e.getPoint().x - getBounds().x < 30) { return false; }

        return true;
    }

    @Override
    public boolean isEnabled(AccountInterface obj) {
        if (obj instanceof GroupWrapper) return true;
        return obj.isEnabled();
    }

    @Override
    public boolean isSortable(AccountInterface obj) {
        return false;
    }

    @Override
    public int getDefaultWidth() {
        return 200;
    }

    public boolean onRenameClick(final MouseEvent e, final AccountInterface obj) {
        if (e.getPoint().x - getBounds().x < 40) { return false; }
        try {
            programaticallyStarted = true;
            startEditing(obj);

        } finally {
            programaticallyStarted = false;
        }

        return true;

    }

    protected boolean isEditable(final AccountInterface obj, final boolean enabled) {
        if (programaticallyStarted) return true;
        return obj instanceof AccountWrapper;

    }

    @Override
    protected void setStringValue(final String value, final AccountInterface object) {

    }

    @Override
    protected Icon getIcon(AccountInterface value) {
        if (value instanceof GroupWrapper) {
            return NewTheme.I().getIcon("package_open", 18);
        } else {
            return DomainInfo.getInstance(((AccountWrapper) value).getHost()).getFavIcon();
        }

    }

    public void configureRendererComponent(AccountInterface value, boolean isSelected, boolean hasFocus, int row, int column) {
        this.rendererIcon.setIcon(this.getIcon(value));
        String str = this.getStringValue(value);
        if (str == null) {
            // under substance, setting setText(null) somehow sets the label
            // opaque.
            str = "";
        }

        if (getTableColumn() != null) {
            this.rendererField.setText(SwingUtilities2.clipStringIfNecessary(rendererField, rendererField.getFontMetrics(rendererField.getFont()), str, getTableColumn().getWidth() - rendererIcon.getPreferredSize().width - 32));
        } else {
            this.rendererField.setText(str);
        }

        rendererBox.setSelected(value.isEnabled());
        if (value instanceof AccountWrapper) {
            renderer.setBorder(leftGapBorder);
            rendererBox.setVisible(true);
            counterField.setVisible(false);
        } else {
            rendererBox.setVisible(false);
            counterField.setVisible(true);
            // slowwww
            counterField.setText((((ExtTreeTableModel) getModel()).getTreePositionByObject(value).getIndex() + 1) + ".");
            renderer.setBorder(normalBorder);

        }

    }

    @Override
    public final Object getCellEditorValue() {
        if (editing instanceof AccountWrapper) {
            return this.editorBox.isSelected();
        } else {
            return this.editorField.getText();
        }

    }

    public void actionPerformed(final ActionEvent e) {
        this.editorBox.removeActionListener(this);
        this.stopCellEditing();

    }

    @Override
    public void setValue(final Object value, final AccountInterface object) {
        if (object instanceof AccountWrapper) {

            ((AccountWrapper) object).setEnabled((Boolean) value);
        } else {
            if (value != null && value.equals(_GUI._.FileColumn_getStringValue_accountgroup_())) {
                ((GroupWrapper) object).setName(null);
            } else {
                ((GroupWrapper) object).setName((String) value);
            }
        }

    }

    @Override
    public void configureEditorComponent(AccountInterface value, boolean isSelected, int row, int column) {
        editing = value;
        super.configureEditorComponent(value, isSelected, row, column);
        this.editorBox.removeActionListener(this);
        this.editorBox.setSelected(value.isEnabled());
        this.editorBox.addActionListener(this);

        editorField.setEnabled(isEnabled(value));

        editorIconLabel.setEnabled(isEnabled(value));
        editorLabel.setEnabled(isEnabled(value));
        if (value instanceof AccountWrapper) {
            selectAll = true;
            editorLabel.setVisible(true);
            editorField.setVisible(false);
            editorLabel.setText(editorField.getText());
            editor.setBorder(leftGapBorder);
            editorBox.setVisible(true);
            editorCounterField.setVisible(false);
        } else {
            editorBox.setVisible(false);
            editorLabel.setVisible(false);
            editorField.setVisible(true);
            editor.setBorder(normalBorder);
            editorCounterField.setVisible(true);
            editorCounterField.setText((((ExtTreeTableModel) getModel()).getTreePositionByObject(value).getIndex() + 1) + ".");
        }

    }

    @Override
    public boolean isHidable() {

        return false;
    }

    @Override
    public final String getStringValue(AccountInterface value) {

        if (value instanceof GroupWrapper) {
            String name = ((GroupWrapper) value).getName();
            return StringUtils.isEmpty(name) ? _GUI._.FileColumn_getStringValue_accountgroup_() : name;

        } else if (value instanceof AccountWrapper) {
            if (((AccountWrapper) value).getAccount() instanceof FreeAccountReference) { return _GUI._.PackageColumn_getStringValue_freedownload_(); }
            return value.getHost();

        }
        return null;

    }

}
