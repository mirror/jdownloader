package jd.plugins.optional.jdunrar;
/**
 * Über das UnrarListenerinterfacegibt der unrarwarpper seinen status z.B. an JDUNrar ab.
 * @author coalado
 *
 */
public interface UnrarListener {

    abstract public void onUnrarEvent(int id,UnrarWrapper wrapper);
}
