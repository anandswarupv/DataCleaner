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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.swing.JButton;

import org.datacleaner.descriptors.ConfiguredPropertyDescriptor;
import org.datacleaner.job.builder.ComponentBuilder;
import org.datacleaner.reference.Dictionary;
import org.datacleaner.panels.DCPanel;
import org.datacleaner.user.DictionaryChangeListener;
import org.datacleaner.user.MutableReferenceDataCatalog;
import org.datacleaner.util.IconUtils;
import org.datacleaner.util.WidgetFactory;
import org.datacleaner.widgets.DCComboBox;
import org.datacleaner.widgets.DCComboBox.Listener;
import org.datacleaner.widgets.ReferenceDataComboBoxListRenderer;
import org.datacleaner.windows.ReferenceDataDialog;
import org.jdesktop.swingx.HorizontalLayout;

public class SingleDictionaryPropertyWidget extends AbstractPropertyWidget<Dictionary> implements
        DictionaryChangeListener {

    private final DCComboBox<Dictionary> _comboBox;
    private final MutableReferenceDataCatalog _referenceDataCatalog;
    private final Provider<ReferenceDataDialog> _referenceDataDialogProvider;

    @Inject
    public SingleDictionaryPropertyWidget(ConfiguredPropertyDescriptor propertyDescriptor,
            ComponentBuilder componentBuilder, MutableReferenceDataCatalog referenceDataCatalog,
            Provider<ReferenceDataDialog> referenceDataDialogProvider) {
        super(componentBuilder, propertyDescriptor);
        _referenceDataCatalog = referenceDataCatalog;
        _referenceDataDialogProvider = referenceDataDialogProvider;

        _comboBox = new DCComboBox<Dictionary>();
        _comboBox.setRenderer(new ReferenceDataComboBoxListRenderer());
        _comboBox.setEditable(false);

        if (!propertyDescriptor.isRequired()) {
            _comboBox.addItem(null);
        }

        final String[] dictionaryNames = referenceDataCatalog.getDictionaryNames();
        for (String name : dictionaryNames) {
            _comboBox.addItem(referenceDataCatalog.getDictionary(name));
        }

        Dictionary currentValue = getCurrentValue();
        _comboBox.setSelectedItem(currentValue);

        _comboBox.addListener(new Listener<Dictionary>() {
            @Override
            public void onItemSelected(Dictionary item) {
                fireValueChanged();
            }
        });

        final JButton dialogButton = WidgetFactory.createSmallButton(IconUtils.MENU_OPTIONS);
        dialogButton.setToolTipText("Configure dictionaries");
        dialogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ReferenceDataDialog dialog = _referenceDataDialogProvider.get();
                dialog.selectDictionariesTab();
                dialog.setVisible(true);
            }
        });

        final DCPanel outerPanel = new DCPanel();
        outerPanel.setLayout(new HorizontalLayout(2));
        outerPanel.add(_comboBox);
        outerPanel.add(dialogButton);

        add(outerPanel);
    }

    @Override
    public void onPanelAdd() {
        super.onPanelAdd();
        _referenceDataCatalog.addDictionaryListener(this);
    }

    @Override
    public void onPanelRemove() {
        super.onPanelRemove();
        _referenceDataCatalog.removeDictionaryListener(this);
    }

    @Override
    public Dictionary getValue() {
        return (Dictionary) _comboBox.getSelectedItem();
    }

    @Override
    protected void setValue(Dictionary value) {
        _comboBox.setEditable(true);
        _comboBox.setSelectedItem(value);
        _comboBox.setEditable(false);
    }

    @Override
    public void onAdd(Dictionary dictionary) {
        _comboBox.addItem(dictionary);
    }

    @Override
    public void onRemove(Dictionary dictionary) {
        _comboBox.removeItem(dictionary);
    }
}
