package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.Component;
import java.awt.Cursor;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import jd.plugins.Account;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.table.columns.ExtComponentColumn;
import org.jdownloader.gui.translate._GUI;

public class ActionColumn extends ExtComponentColumn<Account> {

    public static final int   SIZE = 16;
    private JPanel            renderer;
    private JPanel            editor;
    private RenewAction       renew;
    private InfoAction        info;
    private DeleteAction      delete;
    private PremiumzoneAction premzone;

    public ActionColumn() {
        super(_GUI._.premiumaccounttablemodel_column_actions());
        renderer = new JPanel(new MigLayout("ins 2", "[]", "[]"));
        editor = new JPanel(new MigLayout("ins 2", "[]", "[]"));
        renew = new RenewAction();
        info = new InfoAction();
        delete = new DeleteAction();
        premzone = new PremiumzoneAction();
        // add(premzone);
        add(info);
        add(renew);
        // add(delete);
    }

    private void add(TableBarAction action) {
        renderer.add(getButton(action), "width 18!,height 18!");
        editor.add(getButton(action), "width 18!,height 18!");
    }

    private Component getButton(TableBarAction action) {
        final JButton bt = new JButton(action);
        // final Border border = bt.getBorder();

        // bt.addMouseListener(new MouseAdapter() {
        //
        // @Override
        // public void mouseEntered(MouseEvent e) {
        //
        // bt.setContentAreaFilled(true);
        // bt.setBorderPainted(true);
        // }
        //
        // @Override
        // public void mouseExited(MouseEvent e) {
        // bt.setContentAreaFilled(false);
        // bt.setBorderPainted(false);
        // }
        // });
        bt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bt.setContentAreaFilled(false);
        bt.setBorderPainted(false);
        // bt.setBorder(null);
        bt.setToolTipText(bt.getText());
        bt.setText("");
        return bt;
    }

    @Override
    protected int getMaxWidth() {
        return getMinWidth();
    }

    @Override
    public int getMinWidth() {
        return 68;
    }

    @Override
    protected JComponent getEditorComponent(Account value, boolean isSelected, int row, int column) {
        setAccount(value);

        return editor;
    }

    private void setAccount(Account value) {
        renew.setAccount(value);
        info.setAccount(value);
        delete.setAccount(value);
        premzone.setAccount(value);

    }

    @Override
    protected JComponent getRendererComponent(Account value, boolean isSelected, int row, int column) {
        setAccount(value);
        return renderer;
    }

}
