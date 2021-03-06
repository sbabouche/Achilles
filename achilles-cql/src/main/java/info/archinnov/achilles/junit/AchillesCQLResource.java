/**
 *
 * Copyright (C) 2012-2013 DuyHai DOAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.archinnov.achilles.junit;

import static info.archinnov.achilles.embedded.CassandraEmbeddedConfigParameters.*;
import info.archinnov.achilles.embedded.CQLEmbeddedServer;
import info.archinnov.achilles.entity.manager.CQLPersistenceManager;
import info.archinnov.achilles.entity.manager.CQLPersistenceManagerFactory;
import info.archinnov.achilles.validation.Validator;

import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableMap;

public class AchillesCQLResource extends AchillesTestResource {

	private final CQLPersistenceManagerFactory pmf;

	private final CQLPersistenceManager manager;

	private final CQLEmbeddedServer server;

	private final Session session;

	/**
	 * Initialize a new embedded Cassandra server
	 * 
	 * @param entityPackages
	 *            packages to scan for entity discovery, comma separated
	 * @param tables
	 *            list of tables to truncate before and after tests
	 */
	AchillesCQLResource(String entityPackages, String... tables) {
		super(tables);

		final ImmutableMap<String, Object> config = ImmutableMap.<String, Object> of(CLEAN_CASSANDRA_DATA_FILES, true,
				ENTITY_PACKAGES, entityPackages, KEYSPACE_NAME, DEFAULT_ACHILLES_TEST_KEYSPACE_NAME,
				KEYSPACE_DURABLE_WRITE, false);

		server = new CQLEmbeddedServer(config);
		pmf = server.getPersistenceManagerFactory(DEFAULT_ACHILLES_TEST_KEYSPACE_NAME);
		manager = server.getPersistenceManager(DEFAULT_ACHILLES_TEST_KEYSPACE_NAME);
		session = server.getNativeSession(DEFAULT_ACHILLES_TEST_KEYSPACE_NAME);
	}

	/**
	 * Initialize a new embedded Cassandra server
	 * 
	 * @param entityPackages
	 *            packages to scan for entity discovery, comma separated
	 * @param cleanUpSteps
	 *            when to truncate tables for clean up. Possible values are :
	 *            Steps.BEFORE_TEST, Steps.AFTER_TEST and Steps.BOTH (Default
	 *            value) <br/>
	 * <br/>
	 * @param tables
	 *            list of tables to truncate before, after or before and after
	 *            tests, depending on the 'cleanUpSteps' parameters
	 */
	AchillesCQLResource(String entityPackages, Steps cleanUpSteps, String... tables) {
		super(cleanUpSteps, tables);

		Validator.validateNotBlank(entityPackages, "Entity packages should be provided");
		final ImmutableMap<String, Object> config = ImmutableMap.<String, Object> of(CLEAN_CASSANDRA_DATA_FILES, true,
				ENTITY_PACKAGES, entityPackages, KEYSPACE_NAME, DEFAULT_ACHILLES_TEST_KEYSPACE_NAME,
				KEYSPACE_DURABLE_WRITE, false);

		server = new CQLEmbeddedServer(config);
		pmf = server.getPersistenceManagerFactory(DEFAULT_ACHILLES_TEST_KEYSPACE_NAME);
		manager = server.getPersistenceManager(DEFAULT_ACHILLES_TEST_KEYSPACE_NAME);
		session = server.getNativeSession(DEFAULT_ACHILLES_TEST_KEYSPACE_NAME);
	}

	/**
	 * Return a singleton CQLPersistenceManagerFactory
	 * 
	 * @return CQLPersistenceManagerFactory singleton
	 */
	public CQLPersistenceManagerFactory getPersistenceManagerFactory() {
		return pmf;
	}

	/**
	 * Return a singleton CQLPersistenceManager
	 * 
	 * @return CQLPersistenceManager singleton
	 */
	public CQLPersistenceManager getPersistenceManager() {
		return manager;
	}

	/**
	 * Return a native CQL3 Session
	 * 
	 * @return native CQL3 Session
	 */
	public Session getNativeSession() {
		return session;
	}

	@Override
	protected void truncateTables() {
		if (tables != null) {
			for (String table : tables) {
				server.truncateTable(DEFAULT_ACHILLES_TEST_KEYSPACE_NAME, table);
			}
		}
	}

}
