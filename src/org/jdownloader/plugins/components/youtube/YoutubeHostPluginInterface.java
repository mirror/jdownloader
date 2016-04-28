package org.jdownloader.plugins.components.youtube;

import java.util.ArrayList;
import java.util.List;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.youtube.choosevariantdialog.YoutubeVariantSelectionDialog;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.VariantInfo;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;

public interface YoutubeHostPluginInterface {

    void showChangeOrAddVariantDialog(final CrawledLink link, final AbstractVariant s);

}
