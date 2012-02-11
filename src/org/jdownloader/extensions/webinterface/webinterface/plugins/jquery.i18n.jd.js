function translate() {
	/* MAINSITE */
	$('#nav_dashboard').find("a").first().html($.i18n._('main.nav.dashboard'));
	$('#nav_downloads').find("a").first().text($.i18n._('main.nav.downloads.head'));
	$('#nav_downloads__running').find("a").first().text($.i18n._('main.nav.downloads.running'));
	$('#nav_downloads__paused').find("a").first().text($.i18n._('main.nav.downloads.paused'));
	$('#nav_downloads__done').find("a").first().text($.i18n._('main.nav.downloads.done'));
	$('#nav_downloads__error').find("a").first().text($.i18n._('main.nav.downloads.aborted'));
	$('#nav_linkadd').find("a").first().text($.i18n._('main.nav.addlinks'));
	$('#nav_passwd').find("a").first().text($.i18n._('main.nav.passwordlist'));
	$('#nav_accounts').find("a").first().text($.i18n._('main.nav.accounts'));
	$('#nav_settings').find("a").first().text($.i18n._('main.nav.settings'));
	/* Dashboard */
	$('#dashboard__report_donedl').text($.i18n._('dashboard.report.donedl'));
	$('#dashboard__report_error').text($.i18n._('dashboard.report.error'));
	$('#dashboard__report_speed').text($.i18n._('dashboard.report.speed'));
	$('#dashboard__report_donetime').text($.i18n._('dashboard.report.donetime'));
	$('#dashboard__report_activeprem').text($.i18n._('dashboard.report.activeprem'));
	
}