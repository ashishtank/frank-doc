package org.frankframework.frankdoc.wrapper;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

public class MethodPlaceholderTest {
	static final String PACKAGE = "org.frankframework.frankdoc.testtarget.wrapper.method.placeholder.";

	private FrankClass child;
	private final List<String> ancestorMethodOwningClassNames = new ArrayList<>();

	@Before
	public void setUp() throws Exception {
		FrankClassRepository classRepository = TestUtil.getFrankClassRepositoryDoclet(PACKAGE);
		child = classRepository.findClass(PACKAGE + "Child");
		ancestorMethodOwningClassNames.clear();
	}

	@Test
	public void pureInterfaceMethodProducesPlaceholder() throws Exception {
		FrankMethod method = getMethodByName(child, "pureInterfaceMethod");
		assertTrue(method.isMultiplyInheritedPlaceholder());
		method.browseAncestorsUntilTrue(this::acceptAncestorMethod);
		assertArrayEquals(new String[] {"Child", "Intf"}, ancestorMethodOwningClassNames.toArray(new String[] {}));
	}

	@Test
	public void reintroducedAncestorMethodProducesPlaceholderButBrowsesClassMethodBeforeInterfaceMethod() throws Exception {
		FrankMethod method = getMethodByName(child, "reintroducedMethod");
		assertTrue(method.isMultiplyInheritedPlaceholder());
		method.browseAncestorsUntilTrue(this::acceptAncestorMethod);
		assertArrayEquals(new String[] {"Child", "Parent", "Intf"}, ancestorMethodOwningClassNames.toArray(new String[] {}));
	}

	@Test
	public void childMethodAlsoInterfaceInheritedProducesOrdinaryMethod() throws Exception {
		FrankMethod method = getMethodByName(child, "childMethodAlsoInInterface");
		assertFalse(method.isMultiplyInheritedPlaceholder());
		method.browseAncestorsUntilTrue(this::acceptAncestorMethod);
		assertArrayEquals(new String[] {"Child", "Intf"}, ancestorMethodOwningClassNames.toArray(new String[] {}));
	}

	private FrankMethod getMethodByName(FrankClass c, String name) {
		return Arrays.asList(c.getDeclaredMethodsAndMultiplyInheritedPlaceholders()).stream()
				.filter(m -> m.getName().equals(name))
				.collect(Collectors.toList()).get(0);
	}

	private boolean acceptAncestorMethod(FrankMethod m) {
		ancestorMethodOwningClassNames.add(m.getDeclaringClass().getSimpleName());
		return false;
	}
}
