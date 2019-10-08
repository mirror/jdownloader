package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.controlling.TaskQueue;
import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.DownloadLink;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableSelectionAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.translate._JDT;

public class GenericChunksAction extends CustomizableSelectionAppAction implements ActionContext {
    /**
     *
     */
    private static final long  serialVersionUID = 1L;
    public final static String CHUNKS           = "CHUNKS";
    private int                chunks           = 0;

    public GenericChunksAction() {
        super();
        setName(Integer.toString(Math.max(0, chunks)));
    }

    public void actionPerformed(ActionEvent e) {
        final SelectionInfo<?, ?> selectionInfo = getSelection();
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                if (selectionInfo != null) {
                    for (final Object child : selectionInfo.getChildren()) {
                        if (child instanceof DownloadLink) {
                            ((DownloadLink) child).setChunks(chunks);
                        } else if (child instanceof CrawledLink) {
                            final DownloadLink downloadLink = ((CrawledLink) child).getDownloadLink();
                            if (downloadLink != null) {
                                downloadLink.setChunks(chunks);
                            }
                        }
                    }
                }
                return null;
            }
        });
    }

    public void setChunks(final int chunks) {
        this.chunks = Math.max(0, chunks);
    }

    public static String getTranslationChunks() {
        return _JDT.T.GenericChunksAction_getTranslationChunks();
    }

    @Customizer(link = "#getTranslationChunks")
    public int getChunks() {
        return chunks;
    }

    @Override
    public void loadContextSetups() {
        super.loadContextSetups();
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                setName(Integer.toString(Math.max(0, chunks)));
            }
        }.start();
    }
}
