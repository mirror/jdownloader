package org.jdownloader.api.downloads;

import java.util.List;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;

@ApiNamespace("downloads")
public interface DownloadsAPI extends RemoteAPIInterface {

	public List<FilePackageAPIStorable> list();

	public boolean stop();

	public boolean start();

	public int speed();

	public int limit();

	public long traffic();

}
