package com.lewisd.maven.lint.rules.basic;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.maven.model.InputLocation;
import org.apache.maven.project.MavenProject;
import org.springframework.beans.factory.annotation.Autowired;

import com.lewisd.maven.lint.ResultCollector;
import com.lewisd.maven.lint.rules.AbstractRule;
import com.lewisd.maven.lint.util.ExpressionEvaluator;
import com.lewisd.maven.lint.util.ModelUtil;

public class GroupArtifactVersionMustBeInCorrectOrderRule extends AbstractRule {
	
	private static final List<Object> sortOrder = new LinkedList<Object>();
	
	static {
		sortOrder.add("");
		sortOrder.add("modelVersion"); // for <project>
		sortOrder.add("extensions"); // for <plugin>
		sortOrder.add("groupId");
		sortOrder.add("artifactId");
		sortOrder.add("classifier");
		sortOrder.add("type"); // for <dependencies>
		sortOrder.add("version");
		sortOrder.add("scope"); // for <dependencies>
		
		// We don't enforce the order of <configuration>, <exclusions>, <executions>, <dependencies>, and any other
		// unlisted elements, other than they must be after the ones we do enforce.
	}
	
	@Autowired
	public GroupArtifactVersionMustBeInCorrectOrderRule(final ExpressionEvaluator expressionEvaluator, final ModelUtil modelUtil) {
		super(expressionEvaluator, modelUtil);
	}

	@Override
	protected void addRequiredModels(final Set<String> requiredModels) {
	}

	@Override
	public String getIdentifier() {
		return "GAVORDER";
	}

	public void invoke(final MavenProject mavenProject, final Map<String, Object> models, final ResultCollector resultCollector) {
		
		final Collection<Object> objectsToCheck = modelUtil.findGAVObjects(mavenProject);
		
		for (final Object object: objectsToCheck) {
			final Map<Object, InputLocation> locations = modelUtil.getLocations(object);

			// Sort the locations by their location in the file
			final SortedMap<InputLocation, Object> actualOrderedElements = new TreeMap<InputLocation, Object>(new InputLocationMapValueComparator());
			for(final Map.Entry<Object, InputLocation> entry : locations.entrySet()) {
				actualOrderedElements.put(entry.getValue(), entry.getKey());
			}
			

			final List<Object> expectedOrderElements = new LinkedList<Object>();
			// First, find the elements that do exist, for which we care about the order
			for (final Object location : sortOrder) {
				if (locations.containsKey(location)) {
					expectedOrderElements.add(location);
				}
			}
			// Next, find the remaining elements, for which we don't care about the order,
			// so add them in the order they actually are.
			for (final Object location : actualOrderedElements.values()) {
				if (!expectedOrderElements.contains(location)) {
					expectedOrderElements.add(location);
				}
			}
			
			// Don't expect any elements that aren't actually there
			expectedOrderElements.retainAll(locations.keySet());
			

			final Iterator<Object> expectedOrderElementIterator = expectedOrderElements.iterator();
			final Iterator<Object> actualOrderedElementsIterator = actualOrderedElements.values().iterator();
			
			while (expectedOrderElementIterator.hasNext() && actualOrderedElementsIterator.hasNext()) {
				final Object expectedElement = expectedOrderElementIterator.next();
				final Object actualElement = actualOrderedElementsIterator.next();
				if (!expectedElement.equals(actualElement)) {
					resultCollector.addViolation(mavenProject, this, "Found '" + actualElement + "' but was expecting '" + expectedElement + "'", locations.get(actualElement));
					break;
				}
			}
			
			
		}
	}
	
	private class InputLocationMapValueComparator implements Comparator<InputLocation> {

		@Override
		public int compare(final InputLocation a, final InputLocation b) {
			if (a.getLineNumber() == b.getLineNumber()) {
				return a.getColumnNumber() - b.getColumnNumber();
			} else {
				return a.getLineNumber() - b.getLineNumber();
			}
		}
		
	}
	
}
