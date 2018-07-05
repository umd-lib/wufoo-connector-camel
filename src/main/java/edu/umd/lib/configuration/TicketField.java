package edu.umd.lib.configuration;

import java.util.ArrayList;
import java.util.List;

public class TicketField {
    private String name;
    private String template;
    List<MappedField> mappedFields;

    public TicketField() {
    }

    public TicketField(String name, String template, List<MappedField> mappedFields) {
        this.name = name;
        this.template = template;
        this.mappedFields = mappedFields;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemplate() {
        return this.template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public List<MappedField> getMappedFields() {
        if (this.mappedFields == null) {
            this.mappedFields = new ArrayList<MappedField>();
        }
        return this.mappedFields;
    }

    public void setMappedFields(List<MappedField> mappedFields) {
        this.mappedFields = mappedFields;
    }
}
