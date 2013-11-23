package org.jdownloader.extensions.streaming.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.plugins.FilePackage;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.T;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.images.NewTheme;

public class AddToLibraryAction extends AppAction {

    private SelectionInfo<CrawledPackage, CrawledLink> selection;
    private StreamingExtension                         extension;

    public AddToLibraryAction(StreamingExtension streamingExtension, SelectionInfo<CrawledPackage, CrawledLink> selectionInfo) {
        this.selection = selectionInfo;
        this.extension = streamingExtension;

        setName(T._.AddToLibraryAction());

        setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("go-next", 20), NewTheme.I().getImage("video", 14), 0, 0, 6, 6)));

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        MainTabbedPane.getInstance().setSelectedComponent(extension.getGUI());
        LinkCollector.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                for (PackageView<CrawledPackage, CrawledLink> cp : selection.getPackageViews()) {
                    for (FilePackage fp : LinkCollector.getInstance().convert(cp.getChildren(), false)) {
                        extension.getMediaArchiveController().mount(fp);
                    }
                }
                return null;
            }
        });

    }

}
