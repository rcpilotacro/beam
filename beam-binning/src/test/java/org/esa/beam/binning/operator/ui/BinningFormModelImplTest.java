/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.binning.operator.ui;

import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class BinningFormModelImplTest {

    @Test
    public void testSetGetProperty() throws Exception {
        final BinningFormModelImpl binningFormModel = new BinningFormModelImpl();
        binningFormModel.setProperty("key", new Float[]{2.0f, 3.0f});
        binningFormModel.setProperty("key2", new Integer[]{10, 20, 30});

        assertArrayEquals(new Product[0], binningFormModel.getSourceProducts());
        assertArrayEquals(new Float[]{2.0f, 3.0f}, (Float[]) binningFormModel.getPropertyValue("key"));
        assertArrayEquals(new Integer[]{10, 20, 30}, (Integer[]) binningFormModel.getPropertyValue("key2"));
    }

    @Test
    public void testVariableConfigurationProperty() throws Exception {
        final BinningFormModel binningFormModel = new BinningFormModelImpl();
        assertArrayEquals(new TargetVariableSpec[0], binningFormModel.getTargetVariableSpecs());

        final TargetVariableSpec spec = new TargetVariableSpec();
        spec.targetName = "prefix";
        TargetVariableSpec.Source source = new TargetVariableSpec.Source();
        source.type = 1;
        source.bandName = "testBand";
        spec.source = source;
        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_VARIABLE_SPECS,
                                     new TargetVariableSpec[]{spec});

        assertArrayEquals(new TargetVariableSpec[]{spec}, binningFormModel.getTargetVariableSpecs());
    }

    @Test
    public void testListening() throws Exception {
        final BinningFormModel binningFormModel = new BinningFormModelImpl();
        final MyPropertyChangeListener listener = new MyPropertyChangeListener();
        binningFormModel.addPropertyChangeListener(listener);

        binningFormModel.setProperty("key1", "value1");
        binningFormModel.setProperty("key2", "value2");

        assertEquals("value1", listener.targetMap.get("key1"));
        assertEquals("value2", listener.targetMap.get("key2"));
    }

    @Test
    public void testGetStartDate() throws Exception {
        final BinningFormModelImpl binningFormModel = new BinningFormModelImpl();
        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_TIME_FILTER_METHOD, BinningOp.TimeFilterMethod.NONE);

        assertNull(binningFormModel.getStartDateTime());

        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_TIME_FILTER_METHOD, BinningOp.TimeFilterMethod.TIME_RANGE);
        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_START_DATE_TIME, new GregorianCalendar(2000, 1, 1));

        assertNotNull(binningFormModel.getStartDateTime());
        SimpleDateFormat dateFormat = new SimpleDateFormat(BinningOp.DATE_INPUT_PATTERN);
        String expectedString = dateFormat.format(new GregorianCalendar(2000, 1, 1).getTime());
        assertEquals(expectedString, binningFormModel.getStartDateTime());
    }

    @Test
    public void testGetValidExpression() throws Exception {
        final BinningFormModelImpl binningFormModel = new BinningFormModelImpl();
        assertTrue(Boolean.parseBoolean(binningFormModel.getMaskExpr()));
        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_EXPRESSION, "some_expression");

        assertEquals("some_expression", binningFormModel.getMaskExpr());
    }

    @Test
    public void testGetSuperSampling() throws Exception {
        final BinningFormModelImpl binningFormModel = new BinningFormModelImpl();
        assertEquals(1, binningFormModel.getSupersampling());

        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_SUPERSAMPLING, 10);
        assertEquals(10, binningFormModel.getSupersampling());
    }

    @Test
    public void testGetNumRows() throws Exception {
        final BinningFormModelImpl binningFormModel = new BinningFormModelImpl();
        assertEquals(2160, binningFormModel.getNumRows());

        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_TARGET_HEIGHT, 2000);
        assertEquals(2000, binningFormModel.getNumRows());
    }

    @Test
    public void testGetRegion_Fail() throws Exception {
        final BinningFormModelImpl binningFormModel = new BinningFormModelImpl();
        try {
            binningFormModel.getRegion();
            fail();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().equals("Should never come here"));
        }
    }

    @Test
    public void testGetRegion_Global() throws Exception {
        final BinningFormModelImpl binningFormModel = new BinningFormModelImpl();

        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_GLOBAL, true);
        assertEquals("polygon((-180 -90, 180 -90, 180 90, -180 90, -180 -90))", binningFormModel.getRegion());
    }

    @Test
    public void testGetRegion_Compute() throws Exception {
        final BinningFormModelImpl binningFormModel = new BinningFormModelImpl();

        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_COMPUTE_REGION, true);
        assertNull(binningFormModel.getRegion());
    }

    @Test
    public void testGetRegion_WithSpecifiedRegion() throws Exception {
        final BinningFormModelImpl binningFormModel = new BinningFormModelImpl();

        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_REGION, true);
        binningFormModel.setProperty(BinningFilterPanel.PROPERTY_NORTH_BOUND, 50.0);
        binningFormModel.setProperty(BinningFilterPanel.PROPERTY_EAST_BOUND, 15.0);
        binningFormModel.setProperty(BinningFilterPanel.PROPERTY_WEST_BOUND, 10.0);
        binningFormModel.setProperty(BinningFilterPanel.PROPERTY_SOUTH_BOUND, 40.0);

        final String region = binningFormModel.getRegion();
        assertEquals("POLYGON ((10 40, 10 50, 15 50, 15 40, 10 40))", region);
    }

    private static class MyPropertyChangeListener implements PropertyChangeListener {

        Map<String, Object> targetMap = new HashMap<>();

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            targetMap.put(evt.getPropertyName(), evt.getNewValue());
        }
    }

}
