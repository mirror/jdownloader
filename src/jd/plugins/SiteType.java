package jd.plugins;


/**
 * used for defining template types. Use by testclass
 *
 * @author raztoki
 *
 */
public class SiteType {

    public static enum SiteTemplate {

        /**
         * <a href="http://gempixel.com/project/premium-url-shortener/">Premium URL Shortener</a><br />
         * sold on <a href="http://codecanyon.net/item/premium-url-shortener/3688135">codecanyon.net</a><br />
         * examples <a href="http://cehs.ch/">cehs.ch</a> <a href="http://www.csurl.it">csurl.it</a>
         *
         */
        GemPixel_PremiumURLShortener,

        /**
         * Should cover all given templates.<br />
         * <a href="http://sibsoft.net/xfilesharing.html">XFileSharing<a><br />
         * <a href="http://sibsoft.net/xfilesharing_free.html">XFileSharing FREE (old version)</a><br />
         * <a href="http://sibsoft.net/xvideosharing.html">XVideoSharing<a><br />
         * <a href="http://sibsoft.net/ximagesharing.html">XImageSharing<a><br />
         */
        SibSoft_XFileShare,

        /**
         * <a href="http://sibsoft.net/xlinker.html">XLinker</a>
         */
        SibSoft_XLinker,

        /**
         * <a href="https://mfscripts.com/yetishare/overview.html">YetiShare - PHP File Hosting Site Script</a>
         */
        MFScripts_YetiShare,

        /**
         * <a href="https://mfscripts.com/wurlie/overview.html">Wurlie - PHP Short Url Script</a>
         */
        MFScripts_Wurlie,

        /**
         * <a href="http://www.hostedtube.com/">hosted tube</a> porn script/template provided by <a
         * href="http://pimproll.com/">pimproll.com</a>
         */
        PimpRoll_HostedTube,

        /**
         * MultiUpload script by unknown, called Qooy Mirrors (taken from paypal description) <a href="http://qooy.com/sale.php">Qooy
         * Mirrors</a>
         */
        Qooy_Mirrors,

        /**
         * the template that supports mirror stack type sites.. Not sure what to call this script.
         */
        Unknown_MirrorStack,

        /**
         * Turbobit hosted sites. <a href="http://turbobit.net/">turbobit.net</a>
         */
        Turbobit_Turbobit,

        /**
         * linkbucks hosted sites. <a href="http://linkbucks.com/">linkbucks.com</a>
         */
        Linkbucks_Linkbucks,

        /**
         * safelinking hosted sites <a href="http://safelinking.net/">safelinking.net</a>
         */
        SafeLinking_SafeLinking;

    }

}
