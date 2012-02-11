package org.jdownloader.extensions.neembuu;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.plugins.download.DownloadInterface.Chunk;

import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.neembuu.gui.NeembuuGui;
import org.jdownloader.extensions.neembuu.translate._NT;
import org.jdownloader.images.NewTheme;

public class NeembuuExtension extends AbstractExtension<NeembuuConfig> {

	private NeembuuGui tab;
	private int number_of_retries = 0; // 0=not checked, 1=checked once but
										// failed ... -1 = works :)

	private final LinkedList<WatchAsYouDownloadSession> watchAsYouDownloadSessions = new LinkedList<WatchAsYouDownloadSession>();

	public NeembuuExtension() {
		super(_NT._.title());
		new EDTRunner() {

			@Override
			protected void runInEDT() {
				tab = new NeembuuGui(NeembuuExtension.this);
			}
		}.waitForEDT();
	}

	public final boolean isUsable() {
		if (number_of_retries > 10) {
			logger.fine("Virtual File system checked more than 10 times, and it is not working");
			return false;
			// we just simply assume
			// that it is not going to work
		}
		if (number_of_retries != -1) {
			if (CheckJPfm.checkVirtualFileSystemCompatibility(logger)) {
				number_of_retries = -1;
			} else
				number_of_retries++;
		}
		return number_of_retries == -1;
	}

	public static boolean tryHandle(final JDDownloadSession jdds) {
		if (!isActive())
			return false;
		NeembuuExtension ne = getInstance();
		if (!ne.isUsable())
			return false;

		return ne.tryHandle_(jdds);
	}

	private boolean tryHandle_(final JDDownloadSession jdds) {
		synchronized (watchAsYouDownloadSessions) {
			int o = JOptionPane.showConfirmDialog(null,
					"Do you wish to watch as you download this file?",
					"Neembuu Watch as you download", JOptionPane.YES_NO_OPTION);
			if (o != JOptionPane.YES_OPTION) {
				return false;
			}

			try {
				WatchAsYouDownloadSessionImpl.makeNew(jdds);
				watchAsYouDownloadSessions.add(jdds
						.getWatchAsYouDownloadSession());
			} catch (Exception a) {
				SwingUtilities.invokeLater(new Runnable() {
					// @Override
					public void run() {
						JOptionPane.showMessageDialog(null,
								"Could not start a watch as you download session for\n"
										+ jdds.toString(),
								"Neembuu watch as you download failed.",
								JOptionPane.ERROR_MESSAGE);
					}
				});

				logger.log(Level.SEVERE,
						"Could not start a watch as you download session", a);
				return false;
			}

			jdds.getDownloadInterface().addChunksDownloading(1);
			Chunk ch = jdds.getDownloadInterface().new Chunk(0, 0, null, null) {
				@Override
				public long getSpeed() {
					return (long) jdds.getWatchAsYouDownloadSession()
							.getSeekableConnectionFile()
							.getTotalFileReadStatistics()
							.getTotalAverageDownloadSpeedProvider()
							.getDownloadSpeed_KiBps() * 1024;
				}
			};
			ch.setInProgress(true);
			jdds.getDownloadInterface().getChunks().add(ch);
			tab.addSession(jdds);

			return true;
		}
	}

	public static boolean isActive() {
		return ExtensionController.getInstance().isExtensionActive(
				NeembuuExtension.class);
	}

	public static NeembuuExtension getInstance() {
		return (NeembuuExtension) ExtensionController.getInstance()
				.getExtension(NeembuuExtension.class)._getExtension();
	}

	/**
	 * Action "onStop". Is called each time the user disables the extension
	 */
	@Override
	protected void stop() throws StopException {
		synchronized (watchAsYouDownloadSessions) {
			Iterator<WatchAsYouDownloadSession> it = watchAsYouDownloadSessions
					.iterator();
			while (it.hasNext()) {
				try {
					it.next().unMount();
				} catch (Exception a) {
					// ignore
				}
				it.remove();
			}
		}
		Log.L.finer("Stopped " + getClass().getSimpleName());
	}

	/**
	 * Actions "onStart". is called each time the user enables the extension
	 */
	@Override
	protected void start() throws StartException {
		Log.L.finer("Started " + getClass().getSimpleName());
	}

	/**
	 * Has to return the Extension MAIN Icon. This icon will be used,for
	 * example, in the settings pane
	 */
	@Override
	public ImageIcon getIcon(int size) {
		return NewTheme.I().getIcon("ok", size);
	}

	@Override
	public boolean isDefaultEnabled() {
		return true;
	}

	@Override
	public boolean isQuickToggleEnabled() {
		return true;
	}

	/**
	 * gets called once as soon as the extension is loaded.
	 */
	@Override
	protected void initExtension() throws StartException {
	}

	/**
	 * Returns the Settingspanel for this extension. If this extension does not
	 * have a configpanel, null can be returned
	 */
	@Override
	public ExtensionConfigPanel<?> getConfigPanel() {
		return null;
	}

	/**
	 * Should return false of this extension has no configpanel
	 */
	@Override
	public boolean hasConfigPanel() {
		return false;
	}

	/**
	 * DO NOT USE THIS FUNCTION. it is only used for compatibility reasons
	 */
	@Override
	@Deprecated
	public String getConfigID() {
		return null;
	}

	@Override
	public String getAuthor() {
		return "Shashank Tulsyan";
	}

	@Override
	public String getDescription() {
		return _NT._.description();
	}

	/**
	 * Returns the gui
	 */
	@Override
	public NeembuuGui getGUI() {
		return tab;
	}

}
