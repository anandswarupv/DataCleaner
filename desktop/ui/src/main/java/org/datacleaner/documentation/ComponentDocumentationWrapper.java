/**
 * DataCleaner (community edition)
 * Copyright (C) 2014 Neopost - Customer Information Management
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.datacleaner.documentation;

import java.util.EnumSet;
import java.util.Set;

import org.datacleaner.api.AnalyzerResult;
import org.datacleaner.api.ComponentCategory;
import org.datacleaner.api.Concurrent;
import org.datacleaner.api.ExternalDocumentation;
import org.datacleaner.api.HasAnalyzerResult;
import org.datacleaner.api.QueryOptimizedFilter;
import org.datacleaner.descriptors.AnalyzerDescriptor;
import org.datacleaner.descriptors.ComponentDescriptor;
import org.datacleaner.descriptors.FilterDescriptor;
import org.datacleaner.descriptors.HasAnalyzerResultComponentDescriptor;
import org.datacleaner.descriptors.MetricDescriptor;
import org.datacleaner.descriptors.SimpleHasAnalyzerResultComponentDescriptor;
import org.datacleaner.descriptors.TransformerDescriptor;
import org.datacleaner.util.ReflectionUtils;

/**
 * A wrapper around the {@link ComponentDescriptor} object to make it easier for
 * the documentation template to get to certain aspects that should be presented
 * in the documentation.
 */
public class ComponentDocumentationWrapper {

    private final ComponentDescriptor<?> _componentDescriptor;

    public ComponentDocumentationWrapper(ComponentDescriptor<?> componentDescriptor) {
        _componentDescriptor = componentDescriptor;
    }

    public String getName() {
        return _componentDescriptor.getDisplayName();
    }

    public String getDescription() {
        return DocumentationUtils.createHtmlParagraphs(_componentDescriptor.getDescription());
    }

    public String getSuperCategory() {
        return _componentDescriptor.getComponentSuperCategory().getName();
    }

    public String[] getCategories() {
        final Set<ComponentCategory> componentCategories = _componentDescriptor.getComponentCategories();
        final ComponentCategory[] array = componentCategories
                .toArray(new ComponentCategory[componentCategories.size()]);
        final String[] result = new String[componentCategories.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = array[i].getName();
        }
        return result;
    }

    public boolean isQueryOptimizable() {
        return ReflectionUtils.is(_componentDescriptor.getComponentClass(), QueryOptimizedFilter.class);
    }

    public ExternalDocumentation.DocumentationLink[] getDocumentationLinks() {
        final ExternalDocumentation externalDocumentation = _componentDescriptor
                .getAnnotation(ExternalDocumentation.class);
        if (externalDocumentation == null) {
            return new ExternalDocumentation.DocumentationLink[0];
        }
        return externalDocumentation.value();
    }

    public String[] getAliases() {
        return _componentDescriptor.getAliases();
    }

    public boolean isDistributable() {
        return _componentDescriptor.isDistributable();
    }

    public boolean isConcurrent() {
        final Concurrent annotation = _componentDescriptor.getAnnotation(Concurrent.class);
        if (annotation != null) {
            return annotation.value();
        }
        if (isAnalyzer()) {
            return false;
        }
        return true;
    }

    public boolean isAnalyzer() {
        return _componentDescriptor instanceof AnalyzerDescriptor;
    }

    public boolean isTransformer() {
        return _componentDescriptor instanceof TransformerDescriptor;
    }

    public boolean isFilter() {
        return _componentDescriptor instanceof FilterDescriptor;
    }

    public boolean isResultProducer() {
        return ReflectionUtils.is(_componentDescriptor.getComponentClass(), HasAnalyzerResult.class);
    }

    public String getResultType() {
        final HasAnalyzerResultComponentDescriptor<?> descriptor = getHasAnalyzerResultComponentDescriptor();
        final Class<? extends AnalyzerResult> resultClass = descriptor.getResultClass();
        return resultClass.getSimpleName();
    }

    public MetricDocumentationWrapper[] getMetrics() {
        final HasAnalyzerResultComponentDescriptor<?> descriptor = getHasAnalyzerResultComponentDescriptor();
        final Set<MetricDescriptor> metrics = descriptor.getResultMetrics();
        MetricDocumentationWrapper[] result = new MetricDocumentationWrapper[metrics.size()];
        int i = 0;
        for (MetricDescriptor metricDescriptor : metrics) {
            result[i] = new MetricDocumentationWrapper(metricDescriptor);
            i++;
        }
        return result;
    }

    private HasAnalyzerResultComponentDescriptor<?> getHasAnalyzerResultComponentDescriptor() {
        if (_componentDescriptor instanceof HasAnalyzerResultComponentDescriptor) {
            return (HasAnalyzerResultComponentDescriptor<?>) _componentDescriptor;
        }
        @SuppressWarnings("unchecked")
        Class<? extends HasAnalyzerResult<?>> componentClass = (Class<? extends HasAnalyzerResult<?>>) _componentDescriptor
                .getComponentClass();
        return new SimpleHasAnalyzerResultComponentDescriptor<>(componentClass);
    }

    public FilterOutcomeDocumentationWrapper[] getFilterOutcomes() {
        if (_componentDescriptor instanceof FilterDescriptor) {
            final EnumSet<?> outcomes = ((FilterDescriptor<?, ?>) _componentDescriptor).getOutcomeCategories();
            final FilterOutcomeDocumentationWrapper[] result = new FilterOutcomeDocumentationWrapper[outcomes.size()];
            int i = 0;
            for (Enum<?> outcome : outcomes) {
                result[i] = new FilterOutcomeDocumentationWrapper(outcome);
                i++;
            }
            return result;
        }
        return new FilterOutcomeDocumentationWrapper[0];
    }
}
