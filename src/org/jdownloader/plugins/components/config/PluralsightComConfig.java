package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;
import org.jdownloader.translate._JDT;

/**
 *
 * @author Neokyuubi
 *
 */

@PluginHost(host = "pluralsight.com", type = Type.CRAWLER)
public interface PluralsightComConfig extends PluginConfigInterface
{
	public static class TRANSLATION
	{
		//public String isDownloadSubtitles_label() {
		//	return "Download Subtitles : ";
		//}
		public String isDownloadSubtitles_label() {
			return _JDT.T.lit_add_subtitles();
		}
		public String getDownloadSubtitles_label() {
			return _JDT.T.lit_add_subtitles();
		}
		
	}
	public static  TRANSLATION TRANSLATION = new TRANSLATION();
	
	@AboutConfig
	@DefaultBooleanValue(true)
	boolean isDownloadSubtitles();
	
	void setDownloadSubtitles(boolean b);
}
