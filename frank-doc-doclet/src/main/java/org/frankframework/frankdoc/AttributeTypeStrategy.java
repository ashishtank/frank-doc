/* 
Copyright 2021, 2022 WeAreFrank! 

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

import static org.frankframework.frankdoc.DocWriterNew.ATTRIBUTE_VALUES_TYPE;
import static org.frankframework.frankdoc.DocWriterNew.VARIABLE_REFERENCE;
import static org.frankframework.frankdoc.DocWriterNewXmlUtils.XML_SCHEMA_URI;
import static org.frankframework.frankdoc.DocWriterNewXmlUtils.addDocumentation;
import static org.frankframework.frankdoc.DocWriterNewXmlUtils.addEnumeration;
import static org.frankframework.frankdoc.DocWriterNewXmlUtils.addPattern;
import static org.frankframework.frankdoc.DocWriterNewXmlUtils.addRestriction;
import static org.frankframework.frankdoc.DocWriterNewXmlUtils.addSimpleType;
import static org.frankframework.frankdoc.DocWriterNewXmlUtils.addUnion;
import static org.frankframework.frankdoc.DocWriterNewXmlUtils.createAttributeWithType;
import static org.frankframework.frankdoc.DocWriterNewXmlUtils.createSimpleType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.Logger;
import org.frankframework.frankdoc.model.AttributeEnum;
import org.frankframework.frankdoc.model.EnumValue;
import org.frankframework.frankdoc.model.AttributeType;
import org.frankframework.frankdoc.model.FrankAttribute;
import org.frankframework.frankdoc.util.LogUtil;
import org.frankframework.frankdoc.util.XmlBuilder;

public enum AttributeTypeStrategy {
	// Also excludes deprecated enum values
	ALLOW_PROPERTY_REF(new DelegateAllowPropertyRefEnumDocumentedCaseSensitive()),

	// Also includes deprecated enum values
	ALLOW_PROPERTY_REF_ENUM_VALUES_IGNORE_CASE(new DelegateAllowPropertyRefEnumIgnoreCase());

	private static Logger log = LogUtil.getLogger(AttributeTypeStrategy.class);

	static final String ATTRIBUTE_ACTIVE_NAME = "active";

	// The $-sign is not escaped in the regex below. This way,
	// the regexes in the XSDs are not flagged by XMLSpy.
	private static final String PATTERN_REF = "$\\{[^\\}]+\\}";

	private static final String FRANK_BOOLEAN = "frankBoolean";
	private static final String FRANK_INT = "frankInt";
	private static final String PATTERN_FRANK_BOOLEAN = String.format("(true|false)|(%s)", PATTERN_REF);
	private static final String PATTERN_FRANK_INT = String.format("((\\+|-)?[0-9]+)|(%s)", PATTERN_REF);

	private final Delegate delegate;

	private AttributeTypeStrategy(final Delegate delegate) {
		this.delegate = delegate;
	}

	XmlBuilder createAttribute(String name, AttributeType modelAttributeType) {
		return delegate.createAttribute(name, modelAttributeType);
	}

	XmlBuilder createRestrictedAttribute(FrankAttribute attribute, Consumer<XmlBuilder> documenter) {
		return delegate.createRestrictedAttribute(attribute, documenter);
	}

	static XmlBuilder createAttributeActive() {
		return DocWriterNewXmlUtils.createAttributeRef(ATTRIBUTE_ACTIVE_NAME);
	}

	List<XmlBuilder> createHelperTypes() {
		return delegate.createHelperTypes();
	}

	XmlBuilder createAttributeEnumType(AttributeEnum attributeEnum) {
		return delegate.createAttributeEnumType(attributeEnum);
	}

	private static abstract class Delegate {
		// This method ensures that references are still allowed for integer and boolean attributes.
		// For example, an integer attribute can still be set like "${someIdentifier}".
		// This method expects that methods DocWriterNewXmlUtils.createTypeFrankBoolean() and
		// DocWriterNewXmlUtils.createTypeFrankInteger() are used to define the referenced XSD types.
		XmlBuilder createAttribute(String name, AttributeType modelAttributeType) {
			return createAttribute(name, modelAttributeType, FRANK_BOOLEAN, FRANK_INT);
		}

		private final XmlBuilder createAttribute(String name, AttributeType modelAttributeType,
				String boolType, String intType) {
			XmlBuilder attribute = createAttributeWithType(name);
			String typeName = null;
			switch(modelAttributeType) {
			case BOOL:
				typeName = boolType;
				break;
			case INT:
				typeName = intType;
				break;
			case STRING:
				typeName = "xs:string";
				break;
			}
			attribute.addAttribute("type", typeName);
			return attribute;						
		}

		final XmlBuilder createRestrictedAttribute(FrankAttribute attribute, Consumer<XmlBuilder> documenter) {
			AttributeEnum attributeEnum = attribute.getAttributeEnum();
			XmlBuilder attributeBuilder = createAttributeWithType(attribute.getName());
			documenter.accept(attributeBuilder);
			XmlBuilder simpleType = addSimpleType(attributeBuilder);
			addUnion(simpleType, attributeEnum.getUniqueName(ATTRIBUTE_VALUES_TYPE), VARIABLE_REFERENCE);
			return attributeBuilder;
		}

		final XmlBuilder createAttributeEnumType(AttributeEnum attributeEnum) {
			XmlBuilder simpleType = createSimpleType(attributeEnum.getUniqueName(ATTRIBUTE_VALUES_TYPE));
			final XmlBuilder restriction = addRestriction(simpleType, "xs:string");
			attributeEnum.getValues().forEach(v -> addEnumValue(restriction, v));
			return simpleType;
		}

		abstract void addEnumValue(XmlBuilder restriction, EnumValue v);

		final List<XmlBuilder> createHelperTypes() {
			log.trace("Adding helper types for boolean and integer attributes, allowing ${...} references");
			List<XmlBuilder> result = new ArrayList<>();
			result.add(createTypeFrankBoolean());
			result.add(createTypeFrankInteger());
			result.add(createAttributeForAttributeActive());
			// Helper type for allowing a variable reference instead of an enum value
			result.add(createTypeVariableReference(VARIABLE_REFERENCE));
			return result;
		}

		private static XmlBuilder createTypeFrankBoolean() {
			return createStringRestriction(FRANK_BOOLEAN, PATTERN_FRANK_BOOLEAN);
		}

		private static XmlBuilder createTypeFrankInteger() {
			return createStringRestriction(FRANK_INT, PATTERN_FRANK_INT);
		}

		private static XmlBuilder createTypeVariableReference(String name) {
			return createStringRestriction(name, PATTERN_REF);
		}

		private static XmlBuilder createStringRestriction(String name, String pattern) {
			XmlBuilder simpleType = new XmlBuilder("simpleType", "xs", XML_SCHEMA_URI);
			simpleType.addAttribute("name", name);
			XmlBuilder restriction = new XmlBuilder("restriction", "xs", XML_SCHEMA_URI);
			simpleType.addSubElement(restriction);
			restriction.addAttribute("base", "xs:string");
			addPattern(restriction, pattern);
			return simpleType;
		}

		private XmlBuilder createAttributeForAttributeActive() {
			XmlBuilder attribute = new XmlBuilder("attribute", "xs", XML_SCHEMA_URI);
			attribute.addAttribute("name", ATTRIBUTE_ACTIVE_NAME);
			DocWriterNewXmlUtils.addDocumentation(attribute, "If defined and empty or false, then this element and all its children are ignored");
			XmlBuilder simpleType = DocWriterNewXmlUtils.addSimpleType(attribute);
			XmlBuilder restriction = DocWriterNewXmlUtils.addRestriction(simpleType, "xs:string");
			DocWriterNewXmlUtils.addPattern(restriction, getPattern());
			return attribute;
		}

		private String getPattern() {
			return "!?" + "(" + getPatternThatMightBeNegated() + ")";
		}

		private String getPatternThatMightBeNegated() {
			String patternTrue = getCaseInsensitivePattern(Boolean.valueOf(true).toString());
			String patternFalse = getCaseInsensitivePattern(Boolean.valueOf(false).toString());
			return Arrays.asList(PATTERN_REF, patternTrue, patternFalse).stream()
					.map(s -> "(" + s + ")")
					.collect(Collectors.joining("|"));
		}

		final String getCaseInsensitivePattern(final String word) {
			return IntStream.range(0, word.length()).mapToObj(i -> Character.valueOf(word.charAt(i)))
				.map(c -> "[" + Character.toLowerCase(c) + Character.toUpperCase(c) + "]")
				.collect(Collectors.joining(""));
		}
	}

	private static class DelegateAllowPropertyRefEnumDocumentedCaseSensitive extends Delegate {
		@Override
		void addEnumValue(XmlBuilder restriction, EnumValue v) {
			if(! v.isDeprecated()) {
				XmlBuilder valueBuilder = addEnumeration(restriction, v.getLabel());
				if(v.getDescription() != null) {
					addDocumentation(valueBuilder, v.getDescription());
				}
			}
		}
	}

	private static class DelegateAllowPropertyRefEnumIgnoreCase extends Delegate {
		@Override
		void addEnumValue(XmlBuilder restriction, EnumValue v) {
			addPattern(restriction, getCaseInsensitivePattern(v.getLabel()));
		}
	}
}
