package org.jdownloader.api.downloads;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class DownloadsAPIImpl implements DownloadsAPI {

	public List<FilePackageAPIStorable> list() {
		DownloadController dlc = DownloadController.getInstance();
		boolean b = dlc.readLock();
		try {
			java.util.List<FilePackageAPIStorable> ret = new ArrayList<FilePackageAPIStorable>(
					dlc.size());
			for (FilePackage fpkg : dlc.getPackages()) {
				FilePackageAPIStorable pkg;
				ret.add(pkg = new FilePackageAPIStorable(fpkg));
				synchronized (fpkg) {
					List<DownloadLinkAPIStorable> links = new ArrayList<DownloadLinkAPIStorable>(
							fpkg.size());
					for (DownloadLink link : fpkg.getChildren()) {
						links.add(new DownloadLinkAPIStorable(link));
					}
					pkg.setLinks(links);
				}
			}
			return ret;
		} finally {
			dlc.readUnlock(b);
		}
	}

	public boolean stop() {
		DownloadWatchDog.getInstance().startDownloads();
		return true;
	}

	public boolean start() {
		DownloadWatchDog.getInstance().stopDownloads();
		return true;
	}

	public int speed() {
		return DownloadWatchDog.getInstance().getDownloadSpeedManager()
				.getSpeed();
	}

	public int limit() {
		return DownloadWatchDog.getInstance().getDownloadSpeedManager()
				.getLimit();
	}

	public long traffic() {
		return DownloadWatchDog.getInstance().getDownloadSpeedManager()
				.getTraffic();
	}

}
