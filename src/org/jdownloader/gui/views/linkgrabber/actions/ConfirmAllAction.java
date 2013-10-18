package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.Image;
import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import jd.controlling.TaskQueue;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmSelectionContextAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmSelectionContextAction.AutoStartOptions;
import org.jdownloader.images.NewTheme;

public class ConfirmAllAction extends AppAction implements CachableInterface {

    /**
     * 
     */
    private static final long  serialVersionUID = 4794612717641894527L;

    private boolean            autoStart;
    public static final String AUTO_START       = "autoStart";

    @Customizer(name = "Start Downloads automatically")
    public boolean isAutoStart() {

        return autoStart;
    }

    public ConfirmAllAction(boolean autostart) {
        setAutoStart(autostart);

    }

    public void setAutoStart(boolean b) {
        if (b) {
            setName(_GUI._.ConfirmAction_ConfirmAction_context_add_and_start());
            Image add = NewTheme.I().getImage("media-playback-start", 20);
            Image play = NewTheme.I().getImage("add", 12);
            setSmallIcon(new ImageIcon(ImageProvider.merge(add, play, 0, 0, 9, 10)));
            this.autoStart = b;

        } else {
            setName(_GUI._.ConfirmAction_ConfirmAction_context_add());
            setSmallIcon(NewTheme.I().getIcon("go-next", 20));
            this.autoStart = b;

        }
    }

    public Object getValue(String key) {
        return super.getValue(key);
    }

    public ConfirmAllAction() {
        this(false);
    }

    public void actionPerformed(final ActionEvent e) {
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>(QueuePriority.HIGH) {

            @Override
            protected Void run() throws RuntimeException {
                final SelectionInfo<CrawledPackage, CrawledLink> si = new SelectionInfo<CrawledPackage, CrawledLink>(null, LinkGrabberTableModel.getInstance().getAllPackageNodes(), null, null, e, LinkGrabberTableModel.getInstance().getTable());
                ConfirmSelectionContextAction ca = new ConfirmSelectionContextAction(si);
                ca.setAutoStart(autoStart ? AutoStartOptions.ENABLED : AutoStartOptions.DISABLED);
                ca.actionPerformed(null);
                return null;
            }
        });
    }

    @Override
    public void setData(String data) {
    }

}
