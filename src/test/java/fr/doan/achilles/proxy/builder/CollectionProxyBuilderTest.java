package fr.doan.achilles.proxy.builder;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mapping.entity.CompleteBean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import fr.doan.achilles.entity.metadata.PropertyMeta;
import fr.doan.achilles.proxy.collection.CollectionProxy;

@RunWith(MockitoJUnitRunner.class)
public class CollectionProxyBuilderTest
{
	@Mock
	private Map<Method, PropertyMeta<?>> dirtyMap;

	private Method setter;

	@Mock
	private PropertyMeta<String> propertyMeta;

	@Before
	public void setUp() throws Exception
	{
		setter = CompleteBean.class.getDeclaredMethod("setFriends", List.class);
	}

	@Test
	public void should_build() throws Exception
	{
		List<String> target = new ArrayList<String>();
		CollectionProxy<String> collectionProxy = CollectionProxyBuilder.builder(target).dirtyMap(dirtyMap).setter(setter).propertyMeta(propertyMeta)
				.build();

		assertThat(collectionProxy.getDirtyMap()).isSameAs(dirtyMap);

		collectionProxy.add("a");

		verify(dirtyMap).put(setter, propertyMeta);
	}
}