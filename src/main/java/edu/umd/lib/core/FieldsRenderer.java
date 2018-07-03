package edu.umd.lib.core;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.apache.log4j.Logger;
import org.stringtemplate.v4.ST;

import edu.umd.lib.configuration.TicketField;
import edu.umd.lib.exception.FormMappingException;

public class FieldsRenderer {

    private final Logger log = Logger.getLogger(FieldsRenderer.class);

    private Map<String, String> wufooValues;

    private List<TicketField> ticketFields;

    private Map<String, String> renderedFields;

    private String configName;

    public FieldsRenderer(String yamlConfigurationFile, Map<String, String> wufooValues) throws FormMappingException {
        this.configName = yamlConfigurationFile;
        this.wufooValues = wufooValues;
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, TicketField.class);
        File configFile = new File(yamlConfigurationFile);
        try {
			ticketFields = mapper.readValue(configFile, type);
		} catch (IOException e) {
			e.printStackTrace();
			throw new FormMappingException("Exception encountered while processing yaml file: " + configName);
		}
    }

	public Map<String, String> getRenderedFields() {
		if (this.renderedFields == null) {
            renderFields();
        }
        return this.renderedFields;
	}

	private void renderFields() {
        renderedFields = new HashMap<String, String>();
        ticketFields.forEach(field -> System.out.println(field.getName()));
        ticketFields.forEach(field -> renderedFields.put(field.getName(), getRenderedValue(field)));
	}

	private String getRenderedValue(TicketField field) {
		log.debug("Template string: " + field.getTemplate());
        String renderedString = "";
        ST st = new ST(field.getTemplate());
        getNonEmptyMap(wufooValues).keySet().forEach(k -> st.add(k, wufooValues.get(k)));
        // Add mapped values based on the wufoo value
        field.getMappedFields().forEach(fl -> {
            if (wufooValues.get(fl.getWufooField()) != null) {
                st.add(fl.getName(), fl.getMappedValue(wufooValues.get(fl.getWufooField())));
            }
        });
        renderedString = st.render();
        log.debug("Rendered string: " + renderedString);
        return renderedString;
    }
    
    private Map<String, String> getNonEmptyMap(Map<String, String> map) {
        return map.entrySet().stream().filter(x -> !x.getValue().isEmpty())
                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
    }
}
