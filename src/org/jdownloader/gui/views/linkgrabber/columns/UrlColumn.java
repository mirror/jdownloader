package org.jdownloader.gui.views.linkgrabber.columns;

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTextField;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class UrlColumn extends ExtTextColumn<AbstractNode> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private ExtButton         bt;

    public UrlColumn() {
        super(_GUI._.LinkGrabberTableModel_initColumns_url());
        this.setClickcount(2);
        editorField.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    editorField.selectAll();
                    CrossSystem.openURLOrShowMessage(editorField.getText());
                }
            }
        });
        editorField.setBorder(new JTextField().getBorder());
        bt = new ExtButton(new BasicAction() {
            {
                setSmallIcon(NewTheme.I().getIcon("browse", 16));
                setTooltipText(_GUI._.UrlColumn_UrlColumn_open_tt_());
            }

            public void actionPerformed(ActionEvent e) {
                CrossSystem.openURLOrShowMessage(editorField.getText());
            }
        });

        // bt.setRolloverEffectEnabled(true);
        editor.setLayout(new MigLayout("ins 1 4 1 0", "[grow,fill][]", "[fill,grow]"));
        editor.removeAll();
        editor.add(this.editorField, "height 20!");
        editor.add(bt, "height 20!,width 20!");

    }

    @Override
    public void focusGained(final FocusEvent e) {

    }

    @Override
    public boolean isEditable(AbstractNode obj) {

        return obj instanceof CrawledLink;

    }

    @Override
    protected void onSingleClick(MouseEvent e, AbstractNode obj) {
        super.onSingleClick(e, obj);
    }

    @Override
    protected void onDoubleClick(MouseEvent e, AbstractNode obj) {

    }

    @Override
    public boolean isSortable(AbstractNode obj) {
        return true;
    }

    @Override
    protected void setStringValue(String value, AbstractNode object) {
        // set Folder
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof CrawledPackage) {
            return null;
        } else {
            return ((CrawledLink) value).getDownloadLink().getBrowserUrl();
        }
    }

}
