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
package org.datacleaner.panels.maxrows;

import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;

import org.datacleaner.components.maxrows.MaxRowsFilter;
import org.datacleaner.descriptors.ConfiguredPropertyDescriptor;
import org.datacleaner.job.builder.AnalysisJobBuilder;
import org.datacleaner.job.builder.FilterComponentBuilder;
import org.datacleaner.util.StringUtils;
import org.datacleaner.panels.DCPanel;
import org.datacleaner.util.DCDocumentListener;
import org.datacleaner.util.IconUtils;
import org.datacleaner.util.ImageManager;
import org.datacleaner.util.NumberDocument;
import org.datacleaner.util.WidgetFactory;
import org.datacleaner.util.WidgetUtils;
import org.datacleaner.widgets.DCCheckBox;
import org.datacleaner.widgets.DCCheckBox.Listener;
import org.datacleaner.widgets.DCLabel;
import org.jdesktop.swingx.JXTextField;

/**
 * A panel with a simple checkbox and textfield to enable a
 * {@link MaxRowsFilter}.
 * 
 * @author Kasper Sørensen
 */
public class MaxRowsFilterShortcutPanel extends DCPanel {

    private static final long serialVersionUID = 1L;

    public static final String MAX_ROWS_PROPERTY_NAME = "Max rows";
    public static final String FILTER_NAME = "Limit analysis (Max rows)";

    private static final String DEFAULT_MAX_ROWS = "1000";

    private final AnalysisJobBuilder _analysisJobBuilder;
    private final JXTextField _textField;
    private final DCCheckBox<Object> _checkBox;
    private final DCLabel _suffixLabel;
    private final DCLabel _prefixLabel;
    private FilterComponentBuilder<MaxRowsFilter, MaxRowsFilter.Category> _maxRowsFilterComponentBuilder;

    /**
     * Checks if the given filter is in fact the filter specified by this
     * shortcut panel.
     * 
     * @param filterJobBuilder
     * @return
     */
    public static boolean isFilter(FilterComponentBuilder<?, ?> filterJobBuilder) {
        return FILTER_NAME.equals(filterJobBuilder.getName())
                && filterJobBuilder.getDescriptor().getComponentClass() == MaxRowsFilter.class;
    }

    public MaxRowsFilterShortcutPanel(AnalysisJobBuilder analysisJobBuilder) {
        this(analysisJobBuilder, null);
    }

    public MaxRowsFilterShortcutPanel(AnalysisJobBuilder analysisJobBuilder,
            FilterComponentBuilder<?, ?> filterJobBuilder) {
        super();
        _analysisJobBuilder = analysisJobBuilder;
        _checkBox = new DCCheckBox<Object>("", false);
        _textField = WidgetFactory.createTextField();
        _textField.setEnabled(false);
        _textField.setColumns(4);
        _textField.setDocument(new NumberDocument());
        if (filterJobBuilder != null) {
            ConfiguredPropertyDescriptor propertyDescriptor = filterJobBuilder.getDescriptor().getConfiguredProperty(
                    MAX_ROWS_PROPERTY_NAME);
            Object value = filterJobBuilder.getConfiguredProperty(propertyDescriptor);
            if (value != null) {
                _textField.setText(value.toString());
            } else {
                _textField.setText(DEFAULT_MAX_ROWS);
            }
        } else {
            _textField.setText(DEFAULT_MAX_ROWS);
        }
        _prefixLabel = DCLabel.dark("Limit analysis to max ");
        _prefixLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                _checkBox.doClick();
            }
        });
        _prefixLabel.setEnabled(false);
        _suffixLabel = DCLabel.dark(" rows.");
        _suffixLabel.setEnabled(false);

        if (filterJobBuilder != null) {
            @SuppressWarnings("unchecked")
            FilterComponentBuilder<MaxRowsFilter, MaxRowsFilter.Category> maxRowFilterComponentBuilder = (FilterComponentBuilder<MaxRowsFilter, MaxRowsFilter.Category>) filterJobBuilder;
            _maxRowsFilterComponentBuilder = maxRowFilterComponentBuilder;
            _checkBox.setSelected(true);
        }

        _checkBox.addListener(new Listener<Object>() {
            @Override
            public void onItemSelected(Object item, boolean selected) {
                FilterComponentBuilder<MaxRowsFilter, MaxRowsFilter.Category> maxRowsFilterComponentBuilder = getJobBuilder();

                if (selected) {
                    _analysisJobBuilder.addFilter(maxRowsFilterComponentBuilder);
                    _analysisJobBuilder.setDefaultRequirement(maxRowsFilterComponentBuilder,
                            MaxRowsFilter.Category.VALID);
                } else {
                    _analysisJobBuilder.removeFilter(maxRowsFilterComponentBuilder);
                    _maxRowsFilterComponentBuilder = null;
                }
                updateLabels();
            }
        });

        _textField.getDocument().addDocumentListener(new DCDocumentListener() {
            @Override
            protected void onChange(DocumentEvent event) {
                final FilterComponentBuilder<MaxRowsFilter, MaxRowsFilter.Category> fjb = getJobBuilder();
                if (fjb != null) {
                    String text = _textField.getText();
                    if (!StringUtils.isNullOrEmpty(text)) {
                        try {
                            int maxRows = Integer.parseInt(text);
                            fjb.setConfiguredProperty(MAX_ROWS_PROPERTY_NAME, maxRows);
                        } catch (NumberFormatException e) {
                            WidgetUtils.showErrorMessage("Cannot read number",
                                    "The entered value could not be read as a number.", e);
                        }
                    }
                }
            }
        });

        setLayout(new FlowLayout(FlowLayout.LEFT));
        final String imagePath = IconUtils.getImagePathForClass(MaxRowsFilter.class);
        add(new JLabel(ImageManager.get().getImageIcon(imagePath)));
        add(_checkBox);
        add(_prefixLabel);
        add(_textField);
        add(_suffixLabel);

        updateLabels();

        setBorder(WidgetUtils.BORDER_LIST_ITEM);
    }

    public FilterComponentBuilder<MaxRowsFilter, MaxRowsFilter.Category> getJobBuilder() {
        if (_maxRowsFilterComponentBuilder == null) {
            // Lazy initializing getter (to postpone the call to
            // DCConfiguration.
            _maxRowsFilterComponentBuilder = new FilterComponentBuilder<MaxRowsFilter, MaxRowsFilter.Category>(
                    _analysisJobBuilder, _analysisJobBuilder.getConfiguration().getEnvironment()
                            .getDescriptorProvider().getFilterDescriptorForClass(MaxRowsFilter.class));
            _maxRowsFilterComponentBuilder.setName(FILTER_NAME);
        }
        return _maxRowsFilterComponentBuilder;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        _checkBox.setEnabled(enabled);
    }

    public void setFilterEnabled(boolean enabled) {
        if (enabled != isFilterEnabled()) {
            _checkBox.doClick();
        }
    }

    public boolean isFilterEnabled() {
        return _checkBox.isSelected();
    }

    private void updateLabels() {
        final boolean selected = _checkBox.isSelected();
        _prefixLabel.setEnabled(selected);
        _textField.setEnabled(selected);
        _suffixLabel.setEnabled(selected);
    }

    public void resetToDefault() {
        _checkBox.setSelected(false);
        _textField.setText(DEFAULT_MAX_ROWS);
    }
}
