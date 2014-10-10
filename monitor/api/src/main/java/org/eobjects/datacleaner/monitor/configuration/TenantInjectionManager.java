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
package org.eobjects.datacleaner.monitor.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eobjects.analyzer.configuration.InjectionManager;
import org.eobjects.analyzer.configuration.InjectionPoint;
import org.eobjects.analyzer.util.convert.ClasspathResourceTypeHandler;
import org.eobjects.analyzer.util.convert.FileResourceTypeHandler;
import org.eobjects.analyzer.util.convert.ResourceConverter;
import org.eobjects.analyzer.util.convert.ResourceConverter.ResourceTypeHandler;
import org.eobjects.analyzer.util.convert.UrlResourceTypeHandler;
import org.eobjects.analyzer.util.convert.VfsResourceTypeHandler;
import org.eobjects.datacleaner.repository.Repository;
import org.eobjects.datacleaner.repository.RepositoryFileResourceTypeHandler;

/**
 * A {@link InjectionManager} wrapper that is tenant-aware.
 */
public class TenantInjectionManager implements InjectionManager {

    private final InjectionManager _delegate;
    private final TenantContext _tenantContext;
    private final Repository _repository;

    public TenantInjectionManager(InjectionManager delegate, Repository repository, TenantContext tenantContext) {
        _delegate = delegate;
        _repository = repository;
        _tenantContext = tenantContext;
    }
    
    public String getTenantId() {
        return _tenantContext.getTenantId();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> E getInstance(InjectionPoint<E> injectionPoint) {
        final Class<E> baseType = injectionPoint.getBaseType();
        if (baseType == ResourceConverter.class) {
            return (E) createResourceConverter();
        }
        return _delegate.getInstance(injectionPoint);
    }

    private ResourceConverter createResourceConverter() {
        List<ResourceTypeHandler<?>> handlers = new ArrayList<ResourceTypeHandler<?>>();
        handlers.add(new FileResourceTypeHandler(getRelativeParentDirectory()));
        handlers.add(new UrlResourceTypeHandler());
        handlers.add(new ClasspathResourceTypeHandler());
        handlers.add(new VfsResourceTypeHandler());

        String tenantId = _tenantContext.getTenantId();

        handlers.add(new RepositoryFileResourceTypeHandler(_repository, tenantId));

        return new ResourceConverter(handlers, "repo");
    }

    private File getRelativeParentDirectory() {
        // TODO : Check for file repo
        return null;
    }
}
