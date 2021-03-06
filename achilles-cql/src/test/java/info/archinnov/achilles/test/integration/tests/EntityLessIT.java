package info.archinnov.achilles.test.integration.tests;

import static info.archinnov.achilles.configuration.CQLConfigurationParameters.*;
import static info.archinnov.achilles.embedded.AchillesEmbeddedServer.*;
import static info.archinnov.achilles.embedded.CassandraEmbeddedConfigParameters.DEFAULT_CASSANDRA_HOST;
import static org.fest.assertions.api.Assertions.*;

import info.archinnov.achilles.embedded.AchillesEmbeddedServer;
import info.archinnov.achilles.entity.manager.CQLPersistenceManager;
import info.archinnov.achilles.entity.manager.CQLPersistenceManagerFactory;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class EntityLessIT {

	private CQLPersistenceManager manager;

	@Before
	public void setUp() {
       Map<String, Object> configMap = new HashMap<String, Object>();

		configMap.put(CONNECTION_CONTACT_POINTS_PARAM, DEFAULT_CASSANDRA_HOST);
		configMap.put(CONNECTION_PORT_PARAM, AchillesEmbeddedServer.getCqlPort());
		configMap.put(KEYSPACE_NAME_PARAM, "system");
		CQLPersistenceManagerFactory pmf = new CQLPersistenceManagerFactory(configMap);
		manager = pmf.createPersistenceManager();
	}

	@Test
	public void should_bootstrap_achilles_without_entity_package_for_native_query() throws Exception {
		Map<String, Object> keyspaceMap = manager.nativeQuery(
				"SELECT keyspace_name from system.schema_keyspaces where keyspace_name='system'").first();

		assertThat(keyspaceMap).hasSize(1);
		assertThat(keyspaceMap.get("keyspace_name")).isEqualTo("system");
	}
}
