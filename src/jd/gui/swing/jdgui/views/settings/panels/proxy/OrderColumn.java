package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.JComponent;

import jd.controlling.proxy.AbstractProxySelectorImpl;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.columns.ExtComponentColumn;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class OrderColumn extends ExtComponentColumn<AbstractProxySelectorImpl> {

    /**
	 * 
	 */
    private static final long         serialVersionUID = 5932073061658364800L;
    private MigPanel                  renderer;
    private MigPanel                  editor;

    private RenderLabel               rendererUp;
    private RenderLabel               rendererDown;
    private ExtButton                 editorUp;
    private ExtButton                 editorDown;
    private AbstractProxySelectorImpl editing;
    private int                       editingRow;

    public OrderColumn() {
        super(_GUI._.settings_linkgrabber_filter_columns_exepriority());

        this.editor = new MigPanel("ins 0", "[]0[]", "[grow,fill]");
        this.renderer = new MigPanel("ins 0", "[]0[]", "0[grow,fill]0");
        renderer.setOpaque(false);

        rendererUp = new RenderLabel();
        rendererUp.setIcon(NewTheme.I().getIcon("go-up", 18));

        rendererDown = new RenderLabel();
        rendererDown.setIcon(NewTheme.I().getIcon("go-down", 18));

        editorUp = new ExtButton(new AppAction() {
            /**
			 * 
			 */
            private static final long serialVersionUID = -6373308629670760194L;

            {
                setSmallIcon(NewTheme.I().getIcon("go-up", 18));
            }

            public void actionPerformed(ActionEvent e) {
                java.util.List<AbstractProxySelectorImpl> pkgl = new ArrayList<AbstractProxySelectorImpl>();
                pkgl.add(editing);

                getModel().move(pkgl, editingRow - 1);
                getModel().fireTableDataChanged();
                getModel().getTable().editCellAt(getModel().getRowforObject(editing), getIndex());

            }
        });

        editorDown = new ExtButton(new AppAction() {
            /**
			 * 
			 */
            private static final long serialVersionUID = 359422892536364704L;

            {
                setSmallIcon(NewTheme.I().getIcon("go-down", 18));
            }

            public void actionPerformed(ActionEvent e) {
                java.util.List<AbstractProxySelectorImpl> pkgl = new ArrayList<AbstractProxySelectorImpl>();
                pkgl.add(editing);

                getModel().move(pkgl, editingRow + 2);
                getModel().fireTableDataChanged();

                getModel().getTable().editCellAt(getModel().getRowforObject(editing), getIndex());
            }
        });
        editorUp.setRolloverEffectEnabled(true);
        editorDown.setRolloverEffectEnabled(true);

        // bg = rendererLabel.getBackground();
        // fg = rendererLabel.getForeground();

        editor.setOpaque(false);

        editor.add(editorUp, "height 20!,width 20!");
        editor.add(editorDown, "height 20!,width 20!");

        renderer.add(rendererUp, "height 20!,width 20!");
        renderer.add(rendererDown, "height 20!,width 20!");

    }

    @Override
    protected JComponent getInternalEditorComponent(AbstractProxySelectorImpl value, boolean isSelected, int row, int column) {
        return editor;
    }

    @Override
    protected JComponent getInternalRendererComponent(AbstractProxySelectorImpl value, boolean isSelected, boolean hasFocus, int row, int column) {
        return renderer;
    }

    @Override
    public void configureEditorComponent(AbstractProxySelectorImpl value, boolean isSelected, int row, int column) {
        editing = value;
        editingRow = row;

        editorDown.setEnabled(row != getModel().getRowCount() - 1);
        editorUp.setEnabled(row != 0);
    }

    @Override
    public void configureRendererComponent(AbstractProxySelectorImpl value, boolean isSelected, boolean hasFocus, int row, int column) {

        rendererDown.setEnabled(row != getModel().getRowCount() - 1);
        rendererUp.setEnabled(row != 0);
    }

    @Override
    public void resetEditor() {
        // editor.setForeground(fg);
        // editor.setBackground(bg);
        editor.setOpaque(false);
    }

    @Override
    public void resetRenderer() {
        // renderer.setForeground(fg);
        // renderer.setBackground(bg);
        renderer.setOpaque(false);
    }

    @Override
    public boolean isEnabled(AbstractProxySelectorImpl obj) {
        return true;
    }

    @Override
    public boolean isSortable(final AbstractProxySelectorImpl obj) {
        return false;
    }

    @Override
    protected String getTooltipText(AbstractProxySelectorImpl obj) {
        return null;
    }

    @Override
    public int getDefaultWidth() {
        return 42;
    }

    @Override
    protected boolean isDefaultResizable() {

        return false;
    }

}
