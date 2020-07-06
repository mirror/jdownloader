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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PeertubeFr extends antiDDoSForHost {
    public PeertubeFr(PluginWrapper wrapper) {
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
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "peertube.fr" });
        ret.add(new String[] { "peertube.maxweiss.io" });
        ret.add(new String[] { "tube.iddqd.press" });
        ret.add(new String[] { "tube.privacytools.io" });
        ret.add(new String[] { "videos.lukesmith.xyz" });
        ret.add(new String[] { "tube.22decembre.eu" });
        ret.add(new String[] { "tilvids.com" });
        ret.add(new String[] { "peertube.azkware.net" });
        ret.add(new String[] { "tubee.fr" });
        ret.add(new String[] { "tuvideo.encanarias.info" });
        ret.add(new String[] { "tube.kenfm.de" });
        ret.add(new String[] { "p2ptv.ru" });
        ret.add(new String[] { "hostyour.tv" });
        ret.add(new String[] { "justtelly.com" });
        ret.add(new String[] { "controverse.tube" });
        ret.add(new String[] { "video.datsemultimedia.com" });
        ret.add(new String[] { "peertube.devloprog.org" });
        ret.add(new String[] { "peertube.crypto-libertarian.com" });
        ret.add(new String[] { "peertube.designersethiques.org" });
        ret.add(new String[] { "hope.tube" });
        ret.add(new String[] { "testtube.florimond.eu" });
        ret.add(new String[] { "v.sevvie.ltd" });
        ret.add(new String[] { "open.tube" });
        ret.add(new String[] { "tube.1001solutions.net" });
        ret.add(new String[] { "peertube.wivodaim.net" });
        ret.add(new String[] { "video.gafamfree.party" });
        ret.add(new String[] { "watch.haddock.cc" });
        ret.add(new String[] { "tube.gnous.eu" });
        ret.add(new String[] { "videos.casually.cat" });
        ret.add(new String[] { "stream.okin.cloud" });
        ret.add(new String[] { "video.taboulisme.com" });
        ret.add(new String[] { "peertube.acab.io" });
        ret.add(new String[] { "tube-lille.beta.education.fr" });
        ret.add(new String[] { "peertube.monlycee.net" });
        ret.add(new String[] { "tube.plomlompom.com" });
        ret.add(new String[] { "peertube1.zeteo.me" });
        ret.add(new String[] { "v.mkp.ca" });
        ret.add(new String[] { "tube-outremer.beta.education.fr" });
        ret.add(new String[] { "tube-lyon.beta.education.fr" });
        ret.add(new String[] { "tube-dijon.beta.education.fr" });
        ret.add(new String[] { "peertube.public.cat" });
        ret.add(new String[] { "videos.fromouter.space" });
        ret.add(new String[] { "tube-reims.beta.education.fr" });
        ret.add(new String[] { "peertube.de1.sknode.com" });
        ret.add(new String[] { "video.marcorennmaus.tk" });
        ret.add(new String[] { "tube-limoges.beta.education.fr" });
        ret.add(new String[] { "tube-versailles.beta.education.fr" });
        ret.add(new String[] { "pt.steffo.eu" });
        ret.add(new String[] { "stoptrackingus.tv" });
        ret.add(new String[] { "peertube.r2.enst.fr" });
        ret.add(new String[] { "peertube.metalbanana.net" });
        ret.add(new String[] { "peertube.b38.rural-it.org" });
        ret.add(new String[] { "tube-orleans-tours.beta.education.fr" });
        ret.add(new String[] { "spacepub.space" });
        ret.add(new String[] { "tube-rennes.beta.education.fr" });
        ret.add(new String[] { "peertube.devol.it" });
        ret.add(new String[] { "tube-clermont-ferrand.beta.education.fr" });
        ret.add(new String[] { "video.nimag.net" });
        ret.add(new String[] { "peertube.nogafa.org" });
        ret.add(new String[] { "tube-corse.beta.education.fr" });
        ret.add(new String[] { "film.node9.org" });
        ret.add(new String[] { "tube-nice.beta.education.fr" });
        ret.add(new String[] { "peertube.tangentfox.com" });
        ret.add(new String[] { "notretube.asselma.eu" });
        ret.add(new String[] { "tube-montpellier.beta.education.fr" });
        ret.add(new String[] { "haytv.blueline.mg" });
        ret.add(new String[] { "vidcommons.org" });
        ret.add(new String[] { "tube.afix.space" });
        ret.add(new String[] { "videos.ac-nancy-metz.fr" });
        ret.add(new String[] { "tube-nancy.beta.education.fr" });
        ret.add(new String[] { "videos.aadtp.be" });
        ret.add(new String[] { "videosdulib.re" });
        ret.add(new String[] { "tube-aix-marseille.beta.education.fr" });
        ret.add(new String[] { "tube-nantes.beta.education.fr" });
        ret.add(new String[] { "tube-grenoble.beta.education.fr" });
        ret.add(new String[] { "tube-paris.beta.education.fr" });
        ret.add(new String[] { "cinematheque.tube" });
        ret.add(new String[] { "tube-amiens.beta.education.fr" });
        ret.add(new String[] { "peertube.gardeludwig.fr" });
        ret.add(new String[] { "peertube.foxfam.club" });
        ret.add(new String[] { "docker.videos.lecygnenoir.info" });
        ret.add(new String[] { "media.privacyinternational.org" });
        ret.add(new String[] { "tube-normandie.beta.education.fr" });
        ret.add(new String[] { "tube.port0.xyz" });
        ret.add(new String[] { "tube-creteil.beta.education.fr" });
        ret.add(new String[] { "tube1.it.tuwien.ac.at" });
        ret.add(new String[] { "peertube.travnewmatic.com" });
        ret.add(new String[] { "tube.aquilenet.fr" });
        ret.add(new String[] { "video.hylianux.com" });
        ret.add(new String[] { "peertube.chiccou.net" });
        ret.add(new String[] { "peertube.lyceeconnecte.fr" });
        ret.add(new String[] { "tube-education.beta.education.fr" });
        ret.add(new String[] { "tube.darfweb.eu" });
        ret.add(new String[] { "video.phyrone.de" });
        ret.add(new String[] { "vids.roshless.me" });
        ret.add(new String[] { "peertube.s2s.video" });
        ret.add(new String[] { "peertube.agneraya.com" });
        ret.add(new String[] { "tube-toulouse.beta.education.fr" });
        ret.add(new String[] { "peertube.netzbegruenung.de" });
        ret.add(new String[] { "flim.ml" });
        ret.add(new String[] { "plextube.nl" });
        ret.add(new String[] { "queermotion.org" });
        ret.add(new String[] { "peertube.atilla.org" });
        ret.add(new String[] { "tube.opportunis.me" });
        ret.add(new String[] { "nanawel-peertube.dyndns.org" });
        ret.add(new String[] { "tube-strasbourg.beta.education.fr" });
        ret.add(new String[] { "tube.graz.social" });
        ret.add(new String[] { "tube-besancon.beta.education.fr" });
        ret.add(new String[] { "vid.garwood.io" });
        ret.add(new String[] { "kolektiva.media" });
        ret.add(new String[] { "peertube.lefaut.fr" });
        ret.add(new String[] { "peertube.ichigo.everydayimshuflin.com" });
        ret.add(new String[] { "petitlutinartube.fr" });
        ret.add(new String[] { "videos.martyn.berlin" });
        ret.add(new String[] { "video.lundi.am" });
        ret.add(new String[] { "tube.pawelko.net" });
        ret.add(new String[] { "video.unkipamunich.fr" });
        ret.add(new String[] { "tube.chatelet.ovh" });
        ret.add(new String[] { "peertube.xwiki.com" });
        ret.add(new String[] { "tube.florimond.eu" });
        ret.add(new String[] { "peertube.it" });
        ret.add(new String[] { "peertube.taxinachtegel.de" });
        ret.add(new String[] { "peertube.marud.fr" });
        ret.add(new String[] { "peertube.mastodon.host" });
        ret.add(new String[] { "vid.wizards.zone" });
        ret.add(new String[] { "video.mindsforge.com" });
        ret.add(new String[] { "peertube.scic-tetris.org" });
        ret.add(new String[] { "peertube.semipvt.com" });
        ret.add(new String[] { "peertube.lagvoid.com" });
        ret.add(new String[] { "peertube.ireis.site" });
        ret.add(new String[] { "peertube.hardwarehookups.com.au" });
        ret.add(new String[] { "pt.diaspodon.fr" });
        ret.add(new String[] { "video.mugoreve.fr" });
        ret.add(new String[] { "nocensoring.net" });
        ret.add(new String[] { "tube.portes-imaginaire.org" });
        ret.add(new String[] { "peertube.robonomics.network" });
        ret.add(new String[] { "video.data-expertise.com" });
        ret.add(new String[] { "peertubenorge.com" });
        ret.add(new String[] { "meta-tube.de" });
        ret.add(new String[] { "video.minzord.eu.org" });
        ret.add(new String[] { "peertube.mazzonetto.eu" });
        ret.add(new String[] { "videos.ahp-numerique.fr" });
        ret.add(new String[] { "algorithmic.tv" });
        ret.add(new String[] { "peertube.gnumeria.fr" });
        ret.add(new String[] { "troo.tube" });
        ret.add(new String[] { "lanceur-alerte.tv" });
        ret.add(new String[] { "gwadloup.tv" });
        ret.add(new String[] { "matinik.tv" });
        ret.add(new String[] { "peertube.stemy.me" });
        ret.add(new String[] { "ptube.horsentiers.fr" });
        ret.add(new String[] { "videos.lavoixdessansvoix.org" });
        ret.add(new String[] { "peervideo.ru" });
        ret.add(new String[] { "peertube.at" });
        ret.add(new String[] { "p.eertu.be" });
        ret.add(new String[] { "video.marcorennmaus.de" });
        ret.add(new String[] { "doby.io" });
        ret.add(new String[] { "videos.gerdemann.me" });
        ret.add(new String[] { "video.hardlimit.com" });
        ret.add(new String[] { "peertube.debian.social" });
        ret.add(new String[] { "infotik.fr" });
        ret.add(new String[] { "tube.piweb.be" });
        ret.add(new String[] { "video.lewd.host" });
        ret.add(new String[] { "peertube.su" });
        ret.add(new String[] { "freespeech.tube" });
        ret.add(new String[] { "video.connor.money" });
        ret.add(new String[] { "video.linc.systems" });
        ret.add(new String[] { "manicphase.me" });
        ret.add(new String[] { "tube.nx12.net" });
        ret.add(new String[] { "video.hackers.town" });
        ret.add(new String[] { "video.iphodase.fr" });
        ret.add(new String[] { "tube.nox-rhea.org" });
        ret.add(new String[] { "peertube.fedilab.app" });
        ret.add(new String[] { "peertube.volaras.net" });
        ret.add(new String[] { "peertube.terranout.mine.nu" });
        ret.add(new String[] { "tube.fdn.fr" });
        ret.add(new String[] { "tv.datamol.org" });
        ret.add(new String[] { "peertube.demonix.fr" });
        ret.add(new String[] { "videos.hauspie.fr" });
        ret.add(new String[] { "peertube.social.my-wan.de" });
        ret.add(new String[] { "media.zat.im" });
        ret.add(new String[] { "peertube.club" });
        ret.add(new String[] { "peertube.bierzilla.fr" });
        ret.add(new String[] { "tube.maliweb.at" });
        ret.add(new String[] { "lexx.impa.me" });
        ret.add(new String[] { "peertube.ventresmous.fr" });
        ret.add(new String[] { "tube.linc.systems" });
        ret.add(new String[] { "mplayer.demouliere.eu" });
        ret.add(new String[] { "video.liberta.vip" });
        ret.add(new String[] { "banneddata.me" });
        ret.add(new String[] { "peertube.anduin.net" });
        ret.add(new String[] { "peertube.gcfamily.fr" });
        ret.add(new String[] { "video.ploud.fr" });
        ret.add(new String[] { "tube.plaf.fr" });
        ret.add(new String[] { "peertube.tech" });
        ret.add(new String[] { "video.lono.space" });
        ret.add(new String[] { "tube.bn4t.me" });
        ret.add(new String[] { "highvoltage.tv" });
        ret.add(new String[] { "tube.valinor.fr" });
        ret.add(new String[] { "tube.interhacker.space" });
        ret.add(new String[] { "peertube.simounet.net" });
        ret.add(new String[] { "tube.nah.re" });
        ret.add(new String[] { "dreiecksnebel.alex-detsch.de" });
        ret.add(new String[] { "stage.peertube.ch" });
        ret.add(new String[] { "peertube.ti-fr.com" });
        ret.add(new String[] { "video.turbo.chat" });
        ret.add(new String[] { "tube.hoga.fr" });
        ret.add(new String[] { "bittube.video" });
        ret.add(new String[] { "videos.globenet.org" });
        ret.add(new String[] { "merci-la-police.fr" });
        ret.add(new String[] { "tv.lapesto.fr" });
        ret.add(new String[] { "tube.nuagelibre.fr" });
        ret.add(new String[] { "videos.festivalparminous.org" });
        ret.add(new String[] { "juggling.digital" });
        ret.add(new String[] { "peertube.underworld.fr" });
        ret.add(new String[] { "peertube.anzui.de" });
        ret.add(new String[] { "video.ihatebeinga.live" });
        ret.add(new String[] { "pt.neko.bar" });
        ret.add(new String[] { "video.greenmycity.eu" });
        ret.add(new String[] { "tube.troopers.agency" });
        ret.add(new String[] { "tube.thechangebook.org" });
        ret.add(new String[] { "tube.rita.moe" });
        ret.add(new String[] { "wetube.ojamajo.moe" });
        ret.add(new String[] { "lepetitmayennais.fr.nf" });
        ret.add(new String[] { "tube.blob.cat" });
        ret.add(new String[] { "medias.pingbase.net" });
        ret.add(new String[] { "video.oh14.de" });
        ret.add(new String[] { "mytube.madzel.de" });
        ret.add(new String[] { "monplaisirtube.ddns.net" });
        ret.add(new String[] { "mytape.org" });
        ret.add(new String[] { "peertube.iselfhost.com" });
        ret.add(new String[] { "video.okaris.de" });
        ret.add(new String[] { "peertube.alpharius.io" });
        ret.add(new String[] { "p0.pm" });
        ret.add(new String[] { "peertube.video" });
        ret.add(new String[] { "video.isurf.ca" });
        ret.add(new String[] { "replay.jres.org" });
        ret.add(new String[] { "video.blender.org" });
        ret.add(new String[] { "peertube.020.pl" });
        ret.add(new String[] { "peertube.xoddark.com" });
        ret.add(new String[] { "peertube.mxinfo.fr" });
        ret.add(new String[] { "csictv.csic.es" });
        ret.add(new String[] { "peertube.alcalyn.app" });
        ret.add(new String[] { "video.imagotv.fr" });
        ret.add(new String[] { "ptube.rousset.nom.fr" });
        ret.add(new String[] { "tube.lesamarien.fr" });
        ret.add(new String[] { "peertube.cipherbliss.com" });
        ret.add(new String[] { "tube.azbyka.ru" });
        ret.add(new String[] { "greatview.video" });
        ret.add(new String[] { "runtube.re" });
        ret.add(new String[] { "tube.ac-amiens.fr" });
        ret.add(new String[] { "peertube.euskarabildua.eus" });
        ret.add(new String[] { "peertube.schaeferit.de" });
        ret.add(new String[] { "media.krashboyz.org" });
        ret.add(new String[] { "toobnix.org" });
        ret.add(new String[] { "video.emergeheart.info" });
        ret.add(new String[] { "videos.numerique-en-commun.fr" });
        ret.add(new String[] { "vault.mle.party" });
        ret.add(new String[] { "peertube.education-forum.com" });
        ret.add(new String[] { "tube.kdy.ch" });
        ret.add(new String[] { "peertube.linuxrocks.online" });
        ret.add(new String[] { "evertron.tv" });
        ret.add(new String[] { "yt.is.nota.live" });
        ret.add(new String[] { "videos.upr.fr" });
        ret.add(new String[] { "widemus.de" });
        ret.add(new String[] { "video.glassbeadcollective.org" });
        ret.add(new String[] { "peertube.kangoulya.org" });
        ret.add(new String[] { "flix.librenet.co.za" });
        ret.add(new String[] { "video.nesven.eu" });
        ret.add(new String[] { "vidz.dou.bet" });
        ret.add(new String[] { "tube.rebellion.global" });
        ret.add(new String[] { "videos.koumoul.com" });
        ret.add(new String[] { "tube.undernet.uy" });
        ret.add(new String[] { "peertube.cojo.uno" });
        ret.add(new String[] { "peertube.opencloud.lu" });
        ret.add(new String[] { "video.hdys.band" });
        ret.add(new String[] { "cattube.org" });
        ret.add(new String[] { "peertube.ch" });
        ret.add(new String[] { "tube.tappret.fr" });
        ret.add(new String[] { "peertube.hatthieves.es" });
        ret.add(new String[] { "peertube.la-famille-muller.fr" });
        ret.add(new String[] { "video.sftblw.moe" });
        ret.add(new String[] { "watch.snoot.tube" });
        ret.add(new String[] { "video.gcfam.net" });
        ret.add(new String[] { "peertube.pontostroy.gq" });
        ret.add(new String[] { "video.splat.soy" });
        ret.add(new String[] { "peerwatch.xyz" });
        ret.add(new String[] { "peertube.snargol.com" });
        ret.add(new String[] { "peertube.desmu.fr" });
        ret.add(new String[] { "ppstube.portageps.org" });
        ret.add(new String[] { "peertube.live" });
        ret.add(new String[] { "peertube.pl" });
        ret.add(new String[] { "xxxporn.co.uk" });
        ret.add(new String[] { "peertube.dk" });
        ret.add(new String[] { "vid.lubar.me" });
        ret.add(new String[] { "tube.benzo.online" });
        ret.add(new String[] { "tube.kapussinettes.ovh" });
        ret.add(new String[] { "peertube.rainbowswingers.net" });
        ret.add(new String[] { "videomensoif.ynh.fr" });
        ret.add(new String[] { "peertube.montecsys.fr" });
        ret.add(new String[] { "peer.tube" });
        ret.add(new String[] { "tube.nx-pod.de" });
        ret.add(new String[] { "video.monsieurbidouille.fr" });
        ret.add(new String[] { "tube.openalgeria.org" });
        ret.add(new String[] { "vid.lelux.fi" });
        ret.add(new String[] { "video.anormallostpod.ovh" });
        ret.add(new String[] { "tube.crapaud-fou.org" });
        ret.add(new String[] { "lostpod.space" });
        ret.add(new String[] { "exode.me" });
        ret.add(new String[] { "vis.ion.ovh" });
        ret.add(new String[] { "video.qoto.org" });
        ret.add(new String[] { "video.vny.fr" });
        ret.add(new String[] { "peervideo.club" });
        ret.add(new String[] { "tube.taker.fr" });
        ret.add(new String[] { "peertube.chantierlibre.org" });
        ret.add(new String[] { "tube.kicou.info" });
        ret.add(new String[] { "video.yukari.moe" });
        ret.add(new String[] { "peertube.co.uk" });
        ret.add(new String[] { "vod.mochi.academy" });
        ret.add(new String[] { "video.fitchfamily.org" });
        ret.add(new String[] { "video.fdlibre.eu" });
        ret.add(new String[] { "tube.fabrigli.fr" });
        ret.add(new String[] { "video.bruitbruit.com" });
        ret.add(new String[] { "peer.philoxweb.be" });
        ret.add(new String[] { "peertube.bilange.ca" });
        ret.add(new String[] { "libretube.net" });
        ret.add(new String[] { "libre.video" });
        ret.add(new String[] { "us.tv" });
        ret.add(new String[] { "peertube.sl-network.fr" });
        ret.add(new String[] { "peertube.dynlinux.io" });
        ret.add(new String[] { "peertube.david.durieux.family" });
        ret.add(new String[] { "v.kretschmann.social" });
        ret.add(new String[] { "tube.otter.sh" });
        ret.add(new String[] { "videos.funkwhale.audio" });
        ret.add(new String[] { "watch.44con.com" });
        ret.add(new String[] { "pony.tube" });
        ret.add(new String[] { "tube.danq.me" });
        ret.add(new String[] { "tube.fab-l3.org" });
        ret.add(new String[] { "tube.calculate.social" });
        ret.add(new String[] { "tube.netzspielplatz.de" });
        ret.add(new String[] { "peertube.laas.fr" });
        ret.add(new String[] { "tube.govital.net" });
        ret.add(new String[] { "video.ploud.jp" });
        ret.add(new String[] { "video.omniatv.com" });
        ret.add(new String[] { "peertube.ffs2play.fr" });
        ret.add(new String[] { "video.1000i100.fr" });
        ret.add(new String[] { "tube.worldofhauru.xyz" });
        ret.add(new String[] { "conf.tube" });
        ret.add(new String[] { "peertube.jackbot.fr" });
        ret.add(new String[] { "tube.extinctionrebellion.fr" });
        ret.add(new String[] { "peertube.f-si.org" });
        ret.add(new String[] { "video.subak.ovh" });
        ret.add(new String[] { "videos.koweb.fr" });
        ret.add(new String[] { "peertube.roflcopter.fr" });
        ret.add(new String[] { "peertube.floss-marketing-school.com" });
        ret.add(new String[] { "peertube.iriseden.eu" });
        ret.add(new String[] { "videos.ubuntu-paris.org" });
        ret.add(new String[] { "armstube.com" });
        ret.add(new String[] { "peertube.lol" });
        ret.add(new String[] { "peertube.normandie-libre.fr" });
        ret.add(new String[] { "peertube.slat.org" });
        ret.add(new String[] { "peertube.uno" });
        ret.add(new String[] { "peertube.servebeer.com" });
        ret.add(new String[] { "peertube.fedi.quebec" });
        ret.add(new String[] { "tube.h3z.jp" });
        ret.add(new String[] { "tube.plus200.com" });
        ret.add(new String[] { "gouttedeau.space" });
        ret.add(new String[] { "video.antirep.net" });
        ret.add(new String[] { "tube.ksl-bmx.de" });
        ret.add(new String[] { "tube.tchncs.de" });
        ret.add(new String[] { "video.devinberg.com" });
        ret.add(new String[] { "hitchtube.fr" });
        ret.add(new String[] { "peertube.kosebamse.com" });
        ret.add(new String[] { "yunopeertube.myddns.me" });
        ret.add(new String[] { "peertube.anon-kenkai.com" });
        ret.add(new String[] { "tube.maiti.info" });
        ret.add(new String[] { "videos.dinofly.com" });
        ret.add(new String[] { "videotape.me" });
        ret.add(new String[] { "video.lemediatv.fr" });
        ret.add(new String[] { "thickrips.cloud" });
        ret.add(new String[] { "pt.laurentkruger.fr" });
        ret.add(new String[] { "video.monarch-pass.net" });
        ret.add(new String[] { "peertube.artica.center" });
        ret.add(new String[] { "indymotion.fr" });
        ret.add(new String[] { "fanvid.stopthatimp.net" });
        ret.add(new String[] { "video.farci.org" });
        ret.add(new String[] { "v.lesterpig.com" });
        ret.add(new String[] { "tube.fede.re" });
        ret.add(new String[] { "pytu.be" });
        ret.add(new String[] { "devtube.dev-wiki.de" });
        ret.add(new String[] { "raptube.antipub.org" });
        ret.add(new String[] { "video.selea.se" });
        ret.add(new String[] { "peertube.mygaia.org" });
        ret.add(new String[] { "peertube.livingutopia.org" });
        ret.add(new String[] { "peertube.the-penguin.de" });
        ret.add(new String[] { "tube.anjara.eu" });
        ret.add(new String[] { "pt.pube.tk" });
        ret.add(new String[] { "peertube.me" });
        ret.add(new String[] { "video.latavernedejohnjohn.fr" });
        ret.add(new String[] { "peertube.pcservice46.fr" });
        ret.add(new String[] { "video.irem.univ-paris-diderot.fr" });
        ret.add(new String[] { "alttube.fr" });
        ret.add(new String[] { "video.coop.tools" });
        ret.add(new String[] { "video.cabane-libre.org" });
        ret.add(new String[] { "peertube.openstreetmap.fr" });
        ret.add(new String[] { "videos.alolise.org" });
        ret.add(new String[] { "irrsinn.video" });
        ret.add(new String[] { "video.antopie.org" });
        ret.add(new String[] { "scitech.video" });
        ret.add(new String[] { "video.amic37.fr" });
        ret.add(new String[] { "peertube.freeforge.eu" });
        ret.add(new String[] { "peertube.togart.de" });
        ret.add(new String[] { "tube.postblue.info" });
        ret.add(new String[] { "videos.domainepublic.net" });
        ret.add(new String[] { "peertube.cyber-tribal.com" });
        ret.add(new String[] { "video.gresille.org" });
        ret.add(new String[] { "cinema.yunohost.support" });
        ret.add(new String[] { "repro.video" });
        ret.add(new String[] { "videos.wakapo.com" });
        ret.add(new String[] { "pt.kircheneuenburg.de" });
        ret.add(new String[] { "peertube.asrun.eu" });
        ret.add(new String[] { "videos.side-ways.net" });
        ret.add(new String[] { "91video.online" });
        ret.add(new String[] { "videos-libr.es" });
        ret.add(new String[] { "tv.mooh.fr" });
        ret.add(new String[] { "nuage.acostey.fr" });
        ret.add(new String[] { "videos.pair2jeux.tube" });
        ret.add(new String[] { "videos.pueseso.club" });
        ret.add(new String[] { "media.assassinate-you.net" });
        ret.add(new String[] { "videos.squat.net" });
        ret.add(new String[] { "peertube.makotoworkshop.org" });
        ret.add(new String[] { "peertube.serveur.slv-valbonne.fr" });
        ret.add(new String[] { "videos.hack2g2.fr" });
        ret.add(new String[] { "pire.artisanlogiciel.net" });
        ret.add(new String[] { "video.netsyms.com" });
        ret.add(new String[] { "video.die-partei.social" });
        ret.add(new String[] { "video.writeas.org" });
        ret.add(new String[] { "videos.adhocmusic.com" });
        ret.add(new String[] { "tube.rfc1149.net" });
        ret.add(new String[] { "peertube.librelabucm.org" });
        ret.add(new String[] { "peertube.koehn.com" });
        ret.add(new String[] { "peertube.anarchmusicall.net" });
        ret.add(new String[] { "vid.y-y.li" });
        ret.add(new String[] { "diode.zone" });
        ret.add(new String[] { "peertube.nomagic.uk" });
        ret.add(new String[] { "video.rastapuls.com" });
        ret.add(new String[] { "video.mantlepro.com" });
        ret.add(new String[] { "video.deadsuperhero.com" });
        ret.add(new String[] { "peertube.musicstudio.pro" });
        ret.add(new String[] { "peertube.we-keys.fr" });
        ret.add(new String[] { "artitube.artifaille.fr" });
        ret.add(new String[] { "tube.midov.pl" });
        ret.add(new String[] { "tube.nemsia.org" });
        ret.add(new String[] { "tube.bruniau.net" });
        ret.add(new String[] { "tube.traydent.info" });
        ret.add(new String[] { "peertube.nayya.org" });
        ret.add(new String[] { "video.lequerrec.eu" });
        ret.add(new String[] { "peertube.amicale.net" });
        ret.add(new String[] { "aperi.tube" });
        ret.add(new String[] { "tube.ac-lyon.fr" });
        ret.add(new String[] { "video.lw1.at" });
        ret.add(new String[] { "yiny.org" });
        ret.add(new String[] { "videos.pofilo.fr" });
        ret.add(new String[] { "tube.lou.lt" });
        ret.add(new String[] { "betamax.video" });
        ret.add(new String[] { "video.typica.us" });
        ret.add(new String[] { "videos.lescommuns.org" });
        ret.add(new String[] { "videonaute.fr" });
        ret.add(new String[] { "dialup.express" });
        ret.add(new String[] { "megatube.lilomoino.fr" });
        ret.add(new String[] { "peertube.1312.media" });
        ret.add(new String[] { "skeptikon.fr" });
        ret.add(new String[] { "video.blueline.mg" });
        ret.add(new String[] { "tube.homecomputing.fr" });
        ret.add(new String[] { "tube.ouahpiti.info" });
        ret.add(new String[] { "video.tedomum.net" });
        ret.add(new String[] { "video.g3l.org" });
        ret.add(new String[] { "fontube.fr" });
        ret.add(new String[] { "peertube.gaialabs.ch" });
        ret.add(new String[] { "tube.kher.nl" });
        ret.add(new String[] { "peertube.qtg.fr" });
        ret.add(new String[] { "video.migennes.net" });
        ret.add(new String[] { "tube.p2p.legal" });
        ret.add(new String[] { "troll.tv" });
        ret.add(new String[] { "videos.iut-orsay.fr" });
        ret.add(new String[] { "peertube.solidev.net" });
        ret.add(new String[] { "videos.cemea.org" });
        ret.add(new String[] { "video.passageenseine.fr" });
        ret.add(new String[] { "peertube.touhoppai.moe" });
        ret.add(new String[] { "peer.hostux.social" });
        ret.add(new String[] { "share.tube" });
        ret.add(new String[] { "videos.benpro.fr" });
        ret.add(new String[] { "peertube.parleur.net" });
        ret.add(new String[] { "peertube.heraut.eu" });
        ret.add(new String[] { "peertube.gegeweb.eu" });
        ret.add(new String[] { "framatube.org" });
        ret.add(new String[] { "thinkerview.video" });
        ret.add(new String[] { "tube.conferences-gesticulees.net" });
        ret.add(new String[] { "peertube.datagueule.tv" });
        ret.add(new String[] { "video.lqdn.fr" });
        ret.add(new String[] { "tube.mochi.academy" });
        ret.add(new String[] { "video.colibris-outilslibres.org" });
        ret.add(new String[] { "peertube3.cpy.re" });
        ret.add(new String[] { "peertube2.cpy.re" });
        ret.add(new String[] { "peertube.cpy.re" });
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
        return buildAnnotationNames(getPluginDomains());
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
        String hostsStr = "";
        for (final String newHost : newInstances) {
            /* Outputs Java code to easily add new instances */
            System.out.println(String.format("ret.add(new String[] { \"%s\" });", newHost));
            hostsStr += newHost + ",";
        }
        System.out.println(hostsStr);
        /* TODO: Maybe add a check for missing/offline instances too */
        return newInstances;
    }

    /** Using API: https://docs.joinpeertube.org/api-rest-reference.html */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage("https://" + this.getHost() + "/api/v1/videos/" + this.getFID(link));
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
        filename += this.getHost().replace(".", "_") + "_";
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
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
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
}
