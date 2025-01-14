/* 
Copyright 2021 WeAreFrank! 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
*/
package org.frankframework.frankdoc;

import static org.frankframework.frankdoc.Utils.isConfigChildSetter;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.frankframework.frankdoc.wrapper.FrankClass;
import org.frankframework.frankdoc.wrapper.FrankClassRepository;
import org.frankframework.frankdoc.wrapper.FrankDocException;
import org.frankframework.frankdoc.wrapper.FrankMethod;
import org.frankframework.frankdoc.wrapper.TestUtil;
import org.junit.Before;
import org.junit.Test;
 
public class UtilsTest {
	private static final String SIMPLE = "org.frankframework.frankdoc.testtarget.simple";
	private FrankClassRepository repository;

	@Before
	public void setUp() {
		repository = TestUtil.getFrankClassRepositoryDoclet(SIMPLE);
	}

	@Test
	public void testGetSpringBeans() throws FrankDocException {
		List<FrankClass> actual = repository.findClass(SIMPLE + ".IListener").getInterfaceImplementations();
		Collections.sort(actual, Comparator.comparing(FrankClass::getName));
		assertEquals(4, actual.size());
		Iterator<FrankClass> it = actual.iterator();
		FrankClass first = it.next();
		assertEquals(SIMPLE + ".ListenerChild", first.getName());
		FrankClass second = it.next();
		assertEquals(SIMPLE + ".ListenerGrandChild", second.getName());
		FrankClass third = it.next();
		assertEquals(SIMPLE + ".ListenerParent", third.getName());
	}

	@Test
	public void whenMethodIsConfigChildSetterThenRecognized() throws FrankDocException {
		assertTrue(isConfigChildSetter(getTestMethod("setListener")));
	}

	private FrankMethod getTestMethod(String name) throws FrankDocException {
		FrankClass listenerChildClass = repository.findClass(SIMPLE + ".ListenerChild");
		for(FrankMethod m: listenerChildClass.getDeclaredAndInheritedMethods()) {
			if(m.getName().equals(name)) {
				return m;
			}
		}
		throw new RuntimeException("No method in ListenerChild for method name: " + name);
	}

	@Test
	public void whenAttributeSetterThenNotConfigChildSetter() throws FrankDocException {
		assertFalse(isConfigChildSetter(getTestMethod("setChildAttribute")));
	}

	@Test
	public void whenMethodHasTwoArgsThenNotConfigChildSetter() throws FrankDocException {
		assertFalse(isConfigChildSetter(getTestMethod("invalidConfigChildSetterTwoArgs")));
	}

	@Test
	public void whenMethodReturnsPrimitiveThenNotConfigChildSetter() throws FrankDocException {
		assertFalse(isConfigChildSetter(getTestMethod("invalidConfigChildSetterReturnsInt")));
	}

	@Test
	public void whenMethodReturnsStringThenNotConfigChildSetter()  throws FrankDocException {
		assertFalse(isConfigChildSetter(getTestMethod("invalidConfigChildSetterReturnsString")));
	}

	@Test
	public void whenMethodStartsWithRegisterAndTakesStringThenTextConfigChildSetter() throws FrankDocException {
		assertTrue(isConfigChildSetter(getTestMethod("registerTextConfigChild")));
	}

	@Test
	public void whenMethodStartsWithSetAndTakesStringThenNotConfigChildSetter() throws FrankDocException {
		assertFalse(isConfigChildSetter(getTestMethod("setNotTextConfigChildButAttribute")));
	}

	@Test(expected = FrankDocException.class)
	public void whenStringHasUnfinishedJavaDocLinkThenError() throws Exception {
		// No closing "}"
		Utils.flattenJavaDocLinksToLastWords("{@link Receiver");
	}

	@Test
	public void nullEqualsNull() {
		assertTrue(Utils.equalsNullable(null, null));
	}

	@Test
	public void nullDoesNotEqualNonNull() {
		assertFalse(Utils.equalsNullable(null, "something"));
	}

	@Test
	public void nonNullDoesNotEqualNull() {
		assertFalse(Utils.equalsNullable("something", null));
	}

	@Test
	public void nonEqualObjectsAreNotEqualNullable() {
		assertFalse(Utils.equalsNullable("this", "other"));
	}

	@Test
	public void equalObjectsAreEqualNullable() {
		assertTrue(Utils.equalsNullable("this", "thi" + "s"));
	}

	@Test
	public void whenDescriptionHasSimpleHtmlTagThenFound() {
		List<String> actual = Utils.getHtmlTags("This <element> is an element");
		assertArrayEquals(new String[] {"element"}, actual.toArray(new String[] {}));
	}

	@Test
	public void whenHtmlTagWithAttributesThenTagFound() {
		List<String> actual = Utils.getHtmlTags("Link <a href=\"http://myDomain\">Title of link</a> is a link");
		assertArrayEquals(new String[] {"a"}, actual.toArray(new String[] {}));
	}

	@Test
	public void whenNoHtmlTagsThenEmptyList() {
		assertTrue(Utils.getHtmlTags("No tags").isEmpty());
	}

	@Test
	public void whenMultipleHtmlTagsThenAllFound() {
		List<String> actual = Utils.getHtmlTags("With <code>MyCode</code> and a <a href=\"http://myDomain\">Link</a>.");
		assertArrayEquals(new String[] {"code", "a"}, actual.toArray(new String[] {}));
	}
}