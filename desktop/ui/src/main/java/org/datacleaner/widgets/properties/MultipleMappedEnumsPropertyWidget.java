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
package org.datacleaner.widgets.properties;

import java.awt.BorderLayout;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.JComponent;
import javax.swing.ListCellRenderer;

import org.apache.metamodel.util.LazyRef;
import org.datacleaner.api.InputColumn;
import org.datacleaner.descriptors.ConfiguredPropertyDescriptor;
import org.datacleaner.job.builder.ComponentBuilder;
import org.datacleaner.panels.DCPanel;
import org.datacleaner.util.DefaultEnumMatcher;
import org.datacleaner.util.EnumMatcher;
import org.datacleaner.widgets.DCCheckBox;
import org.datacleaner.widgets.DCComboBox;
import org.datacleaner.widgets.DCComboBox.Listener;
import org.datacleaner.widgets.EnumComboBoxListRenderer;

/**
 * A specialized property widget for multiple input columns that are mapped to
 * enum values. This widget looks like the
 * {@link MultipleInputColumnsPropertyWidget}, but is enhanced with enum combo
 * boxes.
 */
public class MultipleMappedEnumsPropertyWidget<E extends Enum<?>> extends MultipleInputColumnsPropertyWidget {

    public class MappedEnumsPropertyWidget extends MinimalPropertyWidget<E[]> {

        public MappedEnumsPropertyWidget(ComponentBuilder componentBuilder,
                ConfiguredPropertyDescriptor propertyDescriptor) {
            super(componentBuilder, propertyDescriptor);
        }

        @Override
        public JComponent getWidget() {
            // do not return a visual widget
            return null;
        }

        @Override
        public boolean isSet() {
            return MultipleMappedEnumsPropertyWidget.this.isSet();
        }

        @Override
        public E[] getValue() {
            return getMappedEnums();
        }

        @Override
        protected void setValue(final E[] value) {
            if (MultipleMappedEnumsPropertyWidget.this.isUpdating()) {
                return;
            }
            setMappedEnums(value);
        }
    }

    private final Map<InputColumn<?>, DCComboBox<E>> _mappedEnumComboBoxes;
    private final ConfiguredPropertyDescriptor _mappedEnumsProperty;
    private final MappedEnumsPropertyWidget _mappedEnumsPropertyWidget;
    private final LazyRef<EnumMatcher<E>> _enumMatcherRef;

    /**
     * Constructs the property widget
     * 
     * @param componentBuilder
     *            the component builder containing the properties
     * @param inputColumnsProperty
     *            the property represeting the columns to use for settig up
     *            conditional lookup (InputColumn[])
     * @param mappedEnumsProperty
     *            the property representing the mapped enums
     */
    public MultipleMappedEnumsPropertyWidget(ComponentBuilder componentBuilder,
            ConfiguredPropertyDescriptor inputColumnsProperty, ConfiguredPropertyDescriptor mappedEnumsProperty) {
        super(componentBuilder, inputColumnsProperty);
        _mappedEnumComboBoxes = new WeakHashMap<>();
        _mappedEnumsProperty = mappedEnumsProperty;
        _enumMatcherRef = new LazyRef<EnumMatcher<E>>() {
            @Override
            protected EnumMatcher<E> fetch() throws Throwable {
                return createEnumMatcher();
            }
        };
        _mappedEnumsPropertyWidget = new MappedEnumsPropertyWidget(componentBuilder, mappedEnumsProperty);

        final InputColumn<?>[] currentValue = getCurrentValue();

        @SuppressWarnings("unchecked")
        final E[] currentMappedEnums = (E[]) componentBuilder.getConfiguredProperty(mappedEnumsProperty);
        if (currentValue != null && currentMappedEnums != null) {
            final int minLength = Math.min(currentValue.length, currentMappedEnums.length);
            for (int i = 0; i < minLength; i++) {
                final InputColumn<?> inputColumn = currentValue[i];
                final E mappedEnum = currentMappedEnums[i];
                createComboBox(inputColumn, mappedEnum);
            }
        }

        if (currentValue != null) {
            // Ticket #945 - this must happen AFTER creating the combo boxes
            // (above)
            setValue(currentValue);
        }
    }

    public ConfiguredPropertyDescriptor getMappedEnumsProperty() {
        return _mappedEnumsProperty;
    }

    public void setMappedEnums(E[] value) {
        final List<InputColumn<?>> inputColumns = MultipleMappedEnumsPropertyWidget.this.getSelectedInputColumns();

        for (int i = 0; i < inputColumns.size(); i++) {
            final InputColumn<?> inputColumn = inputColumns.get(i);
            final E mappedEnum;
            if (value == null) {
                mappedEnum = null;
            } else if (i < value.length) {
                mappedEnum = value[i];
            } else {
                mappedEnum = null;
            }
            final DCComboBox<E> comboBox = _mappedEnumComboBoxes.get(inputColumn);
            if (mappedEnum != null) {
                comboBox.setVisible(true);
                comboBox.setEditable(true);
                comboBox.setSelectedItem(mappedEnum);
                comboBox.setEditable(false);
            }
        }
    }

    /**
     * Produces a list of available enum values for a particular input column.
     * 
     * @param inputColumn
     * @param mappedEnumsProperty
     * @return
     */
    protected E[] getEnumConstants(InputColumn<?> inputColumn, ConfiguredPropertyDescriptor mappedEnumsProperty) {
        final E[] enumConstants = getEnumClass().getEnumConstants();
        return enumConstants;
    }

    @Override
    protected boolean isAllInputColumnsSelectedIfNoValueExist() {
        return false;
    }

    /**
     * Creates a combobox for a particular input column.
     * 
     * @param inputColumn
     * @param mappedEnum
     * @return
     */
    protected DCComboBox<E> createComboBox(InputColumn<?> inputColumn, E mappedEnum) {
        if (mappedEnum == null && inputColumn != null) {
            mappedEnum = getSuggestedValue(inputColumn);
        }

        final E[] enumConstants = getEnumConstants(inputColumn, _mappedEnumsProperty);
        final DCComboBox<E> comboBox = new DCComboBox<E>(enumConstants);
        comboBox.setRenderer(getComboBoxRenderer(inputColumn, _mappedEnumComboBoxes, enumConstants));
        _mappedEnumComboBoxes.put(inputColumn, comboBox);
        if (mappedEnum != null) {
            comboBox.setEditable(true);
            comboBox.setSelectedItem(mappedEnum);
            comboBox.setEditable(false);
        }
        comboBox.addListener(new Listener<E>() {
            @Override
            public void onItemSelected(E item) {
                _mappedEnumsPropertyWidget.fireValueChanged();
            }
        });
        return comboBox;
    }

    /**
     * Gets the renderer of items in the comboboxes presenting the enum values.
     * 
     * @param enumConstants
     * @param mappedEnumComboBoxes
     * @param inputColumn
     * 
     * @return
     */
    protected ListCellRenderer<? super E> getComboBoxRenderer(InputColumn<?> inputColumn,
            Map<InputColumn<?>, DCComboBox<E>> mappedEnumComboBoxes, E[] enumConstants) {
        return new EnumComboBoxListRenderer();
    }

    /**
     * Gets the suggested/pre-filled value for a particular input column.
     * 
     * @param inputColumn
     * @return
     */
    protected E getSuggestedValue(InputColumn<?> inputColumn) {
        final EnumMatcher<E> matcher = _enumMatcherRef.get();
        if (matcher == null) {
            return null;
        }
        final E suggestion = matcher.suggestMatch(inputColumn.getName());
        return suggestion;
    }

    /**
     * Creates an {@link EnumMatcher} for use by the suggested enum mappings in
     * this widget
     * 
     * @return
     */
    protected EnumMatcher<E> createEnumMatcher() {
        final Class<E> enumClass = getEnumClass();
        return new DefaultEnumMatcher<E>(enumClass);
    }

    private Class<E> getEnumClass() {
        @SuppressWarnings("unchecked")
        final Class<E> baseType = (Class<E>) _mappedEnumsProperty.getBaseType();
        return baseType;
    }

    @Override
    protected JComponent decorateCheckBox(final DCCheckBox<InputColumn<?>> checkBox) {
        final DCComboBox<E> comboBox;
        final InputColumn<?> inputColumn = checkBox.getValue();
        if (_mappedEnumComboBoxes.containsKey(inputColumn)) {
            comboBox = _mappedEnumComboBoxes.get(inputColumn);
        } else {
            comboBox = createComboBox(inputColumn, null);
        }
        checkBox.addListener(new DCCheckBox.Listener<InputColumn<?>>() {
            @Override
            public void onItemSelected(InputColumn<?> item, boolean selected) {
                if (isBatchUpdating()) {
                    return;
                }
                comboBox.setVisible(selected);
                _mappedEnumsPropertyWidget.fireValueChanged();
            }
        });

        final boolean selected = checkBox.isSelected();
        comboBox.setVisible(selected);

        final DCPanel panel = new DCPanel();
        panel.setLayout(new BorderLayout());
        panel.add(checkBox, BorderLayout.CENTER);
        panel.add(comboBox, BorderLayout.EAST);
        return panel;
    }

    @Override
    protected void onValuesBatchSelected(List<InputColumn<?>> values) {
        final Collection<DCComboBox<E>> allComboBoxes = _mappedEnumComboBoxes.values();
        for (DCComboBox<E> comboBox : allComboBoxes) {
            comboBox.setVisible(false);
        }
        for (InputColumn<?> inputColumn : values) {
            final DCComboBox<E> comboBox = _mappedEnumComboBoxes.get(inputColumn);
            if (comboBox != null) {
                comboBox.setVisible(true);
            }
        }
    }

    public MappedEnumsPropertyWidget getMappedEnumsPropertyWidget() {
        return _mappedEnumsPropertyWidget;
    }

    private E[] getMappedEnums() {
        final InputColumn<?>[] inputColumns = MultipleMappedEnumsPropertyWidget.this.getValue();
        final List<E> result = new ArrayList<E>();
        for (final InputColumn<?> inputColumn : inputColumns) {
            final DCComboBox<E> comboBox = _mappedEnumComboBoxes.get(inputColumn);
            if (comboBox == null || !comboBox.isVisible()) {
                result.add(null);
            } else {
                final E value = comboBox.getSelectedItem();
                result.add(value);
            }
        }

        @SuppressWarnings("unchecked")
        final E[] array = (E[]) Array.newInstance(getEnumClass(), result.size());

        return result.toArray(array);
    }
    
    @Override
    protected void onBatchFinished() {
        super.onBatchFinished();
        _mappedEnumsPropertyWidget.fireValueChanged();
    }
}
