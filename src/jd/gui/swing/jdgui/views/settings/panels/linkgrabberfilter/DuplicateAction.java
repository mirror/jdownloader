package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.translate._JDT;

public class DuplicateAction extends AppAction {

    private LinkgrabberFilterRule contextObject;
    private FilterTable           filterTable;

    public DuplicateAction(LinkgrabberFilterRule contextObject, FilterTable filterTable) {
        setName(_JDT._.DuplicateAction_DuplicateAction_());
        setIconKey("copy");
        this.contextObject = contextObject;
        this.filterTable = filterTable;
    }

    public boolean isEnabled() {
        return contextObject != null;
    }

    public void actionPerformed(ActionEvent e) {
        LinkgrabberFilterRule newRule = contextObject.duplicate();
        NewAction.add(newRule, filterTable);
    }

}
