package edu.umd.lib.core;

import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.lib.AbstractTest;
import edu.umd.lib.exception.FormMappingException;

public class FieldsRendererTest extends AbstractTest {

    private static Map<String, String> wufooValues;

    private FieldsRenderer renderer;

    @BeforeEach
    public void setupWufooValues() {
        wufooValues = new HashMap<String, String>();
        wufooValues.put("Field1", "ssdr");
    }

    @Test
    public void testRendering() throws FormMappingException {
        renderer = new FieldsRenderer(this.getClass().getResource("test-config.yml").getPath(), wufooValues);
        Map<String, String> renderedFields = renderer.getRenderedFields();
        assertNotNull(renderedFields);

        String expected;

        // Simple Rendereing
        assertThat(renderedFields, IsMapContaining.hasEntry("simpleField", "Simple Text"));

        // Templated Rendering
        assertThat(renderedFields, IsMapContaining.hasEntry("templatedField", "Templated Text - ssdr"));

        // Mapped Rendering
        expected = "Templated Text - Software Systems Development & Research";
        assertThat(renderedFields, IsMapContaining.hasEntry("mappedField", expected));

        // Multiline Rendering
        expected = "Multi line template\n" +
            "Value of Field1: ssdr\n";
        assertThat(renderedFields, IsMapContaining.hasEntry("multilineField", expected));


    }


    @Test
    public void testConditionalTemplateFieldWithoutValue() throws FormMappingException {
        renderer = new FieldsRenderer(this.getClass().getResource("test-config.yml").getPath(), wufooValues);
        Map<String, String> renderedFields = renderer.getRenderedFields();
        assertNotNull(renderedFields);
        assertThat(renderedFields, IsMapContaining.hasEntry("conditionalTemplateField", ""));
    }

    @Test
    public void testConditionalTemplateFieldWithValue() throws FormMappingException {
        wufooValues.put("Field2", "uss");
        renderer = new FieldsRenderer(this.getClass().getResource("test-config.yml").getPath(), wufooValues);
        Map<String, String> renderedFields = renderer.getRenderedFields();
        assertNotNull(renderedFields);
        String expected = "Value of Field2: uss";
        assertThat(renderedFields, IsMapContaining.hasEntry("conditionalTemplateField", expected));
    }

    @Test
    public void testCommaSeparatedFieldWithNoValue() throws FormMappingException {
        renderer = new FieldsRenderer(this.getClass().getResource("test-config.yml").getPath(), new HashMap<>());
        Map<String, String> renderedFields = renderer.getRenderedFields();
        assertNotNull(renderedFields);
        String expected = "";
        assertThat(renderedFields, IsMapContaining.hasEntry("commaSeparatedField", expected));
    }

    @Test
    public void testCommaSeparatedFieldWithOneValue() throws FormMappingException {
        renderer = new FieldsRenderer(this.getClass().getResource("test-config.yml").getPath(), wufooValues);
        Map<String, String> renderedFields = renderer.getRenderedFields();
        assertNotNull(renderedFields);
        String expected = "ssdr";
        assertThat(renderedFields, IsMapContaining.hasEntry("commaSeparatedField", expected));
    }

    @Test
    public void testCommaSeparatedFieldWithDifferntValue() throws FormMappingException {
        wufooValues.put("Field2", "uss");
        wufooValues.remove("Field1");
        renderer = new FieldsRenderer(this.getClass().getResource("test-config.yml").getPath(), wufooValues);
        Map<String, String> renderedFields = renderer.getRenderedFields();
        assertNotNull(renderedFields);
        String expected = "uss";
        assertThat(renderedFields, IsMapContaining.hasEntry("commaSeparatedField", expected));
    }

    @Test
    public void testCommaSeparatedFieldWithMultipleValues() throws FormMappingException {
        wufooValues.put("Field2", "uss");
        renderer = new FieldsRenderer(this.getClass().getResource("test-config.yml").getPath(), wufooValues);
        Map<String, String> renderedFields = renderer.getRenderedFields();
        assertNotNull(renderedFields);
        String expected = "ssdr, uss";
        assertThat(renderedFields, IsMapContaining.hasEntry("commaSeparatedField", expected));
    }

    @Test
    public void testBrokenSingleLineFieldWithNoValue() throws FormMappingException {
        renderer = new FieldsRenderer(this.getClass().getResource("test-config.yml").getPath(), new HashMap<>());
        Map<String, String> renderedFields = renderer.getRenderedFields();
        assertNotNull(renderedFields);
        String expected = "";
        assertThat(renderedFields, IsMapContaining.hasEntry("brokenSingleLineField", expected));
    }

    @Test
    public void testBrokenSingleLineFieldWithOneValue() throws FormMappingException {
        renderer = new FieldsRenderer(this.getClass().getResource("test-config.yml").getPath(), wufooValues);
        Map<String, String> renderedFields = renderer.getRenderedFields();
        assertNotNull(renderedFields);
        String expected = "ssdr";
        assertThat(renderedFields, IsMapContaining.hasEntry("brokenSingleLineField", expected));
    }

    @Test
    public void testBrokenSingleLineFieldWithDifferentValue() throws FormMappingException {
        wufooValues.put("Field2", "uss");
        wufooValues.remove("Field1");
        renderer = new FieldsRenderer(this.getClass().getResource("test-config.yml").getPath(), wufooValues);
        Map<String, String> renderedFields = renderer.getRenderedFields();
        assertNotNull(renderedFields);
        String expected = "uss";
        assertThat(renderedFields, IsMapContaining.hasEntry("brokenSingleLineField", expected));
    }

    @Test
    public void testBrokenSingleLineFieldWithMultipleValues() throws FormMappingException {
        wufooValues.put("Field2", "uss");
        renderer = new FieldsRenderer(this.getClass().getResource("test-config.yml").getPath(), wufooValues);
        Map<String, String> renderedFields = renderer.getRenderedFields();
        assertNotNull(renderedFields);
        String expected = "ssdruss";
        assertThat(renderedFields, IsMapContaining.hasEntry("brokenSingleLineField", expected));
    }

}
