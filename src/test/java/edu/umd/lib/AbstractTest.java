package edu.umd.lib;

import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.junit.jupiter.api.BeforeAll;

import edu.umd.lib.configuration.TicketField;

public abstract class AbstractTest {

    protected static ObjectMapper target;

    protected static CollectionType collectionType;

    @BeforeAll
    protected static void setup() {
        target = new ObjectMapper(new YAMLFactory());
        collectionType = target.getTypeFactory().constructCollectionType(List.class, TicketField.class);
    }

    protected InputStream getStream(String name) {
        return this.getClass().getResourceAsStream(name);
    }
}
