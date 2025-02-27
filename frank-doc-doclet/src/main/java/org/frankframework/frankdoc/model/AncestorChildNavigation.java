/* 
Copyright 2020, 2021 WeAreFrank! 

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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.frankframework.frankdoc.model.ElementChild.AbstractKey;
import org.frankframework.frankdoc.util.LogUtil;

class AncestorChildNavigation<T extends ElementChild> {
	private static Logger log = LogUtil.getLogger(AncestorChildNavigation.class);

	private final CumulativeChildHandler<T> handler;
	private final Predicate<ElementChild> childSelector;
	private final Predicate<ElementChild> childRejector;
	private final Class<T> kind;
	private Predicate<FrankElement> noChildren;
	private FrankElement current;
	private Set<AbstractKey> selectedChildKeys;
	private Set<AbstractKey> selectedOrRejectedChildKeys;
	// The map value is the owner of the overridden method.
	private Map<AbstractKey, FrankElement> overridden;

	AncestorChildNavigation(CumulativeChildHandler<T> handler, Predicate<ElementChild> childSelector, Predicate<ElementChild> childRejector, Class<T> kind) {
		this.handler = handler;
		this.childSelector = childSelector;
		this.childRejector = childRejector;
		this.kind = kind;
		this.noChildren = el -> el.getChildrenOfKind(ElementChild.ALL, kind).stream()
				.noneMatch(childSelector.or(childRejector));
	}

	void run(FrankElement start) {
		log.trace("Enter for FrankElement [{}] and child kind [{}]", () -> start.toString(), () -> kind.toString());
		enter(start);
		overridden = new HashMap<>();
		addDeclaredGroupOrRepeatChildrenInXsd();
		while(current.getNextAncestorThatHasChildren(noChildren) != null) {
			enter(current.getNextAncestorThatHasChildren(noChildren));
			if(! getOverriddenChildren().stream().anyMatch(childSelector)) {
				safeAddCumulative();
				return;
			}
			addDeclaredGroupOrRepeatChildrenInXsd();
		}
		log.trace("Leave for FrankElement [{}] and child kind [{}]", () -> start.toString(), () -> kind.toString());
	}

	private void enter(FrankElement current) {
		this.current = current;
		selectedChildKeys = current.getChildrenOfKind(childSelector, kind).stream()
				.map(ElementChild::getKey).collect(Collectors.toSet());
		Set<AbstractKey> rejectedChildKeys = current.getChildrenOfKind(childRejector, kind).stream()
				.map(ElementChild::getKey).collect(Collectors.toSet());
		checkSelectedChildKeysAreNotRejected(rejectedChildKeys);
		selectedOrRejectedChildKeys = new HashSet<>(selectedChildKeys);
		selectedOrRejectedChildKeys.addAll(rejectedChildKeys);
		if(log.isTraceEnabled()) {
			logEnter();
		}
	}

	private void logEnter() {
		log.trace("current: [{}]", current.toString());
		log.trace("selectedChildKeys: [{}]", keysToString(selectedChildKeys));
		log.trace("selectedOrRejectedChildKeys: [{}]", keysToString(selectedOrRejectedChildKeys));
	}

	private static String keysToString(Collection<AbstractKey> keys) {
		return keys.stream().map(AbstractKey::toString).collect(Collectors.joining(", "));
	}

	private void checkSelectedChildKeysAreNotRejected(Set<AbstractKey> rejectedChildKeys) {
		Set<AbstractKey> offending = new HashSet<>(selectedChildKeys);
		offending.retainAll(rejectedChildKeys);
		if(! offending.isEmpty()) {
			String offendingString = offending.stream().map(AbstractKey::toString).collect(Collectors.joining(", "));
			throw new IllegalArgumentException(String.format("Children that are both selected and rejected are not supported: [%s]", offendingString));
		}
	}

	private void addDeclaredGroupOrRepeatChildrenInXsd() {
		if(selectedChildKeys.isEmpty()) {
			updateOverridden();
			return;
		}
		Set<AbstractKey> omit = new HashSet<>(selectedChildKeys);
		omit.retainAll(overridden.keySet());
		if(omit.isEmpty()) {
			handler.handleChildrenOf(current);
		}
		else {
			List<T> children = current.getChildrenOfKind(childSelector, kind).stream()
					.filter(c -> ! omit.contains(c.getKey()))
					.map(c -> (T) c)
					.collect(Collectors.toList());
			handleSelectedChildren(children);
		}
		updateOverridden();
	}

	private void handleSelectedChildren(List<T> children) {
		if(current.getNextAncestorThatHasChildren(noChildren) == null) {
			handler.handleSelectedChildrenOfTopLevel(children, current);
		} else {
			handler.handleSelectedChildren(children, current);
		}
	}

	private void updateOverridden() {
		for(ElementChild child: current.getChildrenOfKind(ElementChild.ALL, kind)) {
			boolean isRenewedOverride = selectedOrRejectedChildKeys.contains(child.getKey());
			boolean isExistingOverride = overridden.keySet().contains(child.getKey());
			if(isRenewedOverride || isExistingOverride) {
				ElementChild overriddenFrom = getSelectedOrRejectedChildAncestor(child);
				if(overriddenFrom == null) {
					overridden.remove(child.getKey());
				} else {
					overridden.put(child.getKey(), overriddenFrom.getOwningElement());
				}				
			}
		}
		if(log.isTraceEnabled()) {
			logOverriddenFrom();
		}
	}

	private void logOverriddenFrom() {
		Map<FrankElement, Set<AbstractKey>> overriddenByOwner = getOverriddenByOwner();
		log.trace("Below follows the contents of overridden, grouped by owning FrankElement");
		for(FrankElement element: overriddenByOwner.keySet()) {
			log.trace("  From [{}]: [{}]", element.toString(), keysToString(overriddenByOwner.get(element)));
		}
	}

	private Map<FrankElement, Set<AbstractKey>> getOverriddenByOwner() {
		return overridden.keySet().stream()
				.collect(Collectors.groupingBy(k -> overridden.get(k), Collectors.toSet()));
	}

	private ElementChild getSelectedOrRejectedChildAncestor(ElementChild child) {
		FrankElement candidateOwner = child.getOverriddenFrom();
		while(candidateOwner != null) {
			ElementChild candidate = candidateOwner.findElementChildMatch(child);
			if(candidate == null) {
				throw new IllegalStateException("Cannot happen. If this would happen, method getOverriddenFrom() would not work correctly.");
			} else if(childSelector.test(candidate) || childRejector.test(candidate)) {
				return candidate;
			}
			child = candidate;
			candidateOwner = child.getOverriddenFrom();
		}
		return null;
	}

	private List<ElementChild> getOverriddenChildren() {
		Map<FrankElement, Set<AbstractKey>> overriddenByOwner = getOverriddenByOwner();
		return overriddenByOwner.keySet().stream()
				.flatMap(frankElement -> frankElement.getChildrenOfKind(ElementChild.ALL, kind).stream()
						.filter(c -> overriddenByOwner.get(frankElement).contains(c.getKey())))
				.collect(Collectors.toList());
	}

	private void safeAddCumulative() {
		if(current.getNextAncestorThatHasChildren(noChildren) == null) {
			handler.handleChildrenOf(current);
		} else {
			handler.handleCumulativeChildrenOf(current);
		}
	}
}
