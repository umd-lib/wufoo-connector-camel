package edu.umd.lib.configuration;

import java.util.HashMap;
import java.util.Map;

public class MappedField {
    private String name;
    private String wufooField;
    private Map<String, String> mapping;

    public MappedField() {
    }

    public MappedField(String name, String wufooField, Map<String, String> mapping) {
        this.name = name;
        this.wufooField = wufooField;
        this.mapping = mapping;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWufooField() {
        return this.wufooField;
    }

    public void setWufooField(String wufooField) {
        this.wufooField = wufooField;
    }

    public Map<String, String> getMapping() {
        if (this.mapping == null) {
            this.mapping = new HashMap<String, String>();
        }
        return this.mapping;
    }

    public void setMapping(Map<String, String> mapping) {
        this.mapping = mapping;
    }

    public String getMappedValue(String wufooValue) {
        return getMapping().get(wufooValue);
    }
}
