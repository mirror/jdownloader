package jd.controlling.reconnect.plugins.batch.translate;
import org.appwork.txtresource.*;
@Defaults(lngs = { "en"})
public interface BatchTranslation extends TranslateInterface {

@Default(lngs = { "en" }, values = { "Start in (application folder)" })
String interaction_batchreconnect_executein();
@Default(lngs = { "en" }, values = { "External Batch Reconnect" })
String jd_controlling_reconnect_plugins_batch_ExternBatchReconnectPlugin_getName();
@Default(lngs = { "en" }, values = { "Interpreter" })
String interaction_batchreconnect_terminal();
@Default(lngs = { "en" }, values = { "Batch Script" })
String interaction_batchreconnect_batch();
}