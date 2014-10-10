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
package org.eobjects.datacleaner.monitor.server.controllers;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.eobjects.analyzer.configuration.InjectionManagerFactoryImpl;
import org.eobjects.datacleaner.monitor.configuration.TenantContextFactory;
import org.eobjects.datacleaner.monitor.configuration.TenantContextFactoryImpl;
import org.eobjects.datacleaner.monitor.server.job.MockJobEngineManager;
import org.eobjects.datacleaner.repository.Repository;
import org.eobjects.datacleaner.repository.file.FileRepository;

public class JobInvocationControllerTest extends TestCase {

    public void testInvokeDatabaseSchema() throws Throwable {
        final Repository repository = new FileRepository("src/test/resources/example_repo");
        final TenantContextFactory contextFactory = new TenantContextFactoryImpl(repository,
                new InjectionManagerFactoryImpl(), new MockJobEngineManager());
        final JobInvocationController controller = new JobInvocationController();
        controller._contextFactory = contextFactory;

        final JobInvocationPayload sourceRecords = new JobInvocationPayload();
        sourceRecords.addRow(new Object[] { "kasper@eobjects.dk" });
        sourceRecords.addRow(new Object[] { "kasper.sorensen@humaninference.com" });

        JobInvocationPayload result = controller.invokeJob("tenant1", "email_standardizer", sourceRecords);

        assertEquals("[Username, Domain]", result.getColumns().toString());

        List<JobInvocationRowData> rows = result.getRows();
        assertEquals(2, rows.size());

        assertEquals("[kasper, eobjects.dk]", Arrays.toString(rows.get(0).getValues()));
        assertEquals("[kasper.sorensen, humaninference.com]", Arrays.toString(rows.get(1).getValues()));
    }

    public void testInvokeFileWithExtensionNameSchema() throws Throwable {
        final Repository repository = new FileRepository("src/test/resources/example_repo");
        final TenantContextFactory contextFactory = new TenantContextFactoryImpl(repository,
                new InjectionManagerFactoryImpl(), new MockJobEngineManager());
        final JobInvocationController controller = new JobInvocationController();
        controller._contextFactory = contextFactory;

        final JobInvocationPayload sourceRecords = new JobInvocationPayload();

        final int input = 123;
        sourceRecords.addRow(new Object[] { input });

        JobInvocationPayload result = controller.invokeJob("tenant1", "random_number_generation", sourceRecords);

        assertEquals("[Random number]", result.getColumns().toString());

        List<JobInvocationRowData> rows = result.getRows();
        assertEquals(1, rows.size());

        final Object[] values = rows.get(0).getValues();
        assertEquals(1, values.length);
        final Number number = (Number) values[0];
        assertTrue(number.doubleValue() >= 0);
        assertTrue(number.doubleValue() <= input);
    }
}
