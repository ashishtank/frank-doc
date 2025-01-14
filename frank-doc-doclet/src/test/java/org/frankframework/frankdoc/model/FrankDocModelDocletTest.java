package org.frankframework.frankdoc.model;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import org.frankframework.frankdoc.wrapper.FrankClassRepository;
import org.frankframework.frankdoc.wrapper.TestUtil;

public class FrankDocModelDocletTest {
	private static final String SIMPLE = "org.frankframework.frankdoc.testtarget.simple.";
	private static final String EXPECTED_DESCRIPTION =
			"The JavaDoc comment of class \"Container\".\n" +
			" \n" +
			" This is additional text that we do not add to the XSDs or the Frank!Doc website.";
	private static final String EXPECTED_DESCRIPTION_HEADER = "The JavaDoc comment of class \"Container\".";

	private FrankDocModel instance;

	@Before
	public void setUp() throws IOException {
		FrankClassRepository repository = TestUtil.getFrankClassRepositoryDoclet(SIMPLE);
		instance = FrankDocModel.populate(TestUtil.resourceAsURL("doc/xsd-element-name-digester-rules.xml"), SIMPLE + "Container", repository);
	}

	@Test
	public void whenClassHasJavadocThenInFrankElementDescription() {
		FrankElement frankElement = instance.findFrankElement(SIMPLE + "Container");
		assertEquals(EXPECTED_DESCRIPTION, frankElement.getDescription());
		assertEquals(EXPECTED_DESCRIPTION_HEADER, frankElement.getDescriptionHeader());
	}
}
