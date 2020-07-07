//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class JoinPeerTubeOrg extends antiDDoSForHost {
    public JoinPeerTubeOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // other:
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "https://instances.joinpeertube.org/instances";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return Browser.getHost(link.getPluginPatternMatcher(), true) + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    protected String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    // public static String[] getAnnotationNames() {
    // return Plugin.buildAnnotationNames(getPluginDomains());
    // }
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "peertube.fr", "justtelly.com", "peertube.maxweiss.io", "tube.iddqd.press", "tube.privacytools.io", "videos.lukesmith.xyz", "tube.22decembre.eu", "tilvids.com", "peertube.azkware.net", "tubee.fr", "tuvideo.encanarias.info", "tube.kenfm.de", "p2ptv.ru", "hostyour.tv", "justtelly.com", "controverse.tube", "video.datsemultimedia.com", "peertube.devloprog.org", "peertube.crypto-libertarian.com", "peertube.designersethiques.org", "hope.tube", "testtube.florimond.eu", "v.sevvie.ltd", "open.tube", "tube.1001solutions.net", "peertube.wivodaim.net", "video.gafamfree.party", "watch.haddock.cc", "tube.gnous.eu", "videos.casually.cat", "stream.okin.cloud", "video.taboulisme.com", "peertube.acab.io", "tube-lille.beta.education.fr", "peertube.monlycee.net", "tube.plomlompom.com", "peertube1.zeteo.me", "v.mkp.ca", "tube-outremer.beta.education.fr",
                "tube-lyon.beta.education.fr", "tube-dijon.beta.education.fr", "peertube.public.cat", "videos.fromouter.space", "tube-reims.beta.education.fr", "peertube.de1.sknode.com", "video.marcorennmaus.tk", "tube-limoges.beta.education.fr", "tube-versailles.beta.education.fr", "pt.steffo.eu", "stoptrackingus.tv", "peertube.r2.enst.fr", "peertube.metalbanana.net", "peertube.b38.rural-it.org", "tube-orleans-tours.beta.education.fr", "spacepub.space", "tube-rennes.beta.education.fr", "peertube.devol.it", "tube-clermont-ferrand.beta.education.fr", "video.nimag.net", "peertube.nogafa.org", "tube-corse.beta.education.fr", "film.node9.org", "tube-nice.beta.education.fr", "peertube.tangentfox.com", "notretube.asselma.eu", "tube-montpellier.beta.education.fr", "haytv.blueline.mg", "vidcommons.org", "tube.afix.space", "videos.ac-nancy-metz.fr", "tube-nancy.beta.education.fr",
                "videos.aadtp.be", "videosdulib.re", "tube-aix-marseille.beta.education.fr", "tube-nantes.beta.education.fr", "tube-grenoble.beta.education.fr", "tube-paris.beta.education.fr", "cinematheque.tube", "tube-amiens.beta.education.fr", "peertube.gardeludwig.fr", "peertube.foxfam.club", "docker.videos.lecygnenoir.info", "media.privacyinternational.org", "tube-normandie.beta.education.fr", "tube.port0.xyz", "tube-creteil.beta.education.fr", "tube1.it.tuwien.ac.at", "peertube.travnewmatic.com", "tube.aquilenet.fr", "video.hylianux.com", "peertube.chiccou.net", "peertube.lyceeconnecte.fr", "tube-education.beta.education.fr", "tube.darfweb.eu", "video.phyrone.de", "vids.roshless.me", "peertube.s2s.video", "peertube.agneraya.com", "tube-toulouse.beta.education.fr", "peertube.netzbegruenung.de", "flim.ml", "plextube.nl", "queermotion.org", "peertube.atilla.org",
                "tube.opportunis.me", "nanawel-peertube.dyndns.org", "tube-strasbourg.beta.education.fr", "tube.graz.social", "tube-besancon.beta.education.fr", "vid.garwood.io", "kolektiva.media", "peertube.lefaut.fr", "peertube.ichigo.everydayimshuflin.com", "petitlutinartube.fr", "videos.martyn.berlin", "video.lundi.am", "tube.pawelko.net", "video.unkipamunich.fr", "tube.chatelet.ovh", "peertube.xwiki.com", "tube.florimond.eu", "peertube.it", "peertube.taxinachtegel.de", "peertube.marud.fr", "peertube.mastodon.host", "vid.wizards.zone", "video.mindsforge.com", "peertube.scic-tetris.org", "peertube.semipvt.com", "peertube.lagvoid.com", "peertube.ireis.site", "peertube.hardwarehookups.com.au", "pt.diaspodon.fr", "video.mugoreve.fr", "nocensoring.net", "tube.portes-imaginaire.org", "peertube.robonomics.network", "video.data-expertise.com", "peertubenorge.com", "meta-tube.de",
                "video.minzord.eu.org", "peertube.mazzonetto.eu", "videos.ahp-numerique.fr", "algorithmic.tv", "peertube.gnumeria.fr", "troo.tube", "lanceur-alerte.tv", "gwadloup.tv", "matinik.tv", "peertube.stemy.me", "ptube.horsentiers.fr", "videos.lavoixdessansvoix.org", "peervideo.ru", "peertube.at", "p.eertu.be", "video.marcorennmaus.de", "doby.io", "videos.gerdemann.me", "video.hardlimit.com", "peertube.debian.social", "infotik.fr", "tube.piweb.be", "video.lewd.host", "peertube.su", "freespeech.tube", "video.connor.money", "video.linc.systems", "manicphase.me", "tube.nx12.net", "video.hackers.town", "video.iphodase.fr", "tube.nox-rhea.org", "peertube.fedilab.app", "peertube.volaras.net", "peertube.terranout.mine.nu", "tube.fdn.fr", "tv.datamol.org", "peertube.demonix.fr", "videos.hauspie.fr", "peertube.social.my-wan.de", "media.zat.im", "peertube.club", "peertube.bierzilla.fr",
                "tube.maliweb.at", "lexx.impa.me", "peertube.ventresmous.fr", "tube.linc.systems", "mplayer.demouliere.eu", "video.liberta.vip", "banneddata.me", "peertube.anduin.net", "peertube.gcfamily.fr", "video.ploud.fr", "tube.plaf.fr", "peertube.tech", "video.lono.space", "tube.bn4t.me", "highvoltage.tv", "tube.valinor.fr", "tube.interhacker.space", "peertube.simounet.net", "tube.nah.re", "dreiecksnebel.alex-detsch.de", "stage.peertube.ch", "peertube.ti-fr.com", "video.turbo.chat", "tube.hoga.fr", "bittube.video", "videos.globenet.org", "merci-la-police.fr", "tv.lapesto.fr", "tube.nuagelibre.fr", "videos.festivalparminous.org", "juggling.digital", "peertube.underworld.fr", "peertube.anzui.de", "video.ihatebeinga.live", "pt.neko.bar", "video.greenmycity.eu", "tube.troopers.agency", "tube.thechangebook.org", "tube.rita.moe", "wetube.ojamajo.moe", "lepetitmayennais.fr.nf",
                "tube.blob.cat", "medias.pingbase.net", "video.oh14.de", "mytube.madzel.de", "monplaisirtube.ddns.net", "mytape.org", "peertube.iselfhost.com", "video.okaris.de", "peertube.alpharius.io", "p0.pm", "peertube.video", "video.isurf.ca", "replay.jres.org", "video.blender.org", "peertube.020.pl", "peertube.xoddark.com", "peertube.mxinfo.fr", "csictv.csic.es", "peertube.alcalyn.app", "video.imagotv.fr", "ptube.rousset.nom.fr", "tube.lesamarien.fr", "peertube.cipherbliss.com", "tube.azbyka.ru", "greatview.video", "runtube.re", "tube.ac-amiens.fr", "peertube.euskarabildua.eus", "peertube.schaeferit.de", "media.krashboyz.org", "toobnix.org", "video.emergeheart.info", "videos.numerique-en-commun.fr", "vault.mle.party", "peertube.education-forum.com", "tube.kdy.ch", "peertube.linuxrocks.online", "evertron.tv", "yt.is.nota.live", "videos.upr.fr", "widemus.de",
                "video.glassbeadcollective.org", "peertube.kangoulya.org", "flix.librenet.co.za", "video.nesven.eu", "vidz.dou.bet", "tube.rebellion.global", "videos.koumoul.com", "tube.undernet.uy", "peertube.cojo.uno", "peertube.opencloud.lu", "video.hdys.band", "cattube.org", "peertube.ch", "tube.tappret.fr", "peertube.hatthieves.es", "peertube.la-famille-muller.fr", "video.sftblw.moe", "watch.snoot.tube", "video.gcfam.net", "peertube.pontostroy.gq", "video.splat.soy", "peerwatch.xyz", "peertube.snargol.com", "peertube.desmu.fr", "ppstube.portageps.org", "peertube.live", "peertube.pl", "xxxporn.co.uk", "peertube.dk", "vid.lubar.me", "tube.benzo.online", "tube.kapussinettes.ovh", "peertube.rainbowswingers.net", "videomensoif.ynh.fr", "peertube.montecsys.fr", "peer.tube", "tube.nx-pod.de", "video.monsieurbidouille.fr", "tube.openalgeria.org", "vid.lelux.fi", "video.anormallostpod.ovh",
                "tube.crapaud-fou.org", "lostpod.space", "exode.me", "vis.ion.ovh", "videos-libr.es", "video.qoto.org", "video.vny.fr", "peervideo.club", "tube.taker.fr", "peertube.chantierlibre.org", "tube.kicou.info", "video.yukari.moe", "peertube.co.uk", "vod.mochi.academy", "video.fitchfamily.org", "video.fdlibre.eu", "tube.fabrigli.fr", "video.bruitbruit.com", "peer.philoxweb.be", "peertube.bilange.ca", "libretube.net", "libre.video", "us.tv", "peertube.sl-network.fr", "peertube.dynlinux.io", "peertube.david.durieux.family", "v.kretschmann.social", "tube.otter.sh", "videos.funkwhale.audio", "watch.44con.com", "pony.tube", "tube.danq.me", "tube.fab-l3.org", "tube.calculate.social", "tube.netzspielplatz.de", "peertube.laas.fr", "tube.govital.net", "video.ploud.jp", "video.omniatv.com", "peertube.ffs2play.fr", "video.1000i100.fr", "tube.worldofhauru.xyz", "conf.tube",
                "peertube.jackbot.fr", "tube.extinctionrebellion.fr", "peertube.f-si.org", "video.subak.ovh", "videos.koweb.fr", "peertube.roflcopter.fr", "peertube.floss-marketing-school.com", "peertube.iriseden.eu", "videos.ubuntu-paris.org", "armstube.com", "peertube.lol", "peertube.normandie-libre.fr", "peertube.slat.org", "peertube.uno", "peertube.servebeer.com", "peertube.fedi.quebec", "tube.h3z.jp", "tube.plus200.com", "gouttedeau.space", "video.antirep.net", "tube.ksl-bmx.de", "tube.tchncs.de", "video.devinberg.com", "hitchtube.fr", "peertube.kosebamse.com", "yunopeertube.myddns.me", "peertube.anon-kenkai.com", "tube.maiti.info", "videos.dinofly.com", "videotape.me", "video.lemediatv.fr", "thickrips.cloud", "pt.laurentkruger.fr", "video.monarch-pass.net", "peertube.artica.center", "indymotion.fr", "fanvid.stopthatimp.net", "video.farci.org", "v.lesterpig.com", "tube.fede.re",
                "pytu.be", "devtube.dev-wiki.de", "raptube.antipub.org", "video.selea.se", "peertube.mygaia.org", "peertube.livingutopia.org", "peertube.the-penguin.de", "tube.anjara.eu", "pt.pube.tk", "peertube.me", "video.latavernedejohnjohn.fr", "peertube.pcservice46.fr", "video.irem.univ-paris-diderot.fr", "alttube.fr", "video.coop.tools", "video.cabane-libre.org", "peertube.openstreetmap.fr", "videos.alolise.org", "irrsinn.video", "video.antopie.org", "scitech.video", "video.amic37.fr", "peertube.freeforge.eu", "peertube.togart.de", "tube.postblue.info", "videos.domainepublic.net", "peertube.cyber-tribal.com", "video.gresille.org", "cinema.yunohost.support", "repro.video", "videos.wakapo.com", "pt.kircheneuenburg.de", "peertube.asrun.eu", "videos.side-ways.net", "91video.online", "videos-libr.es", "tv.mooh.fr", "nuage.acostey.fr", "videos.pair2jeux.tube", "videos.pueseso.club",
                "media.assassinate-you.net", "videos.squat.net", "peertube.makotoworkshop.org", "peertube.serveur.slv-valbonne.fr", "videos.hack2g2.fr", "pire.artisanlogiciel.net", "video.netsyms.com", "video.die-partei.social", "video.writeas.org", "videos.adhocmusic.com", "tube.rfc1149.net", "peertube.librelabucm.org", "peertube.koehn.com", "peertube.anarchmusicall.net", "vid.y-y.li", "diode.zone", "peertube.nomagic.uk", "video.rastapuls.com", "video.mantlepro.com", "video.deadsuperhero.com", "peertube.musicstudio.pro", "peertube.we-keys.fr", "artitube.artifaille.fr", "tube.midov.pl", "tube.nemsia.org", "tube.bruniau.net", "tube.traydent.info", "peertube.nayya.org", "video.lequerrec.eu", "peertube.amicale.net", "aperi.tube", "tube.ac-lyon.fr", "video.lw1.at", "yiny.org", "videos.pofilo.fr", "tube.lou.lt", "betamax.video", "video.typica.us", "videos.lescommuns.org", "videonaute.fr",
                "dialup.express", "megatube.lilomoino.fr", "peertube.1312.media", "skeptikon.fr", "video.blueline.mg", "tube.homecomputing.fr", "tube.ouahpiti.info", "video.tedomum.net", "video.g3l.org", "fontube.fr", "peertube.gaialabs.ch", "tube.kher.nl", "peertube.qtg.fr", "video.migennes.net", "tube.p2p.legal", "troll.tv", "videos.iut-orsay.fr", "peertube.solidev.net", "videos.cemea.org", "video.passageenseine.fr", "peertube.touhoppai.moe", "peer.hostux.social", "share.tube", "videos.benpro.fr", "peertube.parleur.net", "peertube.heraut.eu", "peertube.gegeweb.eu", "framatube.org", "thinkerview.video", "tube.conferences-gesticulees.net", "peertube.datagueule.tv", "video.lqdn.fr", "tube.mochi.academy", "video.colibris-outilslibres.org", "peertube3.cpy.re", "peertube2.cpy.re", "peertube.cpy.re" });
        return ret;
    }

    /** Returns content of getPluginDomains as single dimensional Array. */
    public static ArrayList<String> getAllSupportedPluginDomainsFlat() {
        ArrayList<String> allDomains = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            for (final String singleDomain : domains) {
                allDomains.add(singleDomain);
            }
        }
        return allDomains;
    }

    public static String[] getAnnotationNames() {
        return new String[] { "joinpeertube.org" };
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/videos/(?:watch|embed)/([a-f0-9\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Debug function which can find new instances compatible with this code/plugin/template from:
     * https://instances.joinpeertube.org/instances
     */
    private static ArrayList<String> findNewScriptInstances() {
        if (false) {
            // don't compile into class file
            final ArrayList<String> existingInstances = getAllSupportedPluginDomainsFlat();
            final ArrayList<String> newInstances = new ArrayList<String>();
            final Browser br = new Browser();
            final int itemsPerRequest = 50;
            int index = 0;
            int totalNumberofItems = 0;
            ArrayList<Object> ressourcelist = null;
            do {
                try {
                    br.getPage("https://instances.joinpeertube.org/api/v1/instances?start=" + index + "&count=" + itemsPerRequest + "&sort=-createdAt");
                    Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    if (index == 0) {
                        totalNumberofItems = ((Number) entries.get("total")).intValue();
                    }
                    ressourcelist = (ArrayList<Object>) entries.get("data");
                    for (final Object siteO : ressourcelist) {
                        entries = (Map<String, Object>) siteO;
                        String host = (String) entries.get("host");
                        // final String siteVersion = (String) entries.get("version");
                        if (StringUtils.isEmpty(host)) {
                            continue;
                        }
                        host = host.replace("www.", "");
                        if (!existingInstances.contains(host)) {
                            newInstances.add(host);
                        }
                    }
                } catch (final Throwable e) {
                    /* Stop on unknown errors */
                    e.printStackTrace();
                    break;
                }
                index += itemsPerRequest;
            } while (ressourcelist != null && ressourcelist.size() == itemsPerRequest);
            String hostsStr = "ret.add(new String[] { ";
            for (final String newHost : newInstances) {
                /* Outputs Java code to easily add new instances */
                // System.out.println(newHost + ",");
                hostsStr += "\"" + newHost + "\", ";
            }
            hostsStr += " });";
            System.out.println(hostsStr);
            /* TODO: Maybe add a check for missing/offline instances too */
            return newInstances;
        } else {
            return null;
        }
    }

    /** Using API: https://docs.joinpeertube.org/api-rest-reference.html */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String host = Browser.getHost(link.getPluginPatternMatcher(), true);
        getPage("https://" + host + "/api/v1/videos/" + this.getFID(link));
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* 2020-07-03: E.g. {"error":"Video not found"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        checkErrorsAPI();
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        String title = (String) entries.get("name");
        final String createdAt = (String) entries.get("createdAt");
        final String description = (String) entries.get("description");
        if (!StringUtils.isEmpty(description) && link.getComment() == null) {
            link.setComment(description);
        }
        final String uploader = (String) JavaScriptEngineFactory.walkJson(entries, "account/name");
        String formattedDate = new Regex(createdAt, "(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        if (formattedDate == null) {
            formattedDate = createdAt;
        }
        /* Grab highest quality downloadurl + filesize */
        this.dllink = (String) JavaScriptEngineFactory.walkJson(entries, "files/{0}/fileDownloadUrl");
        final long filesize = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "files/{0}/size"), 0);
        if (filesize > 0) {
            link.setDownloadSize(filesize);
        }
        if (StringUtils.isEmpty(title)) {
            title = this.getFID(link);
        }
        String filename = "";
        if (!StringUtils.isEmpty(formattedDate)) {
            filename += formattedDate + "_";
        }
        filename += host.replace(".", "_") + "_";
        if (!StringUtils.isEmpty(uploader)) {
            filename += uploader + "_";
        }
        filename += title + ".mp4";
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    protected void checkErrorsAPI() throws PluginException {
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String errorMsg = (String) entries.get("error");
        if (!StringUtils.isEmpty(errorMsg)) {
            if (errorMsg.equalsIgnoreCase("Video not found")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                /* Unknown error */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errorMsg);
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("text")) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            }
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PeerTube;
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.GENERIC };
    }

    @Override
    public String getHost(final DownloadLink link, final Account account) {
        if (link != null) {
            return Browser.getHost(link.getPluginPatternMatcher(), true);
        } else if (account != null) {
            return account.getHoster();
        } else {
            return null;
        }
    }
}
