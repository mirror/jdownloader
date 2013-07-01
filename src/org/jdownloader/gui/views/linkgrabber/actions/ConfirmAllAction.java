package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import jd.controlling.IOEQ;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.images.NewTheme;

public class ConfirmAllAction extends AppAction {
    /**
     * 
     */
    private static final long serialVersionUID = 4794612717641894527L;

    private boolean           autostart;

    public ConfirmAllAction(boolean autostart) {
        setAutoStart(autostart);
        setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK));
    }

    public void setAutoStart(boolean b) {
        if (b) {
            setName(_GUI._.ConfirmAction_ConfirmAction_context_add_and_start());
            Image add = NewTheme.I().getImage("media-playback-start", 20);
            Image play = NewTheme.I().getImage("add", 12);
            setSmallIcon(new ImageIcon(ImageProvider.merge(add, play, 0, 0, 9, 10)));
            this.autostart = b;

        } else {
            setName(_GUI._.ConfirmAction_ConfirmAction_context_add());
            setSmallIcon(NewTheme.I().getIcon("go-next", 20));
            this.autostart = b;

        }
    }

    public Object getValue(String key) {
        return super.getValue(key);
    }

    public ConfirmAllAction() {
        this(false);
    }

    public void actionPerformed(final ActionEvent e) {

        IOEQ.add(new Runnable() {

            public void run() {

                java.util.List<AbstractNode> packages = new ArrayList<AbstractNode>(LinkGrabberTableModel.getInstance().getAllPackageNodes());

                ConfirmAutoAction ca = new ConfirmAutoAction(new SelectionInfo<CrawledPackage, CrawledLink>(null, packages, null, null, e, LinkGrabberTableModel.getInstance().getTable()));
                ca.setAutostart(autostart);
                ca.actionPerformed(null);
            }

        }, true);
    }

}
