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
package info.archinnov.achilles.test.integration.tests;

import static org.fest.assertions.api.Assertions.*;
import info.archinnov.achilles.entity.manager.CQLPersistenceManager;
import info.archinnov.achilles.exception.AchillesStaleObjectStateException;
import info.archinnov.achilles.junit.AchillesTestResource.Steps;
import info.archinnov.achilles.proxy.CQLEntityInterceptor;
import info.archinnov.achilles.test.builders.TweetTestBuilder;
import info.archinnov.achilles.test.integration.AchillesInternalCQLResource;
import info.archinnov.achilles.test.integration.entity.CompleteBean;
import info.archinnov.achilles.test.integration.entity.CompleteBeanTestBuilder;
import info.archinnov.achilles.test.integration.entity.Tweet;
import info.archinnov.achilles.type.CounterBuilder;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import net.sf.cglib.proxy.Factory;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class EmOperationsIT {
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Rule
	public AchillesInternalCQLResource resource = new AchillesInternalCQLResource(Steps.AFTER_TEST, "CompleteBean");

	private CQLPersistenceManager manager = resource.getPersistenceManager();
	private Session session = manager.getNativeSession();

	@Test
	public void should_test() throws Exception {

	}

	@Test
	public void should_persist() throws Exception {

		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("DuyHai").age(35L)
				.addFriends("foo", "bar").addFollowers("George", "Paul").addPreference(1, "FR")
				.addPreference(2, "Paris").addPreference(3, "75014").version(CounterBuilder.incr(15L)).buid();

		manager.persist(entity);

		Row row = session
				.execute(
						"select name,age_in_years,friends,followers,preferences from completebean where id = "
								+ entity.getId()).one();

		assertThat(row.getLong("age_in_years")).isEqualTo(35L);
		assertThat(row.getList("friends", String.class)).containsExactly("foo", "bar");
		assertThat(row.getSet("followers", String.class)).containsOnly("George", "Paul");

		Map<Integer, String> preferences = row.getMap("preferences", Integer.class, String.class);

		assertThat(preferences).containsKey(1);
		assertThat(preferences).containsKey(2);
		assertThat(preferences).containsKey(3);

		assertThat(preferences).containsValue("FR");
		assertThat(preferences).containsValue("Paris");
		assertThat(preferences).containsValue("75014");

		row = session.execute(
				"select counter_value from achilles_counter_table where fqcn = '"
						+ CompleteBean.class.getCanonicalName() + "' and primary_key='" + entity.getId()
						+ "' and property_name='version'").one();

		assertThat(row.getLong("counter_value")).isEqualTo(15L);

	}

	@Test
	public void should_persist_empty_entity() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().buid();

		manager.persist(entity);

		CompleteBean found = manager.find(CompleteBean.class, entity.getId());

		assertThat(found).isNotNull();
		assertThat(found.getId()).isEqualTo(entity.getId());
	}

	@Test
	public void should_overwrite_existing_values_on_persist() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("DuyHai")
				.addFriends("foo", "bar", "qux").addFollowers("John", "Helen").addPreference(1, "Paris")
				.addPreference(2, "Ile de France").addPreference(3, "FRANCE").buid();

		manager.persist(entity);

		entity.getFriends().clear();
		entity.getFollowers().clear();
		entity.getPreferences().clear();

		// Should clean collections & maps before persisting again
		manager.persist(entity);

		entity = manager.find(CompleteBean.class, entity.getId());

		assertThat(entity.getFriends()).isNull();
		assertThat(entity.getFollowers()).isNull();
		assertThat(entity.getPreferences()).isNull();

	}

	@Test
	public void should_find() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("Jonathan").buid();

		manager.persist(entity);

		CompleteBean found = manager.find(CompleteBean.class, entity.getId());

		assertThat(found).isNotNull();
		assertThat(found).isInstanceOf(Factory.class);
	}

	@Test
	public void should_find_lazy_simple() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("Jonathan").label("label").buid();

		manager.persist(entity);

		CompleteBean found = manager.find(CompleteBean.class, entity.getId());

		Factory factory = (Factory) found;
		CQLEntityInterceptor<CompleteBean> interceptor = (CQLEntityInterceptor<CompleteBean>) factory.getCallback(0);

		Method getLabel = CompleteBean.class.getDeclaredMethod("getLabel");
		String label = (String) getLabel.invoke(interceptor.getTarget());

		assertThat(label).isNull();

		String lazyLabel = found.getLabel();

		assertThat(lazyLabel).isNotNull();
		assertThat(lazyLabel).isEqualTo("label");
	}

	@Test
	public void should_find_lazy_list() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("Jonathan").age(40L)
				.addFriends("bob", "alice").addFollowers("Billy", "Stephen", "Jacky").addPreference(1, "US")
				.addPreference(2, "New York").buid();

		manager.persist(entity);

		CompleteBean found = manager.find(CompleteBean.class, entity.getId());

		Factory factory = (Factory) found;
		CQLEntityInterceptor<CompleteBean> interceptor = (CQLEntityInterceptor<CompleteBean>) factory.getCallback(0);

		Method getFriends = CompleteBean.class.getDeclaredMethod("getFriends", (Class<?>[]) null);
		List<String> lazyFriends = (List<String>) getFriends.invoke(interceptor.getTarget());

		assertThat(lazyFriends).isNull();

		List<String> friends = found.getFriends();

		assertThat(friends).isNotNull();
		assertThat(friends).hasSize(2);
		assertThat(friends).containsExactly("bob", "alice");
	}

	@Test
	public void should_merge_modifications() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("Jonathan").age(40L)
				.addFriends("bob", "alice").addFollowers("Billy", "Stephen", "Jacky").addPreference(1, "US")
				.addPreference(2, "New York").buid();
		manager.persist(entity);

		CompleteBean found = manager.find(CompleteBean.class, entity.getId());

		found.setAge(100L);
		found.getFriends().add("eve");
		found.getPreferences().put(1, "FR");

		CompleteBean merged = manager.merge(found);

		assertThat(merged).isSameAs(found);

		assertThat(merged.getFriends()).hasSize(3);
		assertThat(merged.getFriends()).containsExactly("bob", "alice", "eve");
		assertThat(merged.getPreferences()).hasSize(2);
		assertThat(merged.getPreferences().get(1)).isEqualTo("FR");

		Row row = session.execute("select * from completebean where id=" + entity.getId()).one();

		assertThat(row.getLong("age_in_years")).isEqualTo(100L);
		assertThat(row.getList("friends", String.class)).containsExactly("bob", "alice", "eve");
		Map<Integer, String> preferences = row.getMap("preferences", Integer.class, String.class);
		assertThat(preferences.get(1)).isEqualTo("FR");
		assertThat(preferences.get(2)).isEqualTo("New York");

	}

	@Test
	public void should_remove_property_after_merge() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("Jonathan").age(40L)
				.addFriends("bob", "alice").addFollowers("Billy", "Stephen", "Jacky").addPreference(1, "US")
				.addPreference(2, "New York").buid();
		manager.persist(entity);

		CompleteBean found = manager.find(CompleteBean.class, entity.getId());

		found.setName(null);
		found.setFriends(null);
		found.setFollowers(null);
		found.setPreferences(null);

		manager.merge(found);

		found = manager.find(CompleteBean.class, entity.getId());

		assertThat(found.getName()).isNull();
		assertThat(found.getFriends()).isNull();
		assertThat(found.getFollowers()).isNull();
		assertThat(found.getPreferences()).isNull();

	}

	@Test
	public void should_exception_when_trying_to_modify_primary_key() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("Jonathan").age(40L)
				.addFriends("bob", "alice").addFollowers("Billy", "Stephen", "Jacky").addPreference(1, "US")
				.addPreference(2, "New York").buid();

		entity = manager.merge(entity);

		exception.expect(IllegalAccessException.class);
		exception.expectMessage("Cannot change primary key value for existing entity");

		entity.setId(RandomUtils.nextLong());
	}

	@Test
	public void should_return_managed_entity_after_merge() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().buid();
		entity = manager.merge(entity);

		assertThat(entity).isInstanceOf(Factory.class);
	}

	@Test
	public void should_return_same_entity_as_merged_entity_when_managed() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("Jonathan").buid();
		Tweet tweet = TweetTestBuilder.tweet().randomId().content("tweet").buid();
		entity.setWelcomeTweet(tweet);

		entity = manager.merge(entity);

		CompleteBean entity2 = manager.merge(entity);

		assertThat(entity2).isSameAs(entity);
		assertThat(entity.getWelcomeTweet()).isEqualTo(tweet);
		assertThat(entity2.getWelcomeTweet()).isEqualTo(tweet);
	}

	@Test
	public void should_remove() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("DuyHai").age(35L)
				.addFriends("foo", "bar").addFollowers("George", "Paul").addPreference(1, "FR")
				.addPreference(2, "Paris").addPreference(3, "75014").buid();

		entity = manager.merge(entity);

		manager.remove(entity);

		assertThat(manager.find(CompleteBean.class, entity.getId())).isNull();

		List<Row> rows = session.execute("select * from completebean where id=" + entity.getId()).all();
		assertThat(rows).isEmpty();
	}

	@Test
	public void should_remove_by_id() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("DuyHai").age(35L)
				.addFriends("foo", "bar").addFollowers("George", "Paul").addPreference(1, "FR")
				.addPreference(2, "Paris").addPreference(3, "75014").buid();

		entity = manager.merge(entity);

		manager.removeById(CompleteBean.class, entity.getId());

		assertThat(manager.find(CompleteBean.class, entity.getId())).isNull();

		List<Row> rows = session.execute("select * from completebean where id=" + entity.getId()).all();
		assertThat(rows).isEmpty();
	}

	@Test(expected = IllegalStateException.class)
	public void should_exception_when_removing_transient_entity() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("DuyHai").age(35L)
				.addFriends("foo", "bar").addFollowers("George", "Paul").addPreference(1, "FR")
				.addPreference(2, "Paris").addPreference(3, "75014").buid();

		manager.remove(entity);
	}

	@Test
	public void should_get_reference() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("DuyHai").age(35L)
				.addFriends("foo", "bar").addFollowers("George", "Paul").addPreference(1, "FR")
				.addPreference(2, "Paris").addPreference(3, "75014").buid();

		manager.persist(entity);

		CompleteBean foundBean = manager.getReference(CompleteBean.class, entity.getId());

		assertThat(foundBean).isNotNull();

		// Real object should be empty
		CompleteBean realObject = manager.unwrap(foundBean);

		assertThat(realObject.getId()).isEqualTo(entity.getId());
		assertThat(realObject.getName()).isNull();
		assertThat(realObject.getAge()).isNull();
		assertThat(realObject.getFriends()).isNull();
		assertThat(realObject.getFollowers()).isNull();
		assertThat(realObject.getPreferences()).isNull();

		assertThat(foundBean.getId()).isEqualTo(entity.getId());
		assertThat(foundBean.getName()).isEqualTo("DuyHai");
		assertThat(foundBean.getAge()).isEqualTo(35L);
		assertThat(foundBean.getFriends()).containsExactly("foo", "bar");
		assertThat(foundBean.getFollowers()).contains("George", "Paul");

		assertThat(foundBean.getPreferences()).containsKey(1);
		assertThat(foundBean.getPreferences()).containsKey(2);
		assertThat(foundBean.getPreferences()).containsKey(3);

		assertThat(foundBean.getPreferences()).containsValue("FR");
		assertThat(foundBean.getPreferences()).containsValue("Paris");
		assertThat(foundBean.getPreferences()).containsValue("75014");
	}

	@Test
	public void should_exception_refreshing_non_managed_entity() throws Exception {
		CompleteBean completeBean = CompleteBeanTestBuilder.builder().randomId().name("name").buid();
		exception.expect(IllegalStateException.class);
		exception.expectMessage("The entity '" + completeBean + "' is not in 'managed' state");
		manager.refresh(completeBean);
	}

	@Test
	public void should_refresh() throws Exception {

		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("DuyHai").age(35L)
				.addFriends("foo", "bar").addFollowers("George", "Paul").addPreference(1, "FR")
				.addPreference(2, "Paris").addPreference(3, "75014").buid();

		entity = manager.merge(entity);

		entity.getFriends();

		session.execute("UPDATE completebean SET name='DuyHai_modified' WHERE id=" + entity.getId());
		session.execute("UPDATE completebean SET friends=friends + ['qux'] WHERE id=" + entity.getId());

		manager.refresh(entity);

		assertThat(entity.getName()).isEqualTo("DuyHai_modified");
		assertThat(entity.getFriends()).hasSize(3);
		assertThat(entity.getFriends().get(2)).isEqualTo("qux");
	}

	@Test(expected = AchillesStaleObjectStateException.class)
	public void should_exception_when_staled_object_during_refresh() throws Exception {

		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("DuyHai").buid();

		entity = manager.merge(entity);

		session.execute("DELETE FROM completebean WHERE id=" + entity.getId());

		manager.refresh(entity);
	}

	@Test
	public void should_find_unmapped_field() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("DuyHai")
				//
				.label("label").age(35L).addFriends("foo", "bar").addFollowers("George", "Paul").addPreference(1, "FR")
				.addPreference(2, "Paris").addPreference(3, "75014").buid();

		entity = manager.merge(entity);

		assertThat(entity.getLabel()).isEqualTo("label");
	}

	@Test
	public void should_return_null_and_not_wrapper_for_null_values() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("DuyHai") //
				.buid();

		entity.setFriends(null);
		entity.setFollowers(null);
		entity.setPreferences(null);

		manager.persist(entity);

		entity = manager.find(CompleteBean.class, entity.getId());

		assertThat(entity.getFriends()).isNull();
		assertThat(entity.getFollowers()).isNull();
		assertThat(entity.getPreferences()).isNull();
		assertThat(entity.getLabel()).isNull();
		assertThat(entity.getAge()).isNull();
	}
}
