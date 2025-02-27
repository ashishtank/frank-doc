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

package org.frankframework.frankdoc.wrapper;

import java.util.function.Function;

public interface FrankMethod extends FrankProgramElement {
	boolean isMultiplyInheritedPlaceholder();
	String getSignature();
	FrankAnnotation[] getAnnotations();
	FrankAnnotation getAnnotation(String name);
	FrankClass getDeclaringClass();
	/**
	 * If the return type is void, return a {@link FrankPrimitiveType} wrapping "void".
	 */
	FrankType getReturnType();
	int getParameterCount();
	FrankType[] getParameterTypes();
	boolean isVarargs();
	FrankAnnotation getAnnotationIncludingInherited(String name) throws FrankDocException;
	String getJavaDoc();
	String getJavaDocIncludingInherited() throws FrankDocException;
	String getJavaDocTag(String tagName);
	String getJavaDocTagIncludingInherited(String tagName) throws FrankDocException;
	void browseAncestorsUntilTrue(Function<FrankMethod, Boolean> handler) throws FrankDocException;

	default String toStringImpl() {
		return String.format("%s.%s", getDeclaringClass().getSimpleName(), getName());
	}
}
