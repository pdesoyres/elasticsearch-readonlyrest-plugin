/*
 *    This file is part of ReadonlyREST.
 *
 *    ReadonlyREST is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    ReadonlyREST is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with ReadonlyREST.  If not, see http://www.gnu.org/licenses/
 */

package tech.beshu.ror.settings.definitions;

import com.google.common.collect.Sets;
import tech.beshu.ror.commons.settings.RawSettings;
import tech.beshu.ror.commons.settings.SettingsMalformedException;
import tech.beshu.ror.settings.rules.CacheSettings;
import tech.beshu.ror.settings.rules.NamedSettings;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class LdapSettings implements CacheSettings, NamedSettings {

  private static final String NAME = "name";
  private static final String HOST = "host";
  private static final String SERVERS = "servers";
  private static final String PORT = "port";
  private static final String SSL_ENABLED = "ssl_enabled";
  private static final String TRUST_ALL_CERTS = "ssl_trust_all_certs";
  private static final String SEARCH_USER = "search_user_base_DN";
  private static final String USER_ID = "user_id_attribute";
  private static final String CONNECTION_POOL_SIZE = "connection_pool_size";
  private static final String CONNECTION_TIMEOUT = "connection_timeout_in_sec";
  private static final String REQUEST_TIMEOUT = "request_timeout_in_sec";
  private static final String CACHE = "cache_ttl_in_sec";
  private static final String HA_KEY = "ha";

  private static final int DEFAULT_PORT = 389;
  private static final boolean DEFAULT_SSL_ENABLED = true;
  private static final boolean DEFAULT_TRUST_ALL_CERTS = false;
  private static final String DEFAULT_USER_ID_ATTRIBUTE = "uid";
  private static final int DEFAULT_CONNECTION_POOL_SIZE = 30;
  private static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(1);
  private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(1);
  private static final Duration DEFAULT_CACHE_TTL = Duration.ZERO;
  private static final HA DEFAULT_HA = HA.FAILOVER;

  private final String name;
  private final String host;
  private final int port;
  private final boolean isSslEnabled;
  private final boolean trustAllCertificates;
  private final Optional<SearchingUserSettings> searchingUserSettings;
  private final String searchUserBaseDn;
  private final String userIdAttribute;
  private final int connectionPoolSize;
  private final Duration connectionTimeout;
  private final Duration requestTimeout;
  private final Duration cacheTtl;
  private final HA ha;
  private Set<String> availableGroups = Sets.newHashSet();
  private Set<String> servers;

  protected LdapSettings(RawSettings settings) {
    if(!settings.stringOpt(HOST).isPresent() && !settings.notEmptyListOpt(SERVERS).isPresent()){
      throw new SettingsMalformedException("Server information missing: use either 'host' and 'port' or 'servers' option");
    }
    if (settings.stringOpt(HA_KEY).isPresent() && (settings.stringOpt(HOST).isPresent() || settings.intOpt(PORT).isPresent())) {
      throw new SettingsMalformedException(
          "Cannot accept single server settings (host,port) AND multi server configuration (servers) at the same time.");
    }
    if (settings.stringOpt(HA_KEY).isPresent() && settings.stringOpt(HOST).isPresent()) {
      throw new SettingsMalformedException(
          "Please specify more than one LDAP server using 'servers' to use HA");
    }
    if (settings.stringOpt(SSL_ENABLED).isPresent() && !settings.stringOpt(HOST).isPresent()) {
      throw new SettingsMalformedException(
          "When using multi-server, the option '" + SSL_ENABLED + "' can't be used. Please use ldaps:// schema while listing the 'servers'");
    }
    this.name = settings.stringReq(NAME);
    this.host = settings.stringOpt(HOST).orElse(null);
    this.servers = new HashSet(settings.notEmptyListOpt(SERVERS).orElse(Collections.emptyList()));
    this.port = settings.intOpt(PORT).orElse(DEFAULT_PORT);
    this.isSslEnabled = settings.booleanOpt(SSL_ENABLED).orElse(DEFAULT_SSL_ENABLED);
    this.trustAllCertificates = settings.booleanOpt(TRUST_ALL_CERTS).orElse(DEFAULT_TRUST_ALL_CERTS);
    this.searchingUserSettings = SearchingUserSettings.from(settings);
    this.searchUserBaseDn = settings.stringReq(SEARCH_USER);
    this.userIdAttribute = settings.stringOpt(USER_ID).orElse(DEFAULT_USER_ID_ATTRIBUTE);
    this.connectionPoolSize = settings.intOpt(CONNECTION_POOL_SIZE).orElse(DEFAULT_CONNECTION_POOL_SIZE);
    this.connectionTimeout = settings.intOpt(CONNECTION_TIMEOUT).map(Duration::ofSeconds).orElse(DEFAULT_CONNECTION_TIMEOUT);
    this.requestTimeout = settings.intOpt(REQUEST_TIMEOUT).map(Duration::ofSeconds).orElse(DEFAULT_REQUEST_TIMEOUT);
    this.cacheTtl = settings.intOpt(CACHE).map(Duration::ofSeconds).orElse(DEFAULT_CACHE_TTL);
    this.ha = HA.valueOf(settings.stringOpt(HA_KEY).orElse(DEFAULT_HA.name()));
  }

  @Override
  public String getName() {
    return name;
  }

  public Set<String> getAvailableGroups() {
    return availableGroups;
  }

  public void setAvailableGroups(Set<String> availableGroups) {
    this.availableGroups = availableGroups;
  }

  public String getHost() {
    return host;
  }

  public HA getHa() {
    return ha;
  }

  public Set<String> getServers() {
    return servers;
  }

  public int getPort() {
    return port;
  }

  public boolean isSslEnabled() {
    return isSslEnabled;
  }

  public boolean isTrustAllCertificates() {
    return trustAllCertificates;
  }

  public Optional<SearchingUserSettings> getSearchingUserSettings() {
    return searchingUserSettings;
  }

  public String getSearchUserBaseDn() {
    return searchUserBaseDn;
  }

  public String getUserIdAttribute() {
    return userIdAttribute;
  }

  public int getConnectionPoolSize() {
    return connectionPoolSize;
  }

  public Duration getConnectionTimeout() {
    return connectionTimeout;
  }

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  @Override
  public Duration getCacheTtl() {
    return cacheTtl;
  }

  public enum HA {
    FAILOVER, ROUNDR_ROBIN
  }

  public static class SearchingUserSettings {
    private static final String BIND_DN = "bind_dn";
    private static final String BIND_PASS = "bind_password";

    private final String dn;
    private final String password;

    SearchingUserSettings(String dn, String password) {
      this.dn = dn;
      this.password = password;
    }

    static Optional<SearchingUserSettings> from(RawSettings settings) {
      Optional<String> bindDn = settings.stringOpt(BIND_DN);
      Optional<String> bindPassword = settings.stringOpt(BIND_PASS);
      if ((bindDn.isPresent() && !bindPassword.isPresent()) ||
          (!bindDn.isPresent() && bindPassword.isPresent())) {
        throw new SettingsMalformedException("'" + BIND_DN + "' & '" + BIND_PASS + "' should be both present or both absent");
      }
      return bindDn.flatMap(bdn -> bindPassword.map(bp -> new SearchingUserSettings(bdn, bp)));
    }

    public String getDn() {
      return dn;
    }

    public String getPassword() {
      return password;
    }
  }
}
