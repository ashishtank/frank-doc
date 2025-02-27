package org.frankframework.frankdoc.wrapper;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class AnnotationsOfAnnotationTest {
	static final String PACKAGE = "org.frankframework.frankdoc.testtarget.wrapper.annotations.of.annotations.";

	FrankAnnotation instance;

	@Before
	public void setUp() throws Exception {
		FrankClassRepository classRepository = TestUtil.getFrankClassRepositoryDoclet(PACKAGE);
		FrankClass clazz = classRepository.findClass(PACKAGE + "Target");
		FrankMethod method = Arrays.asList(clazz.getDeclaredMethods()).get(0);
		instance = method.getAnnotation(PACKAGE + "Annotation");
	}

	@Test
	public void testStringValue() throws Exception {
		assertEquals("myString", instance.getValueOf("stringValue"));
	}

	@Test
	public void testIntValue() throws Exception {
		assertEquals(5, instance.getValueOf("intValue"));
	}

	@Test
	public void testStringArrayValue() throws Exception {
		assertArrayEquals(new String[] {"value 1", "value 2"}, (String[]) instance.getValueOf("stringArrayValue"));
	}

	@Test
	public void testDefaultValue() throws Exception {
		assertEquals("theDefault", instance.getValueOf("valueWithDefault"));
	}

	@Test
	public void testBoolValue() throws Exception {
		assertTrue((Boolean) instance.getValueOf("boolValue"));
	}

	@Test
	public void testEnumValue() throws Exception {
		FrankEnumConstant enumValue = (FrankEnumConstant) instance.getValueOf("myEnumField");
		assertEquals("SECOND", enumValue.getName());
		assertEquals(1, enumValue.getPosition());
	}
}
