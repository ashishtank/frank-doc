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
package org.frankframework.frankdoc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import org.frankframework.frankdoc.wrapper.FrankClassRepository;
import org.frankframework.frankdoc.wrapper.TestUtil;

public class FrankDocModelGroupsTest {
	private FrankDocModel instance;
	
	@Test
	public void testGroups() throws IOException {
		String thePackage = "org.frankframework.frankdoc.testtarget.groups.";
		FrankClassRepository r = TestUtil.getFrankClassRepositoryDoclet(thePackage);
		instance = FrankDocModel.populate(TestUtil.resourceAsURL("doc/fake-group-digester-rules.xml"), thePackage + "Container", r);
		List<FrankDocGroup> groups = instance.getGroups();
		assertEquals(2, groups.size());
		FrankDocGroup current = groups.get(0);
		assertEquals("Listener", current.getName());
		List<ElementType> types = current.getElementTypes();
		assertEquals(1, types.size());
		assertEquals("IChild", types.get(0).getSimpleName());
		current = groups.get(1);
		assertEquals("Other", current.getName());
		types = current.getElementTypes();
		assertEquals(1, types.size());
		assertEquals("ISender", types.get(0).getSimpleName());
		// We test here that class DefaultSender is excluded from the type, because that would
		// produce a conflicting definition for XML tag "DefaultSender".
		List<FrankElement> frankElements = types.get(0).getSyntax2Members();
		assertEquals(1, frankElements.size());
		assertEquals("Default", frankElements.get(0).getSimpleName());
		assertTrue(frankElements.get(0).getXmlElementNames().contains("DefaultSender"));
		List<FrankElement> leftOvers = instance.getElementsOutsideConfigChildren();
		assertEquals(1, leftOvers.size());
		assertEquals("Container", leftOvers.get(0).getSimpleName());
	}

	@Test
	public void testFrankDocGroupAnnotationOnFrankElement() throws Exception {
		String thePackage = "org.frankframework.frankdoc.testtarget.groups.frankdocgroup.on.element.";
		FrankClassRepository r = TestUtil.getFrankClassRepositoryDoclet(thePackage);
		instance = FrankDocModel.populate(TestUtil.resourceAsURL("doc/fake-group-digester-rules.xml"), thePackage + "Container", r);
		List<FrankDocGroup> groups = instance.getGroups();
		assertEquals(4, groups.size());
		FrankDocGroup group = groups.get(0);
		assertEquals("Listeners", group.getName());
		assertEquals(10, group.getOrder());
		assertEquals(1, group.getElementTypes().size());
		ElementType elementType = group.getElementTypes().get(0);
		assertEquals("IListener", elementType.getSimpleName());
		List<FrankElement> members = elementType.getSyntax2Members();
		assertEquals(2, members.size());
		assertEquals("MyListener", members.get(0).getSimpleName());
		assertEquals("MySenderLikeListener", members.get(1).getSimpleName());
		group = groups.get(1);
		assertEquals("Senders", group.getName());
		assertEquals(20, group.getOrder());
		assertEquals(1, group.getElementTypes().size());
		elementType = group.getElementTypes().get(0);
		assertEquals("ISender", elementType.getSimpleName());
		members = elementType.getSyntax2Members();
		assertEquals(1, members.size());
		assertEquals("MySender", members.get(0).getSimpleName());
		group = groups.get(2);
		assertEquals("Adapters", group.getName());
		assertEquals(30, group.getOrder());
		assertEquals(1, group.getElementTypes().size());
		elementType = group.getElementTypes().get(0);
		assertEquals("Adapter", elementType.getSimpleName());
		members = elementType.getMembers();
		assertEquals(1, members.size());
		assertEquals("Adapter", members.get(0).getSimpleName());
		group = groups.get(3);
		assertEquals(FrankDocGroup.GROUP_NAME_OTHER, group.getName());
		assertEquals(0, group.getElementTypes().size());
		// FrankDocJsonFactory would use FrankDocModel.getElementsOutsideConfigChildren()
		// to build the group for the web application.
	}
}
