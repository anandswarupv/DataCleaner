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
package org.eobjects.datacleaner.panels;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfiguration;
import org.eobjects.analyzer.connection.AccessDatastore;
import org.eobjects.analyzer.connection.CouchDbDatastore;
import org.eobjects.analyzer.connection.CsvDatastore;
import org.eobjects.analyzer.connection.Datastore;
import org.eobjects.analyzer.connection.DatastoreConnection;
import org.eobjects.analyzer.connection.DbaseDatastore;
import org.eobjects.analyzer.connection.ExcelDatastore;
import org.eobjects.analyzer.connection.FixedWidthDatastore;
import org.eobjects.analyzer.connection.HBaseDatastore;
import org.eobjects.analyzer.connection.JdbcDatastore;
import org.eobjects.analyzer.connection.JsonDatastore;
import org.eobjects.analyzer.connection.MongoDbDatastore;
import org.eobjects.analyzer.connection.OdbDatastore;
import org.eobjects.analyzer.connection.SalesforceDatastore;
import org.eobjects.analyzer.connection.SasDatastore;
import org.eobjects.analyzer.connection.SugarCrmDatastore;
import org.eobjects.analyzer.connection.XmlDatastore;
import org.eobjects.analyzer.util.StringUtils;
import org.eobjects.datacleaner.actions.OpenAnalysisJobActionListener;
import org.eobjects.datacleaner.database.DatabaseDriverCatalog;
import org.eobjects.datacleaner.database.DatabaseDriverDescriptor;
import org.eobjects.datacleaner.guice.InjectorBuilder;
import org.eobjects.datacleaner.user.DatastoreChangeListener;
import org.eobjects.datacleaner.user.MutableDatastoreCatalog;
import org.eobjects.datacleaner.user.UserPreferences;
import org.eobjects.datacleaner.util.DCDocumentListener;
import org.eobjects.datacleaner.util.IconUtils;
import org.eobjects.datacleaner.util.ImageManager;
import org.eobjects.datacleaner.util.WidgetFactory;
import org.eobjects.datacleaner.util.WidgetUtils;
import org.eobjects.datacleaner.widgets.Alignment;
import org.eobjects.datacleaner.widgets.DCLabel;
import org.eobjects.datacleaner.widgets.DCPopupBubble;
import org.eobjects.datacleaner.widgets.OpenAnalysisJobMenuItem;
import org.eobjects.datacleaner.windows.AbstractDialog;
import org.eobjects.datacleaner.windows.AccessDatastoreDialog;
import org.eobjects.datacleaner.windows.AnalysisJobBuilderWindow;
import org.eobjects.datacleaner.windows.CompositeDatastoreDialog;
import org.eobjects.datacleaner.windows.CouchDbDatastoreDialog;
import org.eobjects.datacleaner.windows.CsvDatastoreDialog;
import org.eobjects.datacleaner.windows.DbaseDatastoreDialog;
import org.eobjects.datacleaner.windows.ExcelDatastoreDialog;
import org.eobjects.datacleaner.windows.FixedWidthDatastoreDialog;
import org.eobjects.datacleaner.windows.HBaseDatastoreDialog;
import org.eobjects.datacleaner.windows.JdbcDatastoreDialog;
import org.eobjects.datacleaner.windows.JsonDatastoreDialog;
import org.eobjects.datacleaner.windows.MongoDbDatastoreDialog;
import org.eobjects.datacleaner.windows.OdbDatastoreDialog;
import org.eobjects.datacleaner.windows.OptionsDialog;
import org.eobjects.datacleaner.windows.SalesforceDatastoreDialog;
import org.eobjects.datacleaner.windows.SasDatastoreDialog;
import org.eobjects.datacleaner.windows.SugarCrmDatastoreDialog;
import org.eobjects.datacleaner.windows.XmlDatastoreDialog;
import org.jdesktop.swingx.JXTextField;
import org.jdesktop.swingx.VerticalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

/**
 * Panel to select which job or datastore to use. Shown in the "source" tab, if
 * no datastore or job has been selected to begin with.
 */
public class WelcomePanel extends DCPanel implements DatastoreChangeListener {

    private static final long serialVersionUID = 1L;

    private Logger logger = LoggerFactory.getLogger(WelcomePanel.class);

    private static final ImageManager imageManager = ImageManager.get();

    private static final int MAX_JOB_PANELS = 3;

    private final MutableDatastoreCatalog _datastoreCatalog;
    private final AnalysisJobBuilderWindow _analysisJobBuilderWindow;
    private final Provider<OptionsDialog> _optionsDialogProvider;
    private final DatabaseDriverCatalog _databaseDriverCatalog;
    private final List<DatastorePanel> _datastorePanels;
    private final DCGlassPane _glassPane;
    private final JButton _analyzeButton;
    private final JButton _browseJobsButton;
    private final JButton _moreRecentJobsButton;
    private final DCPanel _datastoreListPanel;
    private final DCPanel _jobsListPanel;
    private final JXTextField _searchDatastoreTextField;
    private final InjectorBuilder _injectorBuilder;
    private final UserPreferences _userPreferences;
    private final AnalyzerBeansConfiguration _configuration;
    private final OpenAnalysisJobActionListener _openAnalysisJobActionListener;

    @Inject
    protected WelcomePanel(AnalyzerBeansConfiguration configuration, AnalysisJobBuilderWindow analysisJobBuilderWindow,
            DCGlassPane glassPane, Provider<OptionsDialog> optionsDialogProvider, InjectorBuilder injectorBuilder,
            OpenAnalysisJobActionListener openAnalysisJobActionListener, DatabaseDriverCatalog databaseDriverCatalog,
            UserPreferences userPreferences) {
        super();
        _openAnalysisJobActionListener = openAnalysisJobActionListener;
        _configuration = configuration;
        _datastorePanels = new ArrayList<DatastorePanel>();
        _datastoreCatalog = (MutableDatastoreCatalog) configuration.getDatastoreCatalog();
        _analysisJobBuilderWindow = analysisJobBuilderWindow;
        _glassPane = glassPane;
        _optionsDialogProvider = optionsDialogProvider;
        _injectorBuilder = injectorBuilder;
        _databaseDriverCatalog = databaseDriverCatalog;
        _userPreferences = userPreferences;

        _browseJobsButton = new JButton("Browse jobs", imageManager.getImageIcon(IconUtils.MENU_OPEN,
                IconUtils.ICON_SIZE_SMALL));
        _browseJobsButton.setMargin(new Insets(1, 1, 1, 4));
        _browseJobsButton.addActionListener(openAnalysisJobActionListener);

        // initialize "analyze" button
        _analyzeButton = new JButton("Build job", imageManager.getImageIcon(IconUtils.MODEL_JOB,
                IconUtils.ICON_SIZE_SMALL));
        _analyzeButton.setMargin(new Insets(1, 1, 1, 1));
        _analyzeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (DatastorePanel datastorePanel : _datastorePanels) {
                    if (datastorePanel.isSelected()) {
                        Datastore datastore = datastorePanel.getDatastore();

                        // open the connection here, to make any connection
                        // issues apparent early
                        try (DatastoreConnection datastoreConnection = datastore.openConnection()) {
                            datastoreConnection.getDataContext().getSchemaNames();
                            _analysisJobBuilderWindow.setDatastore(datastore);
                        }
                        return;
                    }
                }
            }
        });

        // initialize search text field
        _searchDatastoreTextField = WidgetFactory.createTextField("Search/filter datastores");
        _searchDatastoreTextField.setBorder(new CompoundBorder(new EmptyBorder(4, 0, 0, 0), WidgetUtils.BORDER_THIN));
        _searchDatastoreTextField.setOpaque(false);
        _searchDatastoreTextField.getDocument().addDocumentListener(new DCDocumentListener() {
            @Override
            protected void onChange(DocumentEvent event) {
                String text = _searchDatastoreTextField.getText();
                if (StringUtils.isNullOrEmpty(text)) {
                    // when there is no search query, set all datastores
                    // visible
                    for (DatastorePanel datastorePanel : _datastorePanels) {
                        datastorePanel.setVisible(true);
                    }
                } else {
                    // do a case insensitive search
                    text = text.trim().toLowerCase();
                    for (DatastorePanel datastorePanel : _datastorePanels) {
                        String name = datastorePanel.getDatastore().getName().toLowerCase();
                        datastorePanel.setVisible(name.indexOf(text) != -1);
                    }
                    selectFirstVisibleDatastore();
                }
            }
        });
        _searchDatastoreTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    clickAnalyzeButton();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    selectNextVisibleDatastore();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    selectPreviousVisibleDatastore();
                }
            }
        });

        setLayout(new VerticalLayout(4));

        add(Box.createVerticalStrut(10));

        final DCLabel jobsHeaderLabel = DCLabel.dark("Jobs");
        jobsHeaderLabel.setFont(WidgetUtils.FONT_HEADER1);
        add(jobsHeaderLabel);

        _jobsListPanel = new DCPanel();
        final GridLayout jobsListLayout = new GridLayout(1, MAX_JOB_PANELS);
        jobsListLayout.setHgap(10);
        _jobsListPanel.setBorder(new EmptyBorder(10, 10, 4, 0));
        _jobsListPanel.setLayout(jobsListLayout);

        final List<FileObject> recentJobFiles = getRecentJobFiles();

        updateJobsListPanel(recentJobFiles);

        add(_jobsListPanel);

        _moreRecentJobsButton = new JButton("More", imageManager.getImageIcon(IconUtils.FILE_FOLDER,
                IconUtils.ICON_SIZE_SMALL));
        _moreRecentJobsButton.setMargin(new Insets(1, 1, 1, 4));
        if (recentJobFiles.size() <= MAX_JOB_PANELS) {
            _moreRecentJobsButton.setEnabled(false);
        } else {
            _moreRecentJobsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final JPopupMenu popup = new JPopupMenu();
                    for (int i = 3; i < recentJobFiles.size(); i++) {
                        final FileObject jobFile = recentJobFiles.get(i);
                        final JMenuItem menuItem = new OpenAnalysisJobMenuItem(jobFile, _openAnalysisJobActionListener);
                        popup.add(menuItem);
                    }
                    popup.show(_moreRecentJobsButton, 0, _moreRecentJobsButton.getHeight());
                }
            });
        }

        final DCPanel jobsButtonPanel = new DCPanel();
        jobsButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        jobsButtonPanel.add(_moreRecentJobsButton);
        jobsButtonPanel.add(_browseJobsButton);
        add(jobsButtonPanel);

        add(Box.createVerticalStrut(40));

        final DCLabel datastoreHeaderLabel = DCLabel.dark("Datastores");
        datastoreHeaderLabel.setFont(WidgetUtils.FONT_HEADER1);
        add(datastoreHeaderLabel);

        final DCLabel registerNewDatastoreLabel = DCLabel.dark("Register new:");
        registerNewDatastoreLabel.setFont(WidgetUtils.FONT_HEADER2);

        final DCPanel newDatastorePanel = new DCPanel();
        newDatastorePanel.setLayout(new VerticalLayout(4));
        newDatastorePanel.setBorder(new EmptyBorder(10, 10, 10, 0));
        newDatastorePanel.add(registerNewDatastoreLabel);
        newDatastorePanel.add(createNewDatastorePanel());

        add(newDatastorePanel);

        _datastoreListPanel = new DCPanel();
        _datastoreListPanel.setLayout(new VerticalLayout(4));
        _datastoreListPanel.setBorder(new EmptyBorder(10, 10, 4, 0));
        add(_datastoreListPanel);
        updateDatastores();

        final DCPanel datastoresButtonPanel = new DCPanel();
        datastoresButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        datastoresButtonPanel.setBorder(new EmptyBorder(0, 10, 0, 0));
        datastoresButtonPanel.add(_analyzeButton);
        add(datastoresButtonPanel);
    }

    private List<FileObject> getRecentJobFiles() {
        final List<FileObject> recentJobFiles = _userPreferences.getRecentJobFiles();
        final List<FileObject> result = new ArrayList<>();
        for (FileObject fileObject : recentJobFiles) {
            try {
                if (fileObject.exists()) {
                    result.add(fileObject);
                    if (result.size() == 10) {
                        break;
                    }
                }
            } catch (FileSystemException ex) {
                logger.debug("Skipping file {} because of unexpected error", ex);
            }
        }
        return result;
    }

    private void updateJobsListPanel(List<FileObject> recentJobFiles) {
        if (recentJobFiles.isEmpty()) {
            _jobsListPanel.add(DCLabel.dark("(no recent jobs)"));
        } else {
            int jobIndex = 0;
            for (FileObject fileObject : recentJobFiles) {
                _jobsListPanel.add(new OpenAnalysisJobPanel(fileObject, _configuration, _openAnalysisJobActionListener));
                jobIndex++;
                if (jobIndex == MAX_JOB_PANELS) {
                    break;
                }
            }
        }
    }

    private void updateDatastores() {
        Datastore selectedDatastore = getSelectedDatastore();
        _datastoreListPanel.removeAll();
        _datastorePanels.clear();

        final DCLabel existingDatastoresLabel = DCLabel.dark("Existing datastores:");
        existingDatastoresLabel.setFont(WidgetUtils.FONT_HEADER2);

        final DCPanel searchDatastorePanel = DCPanel.around(_searchDatastoreTextField);
        searchDatastorePanel.setBorder(WidgetUtils.BORDER_SHADOW);

        final DCPanel headerPanel = new DCPanel();
        headerPanel.setLayout(new FlowLayout(Alignment.LEFT.getFlowLayoutAlignment(), 0, 0));
        headerPanel.add(existingDatastoresLabel);
        headerPanel.add(Box.createHorizontalStrut(20));
        headerPanel.add(searchDatastorePanel);

        _datastoreListPanel.add(headerPanel);

        boolean selectFirst = true;

        String[] datastoreNames = _datastoreCatalog.getDatastoreNames();
        for (int i = 0; i < datastoreNames.length; i++) {
            final Datastore datastore = _datastoreCatalog.getDatastore(datastoreNames[i]);
            DatastorePanel datastorePanel = new DatastorePanel(datastore, _datastoreCatalog, this,
                    _analysisJobBuilderWindow.getWindowContext(), _injectorBuilder);
            _datastorePanels.add(datastorePanel);
            _datastoreListPanel.add(datastorePanel);

            if (selectedDatastore != null && selectedDatastore.getName().equals(datastore.getName())) {
                selectFirst = false;
                setSelectedDatastore(datastore);
            }
        }

        if (selectFirst) {
            selectFirstVisibleDatastore();
        }
    }

    public void setSelectedDatastore(Datastore datastore) {
        if (datastore != null) {
            for (DatastorePanel panel : _datastorePanels) {
                if (datastore.equals(panel.getDatastore())) {
                    panel.setSelected(true);
                } else {
                    panel.setSelected(false);
                }
            }
        }
    }

    public Datastore getSelectedDatastore() {
        DatastorePanel datastorePanel = getSelectedDatastorePanel();
        if (datastorePanel == null) {
            return null;
        }
        return datastorePanel.getDatastore();
    }

    private DCPanel createNewDatastorePanel() {
        final DCPanel panel = new DCPanel();
        panel.setBorder(WidgetUtils.BORDER_LIST_ITEM);
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.add(createNewDatastoreButton("CSV file",
                "Comma-separated values (CSV) file (or file with other separators)", IconUtils.CSV_IMAGEPATH,
                CsvDatastore.class, CsvDatastoreDialog.class));
        panel.add(createNewDatastoreButton("Excel spreadsheet",
                "Microsoft Excel spreadsheet. Either .xls (97-2003) or .xlsx (2007+) format.",
                IconUtils.EXCEL_IMAGEPATH, ExcelDatastore.class, ExcelDatastoreDialog.class));
        panel.add(createNewDatastoreButton("Access database", "Microsoft Access database file (.mdb).",
                IconUtils.ACCESS_IMAGEPATH, AccessDatastore.class, AccessDatastoreDialog.class));
        panel.add(createNewDatastoreButton("SAS library", "A directory of SAS library files (.sas7bdat).",
                IconUtils.SAS_IMAGEPATH, SasDatastore.class, SasDatastoreDialog.class));
        panel.add(createNewDatastoreButton("DBase database", "DBase database file (.dbf)", IconUtils.DBASE_IMAGEPATH,
                DbaseDatastore.class, DbaseDatastoreDialog.class));
        panel.add(createNewDatastoreButton("Fixed width file",
                "Text file with fixed width values. Each value spans a fixed amount of text characters.",
                IconUtils.FIXEDWIDTH_IMAGEPATH, FixedWidthDatastore.class, FixedWidthDatastoreDialog.class));
        panel.add(createNewDatastoreButton("XML file", "Extensible Markup Language file (.xml)",
                IconUtils.XML_IMAGEPATH, XmlDatastore.class, XmlDatastoreDialog.class));
        panel.add(createNewDatastoreButton("JSON file", "JavaScript Object NOtation file (.json).",
                IconUtils.JSON_IMAGEPATH, JsonDatastore.class, JsonDatastoreDialog.class));
        panel.add(createNewDatastoreButton("OpenOffice.org Base database", "OpenOffice.org Base database file (.odb)",
                IconUtils.ODB_IMAGEPATH, OdbDatastore.class, OdbDatastoreDialog.class));

        panel.add(Box.createHorizontalStrut(10));

        panel.add(createNewDatastoreButton("Salesforce.com", "Connect to a Salesforce.com account",
                IconUtils.SALESFORCE_IMAGEPATH, SalesforceDatastore.class, SalesforceDatastoreDialog.class));
        panel.add(createNewDatastoreButton("SugarCRM", "Connect to a SugarCRM system", IconUtils.SUGAR_CRM_IMAGEPATH,
                SugarCrmDatastore.class, SugarCrmDatastoreDialog.class));

        panel.add(Box.createHorizontalStrut(10));

        panel.add(createNewDatastoreButton("MongoDB database", "Connect to a MongoDB database",
                IconUtils.MONGODB_IMAGEPATH, MongoDbDatastore.class, MongoDbDatastoreDialog.class));

        panel.add(createNewDatastoreButton("CouchDB database", "Connect to an Apache CouchDB database",
                IconUtils.COUCHDB_IMAGEPATH, CouchDbDatastore.class, CouchDbDatastoreDialog.class));

        panel.add(createNewDatastoreButton("HBase database", "Connect to an Apache HBase database",
                IconUtils.HBASE_IMAGEPATH, HBaseDatastore.class, HBaseDatastoreDialog.class));

        // set of databases that are displayed directly on panel
        final Set<String> databaseNames = new HashSet<String>();

        createDefaultDatabaseButtons(panel, databaseNames);

        final JButton moreDatastoreTypesButton = new JButton("More", imageManager.getImageIcon(IconUtils.FILE_FOLDER,
                IconUtils.ICON_SIZE_SMALL));
        moreDatastoreTypesButton.setMargin(new Insets(1, 1, 1, 4));
        moreDatastoreTypesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JPopupMenu popup = new JPopupMenu();

                // installed databases
                final List<DatabaseDriverDescriptor> databaseDrivers = _databaseDriverCatalog
                        .getInstalledWorkingDatabaseDrivers();
                for (DatabaseDriverDescriptor databaseDriver : databaseDrivers) {
                    final String databaseName = databaseDriver.getDisplayName();
                    if (!databaseNames.contains(databaseName)) {
                        final String imagePath = databaseDriver.getIconImagePath();
                        final ImageIcon icon = imageManager.getImageIcon(imagePath, IconUtils.ICON_SIZE_SMALL);
                        final JMenuItem menuItem = WidgetFactory.createMenuItem(databaseName, icon);
                        menuItem.addActionListener(createJdbcActionListener(databaseName));
                        popup.add(menuItem);
                    }
                }

                // custom/other jdbc connection
                {
                    final ImageIcon icon = imageManager.getImageIcon(IconUtils.GENERIC_DATASTORE_IMAGEPATH,
                            IconUtils.ICON_SIZE_SMALL);
                    final JMenuItem menuItem = WidgetFactory.createMenuItem("Other database", icon);
                    menuItem.addActionListener(createJdbcActionListener(null));
                    popup.add(menuItem);
                }

                // composite datastore
                final JMenuItem compositeMenuItem = WidgetFactory.createMenuItem("Composite datastore",
                        imageManager.getImageIcon(IconUtils.COMPOSITE_IMAGEPATH, IconUtils.ICON_SIZE_SMALL));
                compositeMenuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        new CompositeDatastoreDialog(_datastoreCatalog, _analysisJobBuilderWindow.getWindowContext())
                                .setVisible(true);
                    }
                });

                final JMenuItem databaseDriversMenuItem = WidgetFactory.createMenuItem("Manage database drivers...",
                        imageManager.getImageIcon(IconUtils.MENU_OPTIONS, IconUtils.ICON_SIZE_SMALL));
                databaseDriversMenuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        OptionsDialog dialog = _optionsDialogProvider.get();
                        dialog.selectDatabaseDriversTab();
                        dialog.setVisible(true);
                    }
                });

                popup.add(databaseDriversMenuItem);
                popup.add(new JSeparator(JSeparator.HORIZONTAL));
                popup.add(compositeMenuItem);
                popup.setBorder(WidgetUtils.BORDER_THIN);

                popup.show(moreDatastoreTypesButton, 0, moreDatastoreTypesButton.getHeight());
            }
        });

        panel.add(Box.createHorizontalStrut(10));
        panel.add(moreDatastoreTypesButton);

        return panel;
    }

    private void createDefaultDatabaseButtons(DCPanel panel, Set<String> databaseNames) {
        if (_databaseDriverCatalog.isInstalled(DatabaseDriverCatalog.DATABASE_NAME_MYSQL)) {
            panel.add(createNewJdbcDatastoreButton("MySQL connection", "Connect to a MySQL database",
                    "images/datastore-types/databases/mysql.png", DatabaseDriverCatalog.DATABASE_NAME_MYSQL,
                    databaseNames));
        }
        if (_databaseDriverCatalog.isInstalled(DatabaseDriverCatalog.DATABASE_NAME_POSTGRESQL)) {
            panel.add(createNewJdbcDatastoreButton("PostgreSQL connection", "Connect to a PostgreSQL database",
                    "images/datastore-types/databases/postgresql.png", DatabaseDriverCatalog.DATABASE_NAME_POSTGRESQL,
                    databaseNames));
        }
        if (_databaseDriverCatalog.isInstalled(DatabaseDriverCatalog.DATABASE_NAME_ORACLE)) {
            panel.add(createNewJdbcDatastoreButton("Oracle connection", "Connect to a Oracle database",
                    "images/datastore-types/databases/oracle.png", DatabaseDriverCatalog.DATABASE_NAME_ORACLE,
                    databaseNames));
        }
        if (_databaseDriverCatalog.isInstalled(DatabaseDriverCatalog.DATABASE_NAME_MICROSOFT_SQL_SERVER_JTDS)) {
            panel.add(createNewJdbcDatastoreButton("Microsoft SQL Server connection",
                    "Connect to a Microsoft SQL Server database", "images/datastore-types/databases/microsoft.png",
                    DatabaseDriverCatalog.DATABASE_NAME_MICROSOFT_SQL_SERVER_JTDS, databaseNames));
        }
    }

    private <D extends Datastore> JButton createNewDatastoreButton(final String title, final String description,
            final String imagePath, final Class<D> datastoreClass, final Class<? extends AbstractDialog> dialogClass) {
        final ImageIcon icon = imageManager.getImageIcon(imagePath);
        final JButton button = WidgetFactory.createImageButton(icon);

        final DCPopupBubble popupBubble = new DCPopupBubble(_glassPane, "<html><b>" + title + "</b><br/>" + description
                + "</html>", 0, 0, imagePath);
        popupBubble.attachTo(button);

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Injector injectorWithNullDatastore = _injectorBuilder.with(datastoreClass, null).createInjector();
                AbstractDialog dialog = injectorWithNullDatastore.getInstance(dialogClass);
                dialog.setVisible(true);
            }
        });
        return button;
    }

    private JButton createNewJdbcDatastoreButton(final String title, final String description, final String imagePath,
            final String databaseName, Set<String> databaseNames) {

        databaseNames.add(databaseName);

        final ImageIcon icon = imageManager.getImageIcon(imagePath);
        final JButton button = WidgetFactory.createImageButton(icon);

        final DCPopupBubble popupBubble = new DCPopupBubble(_glassPane, "<html><b>" + title + "</b><br/>" + description
                + "</html>", 0, 0, imagePath);
        popupBubble.attachTo(button);

        button.addActionListener(createJdbcActionListener(databaseName));

        return button;
    }

    private ActionListener createJdbcActionListener(final String databaseName) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Injector injectorWithDatastore = _injectorBuilder.with(JdbcDatastore.class, null).createInjector();
                JdbcDatastoreDialog dialog = injectorWithDatastore.getInstance(JdbcDatastoreDialog.class);
                dialog.setSelectedDatabase(databaseName);
                dialog.setVisible(true);
            }
        };
    }
    
    @Override
    public void addNotify() {
        super.addNotify();
        _datastoreCatalog.addListener(this);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        _datastoreCatalog.removeListener(this);
    }

    @Override
    public void onAdd(Datastore datastore) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateDatastores();
            }
        });
    }

    @Override
    public void onRemove(Datastore datastore) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateDatastores();
            }
        });
    }

    private void selectFirstVisibleDatastore() {
        boolean found = false;

        for (DatastorePanel datastorePanel : _datastorePanels) {
            if (datastorePanel.isVisible()) {
                setSelectedDatastorePanel(datastorePanel);
                found = true;
                break;
            }
        }

        _analyzeButton.setEnabled(found);
    }

    private void selectNextVisibleDatastore() {
        DatastorePanel selectedDatastorePanel = getSelectedDatastorePanel();
        if (selectedDatastorePanel != null) {

            int indexOf = _datastorePanels.indexOf(selectedDatastorePanel);
            for (int i = indexOf + 1; i < _datastorePanels.size(); i++) {
                DatastorePanel panel = _datastorePanels.get(i);
                if (panel.isVisible()) {
                    setSelectedDatastorePanel(panel);
                    break;
                }
            }
        }
    }

    private void selectPreviousVisibleDatastore() {
        DatastorePanel selectedDatastorePanel = getSelectedDatastorePanel();
        if (selectedDatastorePanel != null) {

            int indexOf = _datastorePanels.indexOf(selectedDatastorePanel);
            for (int i = indexOf - 1; i >= 0; i--) {
                DatastorePanel panel = _datastorePanels.get(i);
                if (panel.isVisible()) {
                    setSelectedDatastorePanel(panel);
                    break;
                }
            }
        }
    }

    public void setSelectedDatastorePanel(DatastorePanel datastorePanel) {
        for (DatastorePanel panel : _datastorePanels) {
            if (datastorePanel == panel) {
                panel.setSelected(true);
            } else {
                panel.setSelected(false);
            }
        }
        requestSearchFieldFocus();
    }

    public DatastorePanel getSelectedDatastorePanel() {
        for (DatastorePanel panel : _datastorePanels) {
            if (panel.isVisible() && panel.isSelected()) {
                return panel;
            }
        }
        return null;
    }

    public void clickAnalyzeButton() {
        if (_analyzeButton.isEnabled()) {
            _analyzeButton.doClick();
        }
    }

    public void requestSearchFieldFocus() {
        _searchDatastoreTextField.requestFocus();
    }
}
