#define _GNU_SOURCE
#include <sys/types.h>

#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <time.h>
#include <stdio.h>

#include "server.h"
#include "connections.h"
#include "response.h"
#include "connections.h"
#include "log.h"

#include "plugin.h"

#include "inet_ntop_cache.h"
#include "version.h"

/*
 * IPCheck Modul for lighttpd ,Copyright (C) 2009  by Jiaz (jiaz@jdownloader.org)
 * INTSTALL:
 * 1.) copy to src folder
 * 2.) add

 lib_LTLIBRARIES += mod_ipcheck.la
 mod_ipcheck_la_SOURCES = mod_ipcheck.c
 mod_ipcheck_la_LDFLAGS = -module -export-dynamic -avoid-version -no-undefined
 mod_ipcheck_la_LIBADD = $(common_libadd)

 * to Makefile.am
 *
 * 3.)autoreconf -fi and ./configure ...
 *
 */

typedef struct {
	buffer *ipcheck_url;
} plugin_config;

typedef struct {
	PLUGIN_DATA;

	buffer *module_list;

	plugin_config **config_storage;

	plugin_config conf;
} plugin_data;

INIT_FUNC(mod_ipcheck_init) {
	plugin_data *p;

	p = calloc(1, sizeof(*p));
	p->module_list = buffer_init();

	return p;
}

FREE_FUNC(mod_ipcheck_free) {
	plugin_data *p = p_d;

	UNUSED(srv);

	if (!p) return HANDLER_GO_ON;

	buffer_free(p->module_list);

	if (p->config_storage) {
		size_t i;
		for (i = 0; i < srv->config_context->used; i++) {
			plugin_config *s = p->config_storage[i];

			buffer_free(s->ipcheck_url);

			free(s);
		}
		free(p->config_storage);
	}

	free(p);

	return HANDLER_GO_ON;
}

SETDEFAULTS_FUNC(mod_ipcheck_set_defaults) {
	plugin_data *p = p_d;
	size_t i;

	config_values_t cv[] = {
		{	"ipcheck.ipcheck-url", NULL, T_CONFIG_STRING, T_CONFIG_SCOPE_CONNECTION},
		{	NULL, NULL, T_CONFIG_UNSET, T_CONFIG_SCOPE_UNSET}
	};

	if (!p) return HANDLER_ERROR;

	p->config_storage = calloc(1, srv->config_context->used * sizeof(specific_config *));

	for (i = 0; i < srv->config_context->used; i++) {
		plugin_config *s;

		s = calloc(1, sizeof(plugin_config));
		s->ipcheck_url = buffer_init();

		cv[0].destination = s->ipcheck_url;

		p->config_storage[i] = s;

		if (0 != config_insert_values_global(srv, ((data_config *)srv->config_context->data[i])->value, cv)) {
			return HANDLER_ERROR;
		}
	}

	return HANDLER_GO_ON;
}

static handler_t mod_ipcheck_handle_server_status(server *srv, connection *con,
		void *p_d) {
	plugin_data *p = p_d;
	buffer *b = p->module_list;
	b = chunkqueue_get_append_buffer(con->write_queue);
	buffer_append_string(b, inet_ntop_cache_get_ip(srv, &(con->dst_addr)));

	con->http_status = 200;
	con->file_finished = 1;

	return HANDLER_FINISHED;
}

#define PATCH(x) \
	p->conf.x = s->x;
static int mod_ipcheck_patch_connection(server *srv, connection *con,
		plugin_data *p) {
	size_t i, j;
	plugin_config *s = p->config_storage[0];

	PATCH(ipcheck_url);

	/* skip the first, the global context */
	for (i = 1; i < srv->config_context->used; i++) {
		data_config *dc = (data_config *) srv->config_context->data[i];
		s = p->config_storage[i];

		/* condition didn't match */
		if (!config_check_cond(srv, con, dc))
			continue;

		/* merge config */
		for (j = 0; j < dc->value->used; j++) {
			data_unset *du = dc->value->data[j];

			if (buffer_is_equal_string(du->key, CONST_STR_LEN(
					"ipcheck.ipcheck-url"))) {
				PATCH(ipcheck_url);
			}
		}
	}

	return 0;
}

static handler_t mod_ipcheck_handler(server *srv, connection *con, void *p_d) {
	plugin_data *p = p_d;

	if (con->mode != DIRECT)
		return HANDLER_GO_ON;

	mod_ipcheck_patch_connection(srv, con, p);

	if (!buffer_is_empty(p->conf.ipcheck_url) && buffer_is_equal(
			p->conf.ipcheck_url, con->uri.path)) {
		return mod_ipcheck_handle_server_status(srv, con, p_d);
	}

	return HANDLER_GO_ON;
}

int mod_ipcheck_plugin_init(plugin *p);
int mod_ipcheck_plugin_init(plugin *p) {
	p->version = LIGHTTPD_VERSION_ID;
	p->name = buffer_init_string("ipcheck");

	p->init = mod_ipcheck_init;
	p->cleanup = mod_ipcheck_free;
	p->set_defaults = mod_ipcheck_set_defaults;

	p->handle_uri_clean = mod_ipcheck_handler;

	p->data = NULL;

	return 0;
}
