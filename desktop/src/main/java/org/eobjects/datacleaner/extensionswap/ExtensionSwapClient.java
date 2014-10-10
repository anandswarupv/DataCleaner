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
package org.eobjects.datacleaner.extensionswap;

import java.io.File;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.apache.http.client.HttpClient;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfiguration;
import org.eobjects.analyzer.util.StringUtils;
import org.eobjects.analyzer.util.VFSUtils;
import org.eobjects.datacleaner.actions.DownloadFilesActionListener;
import org.eobjects.datacleaner.actions.FileDownloadListener;
import org.eobjects.datacleaner.bootstrap.WindowContext;
import org.eobjects.datacleaner.extensions.ExtensionReader;
import org.eobjects.datacleaner.user.ExtensionPackage;
import org.eobjects.datacleaner.user.UserPreferences;
import org.eobjects.datacleaner.util.HttpXmlUtils;
import org.eobjects.datacleaner.util.SimpleWebServiceHttpClient;
import org.eobjects.datacleaner.util.WebServiceHttpClient;
import org.w3c.dom.Element;

public final class ExtensionSwapClient {

    public static final String DEFAULT_WEBSITE_HOSTNAME = "datacleaner.org";

    private static final String EXTENSIONSWAP_ID_PROPERTY = "extensionswap.id";

    private final HttpClient _httpClient;
    private final WindowContext _windowContext;
    private final String _baseUrl;
    private final UserPreferences _userPreferences;
    private final AnalyzerBeansConfiguration _configuration;

    public ExtensionSwapClient(HttpClient httpClient, WindowContext windowContext, UserPreferences userPreferences,
            AnalyzerBeansConfiguration configuration) {
        this(DEFAULT_WEBSITE_HOSTNAME, windowContext, userPreferences, configuration, httpClient);
    }

    public ExtensionSwapClient(String websiteHostname, WindowContext windowContext, UserPreferences userPreferences,
            AnalyzerBeansConfiguration configuration, HttpClient httpClient) {
        this(httpClient, websiteHostname, windowContext, userPreferences, configuration);
    }

    public ExtensionSwapClient(HttpClient httpClient, String websiteHostname, WindowContext windowContext,
            UserPreferences userPreferences, AnalyzerBeansConfiguration configuration) {
        _httpClient = httpClient;
        _windowContext = windowContext;
        _baseUrl = "http://" + websiteHostname + "/ws/extension/";
        _userPreferences = userPreferences;
        _configuration = configuration;
    }

    public ExtensionPackage registerExtensionPackage(ExtensionSwapPackage extensionSwapPackage, File jarFile) {
        final ExtensionReader reader = new ExtensionReader();
        final ExtensionPackage extensionPackage = reader.readExternalExtension(new File[] { jarFile });

        extensionPackage.getAdditionalProperties().put(EXTENSIONSWAP_ID_PROPERTY, extensionSwapPackage.getId());
        extensionPackage.getAdditionalProperties().put("version", Integer.toString(extensionSwapPackage.getVersion()));
        extensionPackage.loadDescriptors(_configuration.getDescriptorProvider());
        _userPreferences.getExtensionPackages().add(extensionPackage);
        return extensionPackage;
    }

    public ExtensionSwapPackage getExtensionSwapPackage(String id) {
        final Element rootNode = HttpXmlUtils.getRootNode(_httpClient, _baseUrl + id);
        final String name = HttpXmlUtils.getChildNodeText(rootNode, "name");
        final int version = Integer.parseInt(HttpXmlUtils.getChildNodeText(rootNode, "version"));
        final String packageName = HttpXmlUtils.getChildNodeText(rootNode, "package");
        return new ExtensionSwapPackage(id, version, name, packageName);
    }

    public void registerExtensionPackage(final ExtensionSwapPackage extensionSwapPackage, final String username) {
        downloadJarFile(extensionSwapPackage, username, new FileDownloadListener() {
            @Override
            public void onFilesDownloaded(FileObject[] files) {
                File jarFile = VFSUtils.toFile(files[0]);
                registerExtensionPackage(extensionSwapPackage, jarFile);
            }
        });
    }

    private void downloadJarFile(ExtensionSwapPackage extensionSwapPackage, String username,
            FileDownloadListener listener) {
        String url = _baseUrl + extensionSwapPackage.getId() + "/jarfile";
        if (!StringUtils.isNullOrEmpty(username)) {
            url = url + "?username=" + username;
        }

        String filename = extensionSwapPackage.getId() + ".jar";
        FileObject targetDirectory = VFSUtils.toFileObject(_userPreferences.getExtensionsDirectory());
        WebServiceHttpClient httpClient = new SimpleWebServiceHttpClient(_httpClient);
        DownloadFilesActionListener actionListener = new DownloadFilesActionListener(new String[] { url },
                targetDirectory, new String[] { filename }, listener, _windowContext, httpClient);
        actionListener.actionPerformed(null);
    }

    public boolean isInstalled(ExtensionSwapPackage extensionSwapPackage) {
        List<ExtensionPackage> extensionPackages = _userPreferences.getExtensionPackages();
        for (ExtensionPackage extensionPackage : extensionPackages) {
            String id = extensionPackage.getAdditionalProperties().get(EXTENSIONSWAP_ID_PROPERTY);
            if (extensionSwapPackage.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
