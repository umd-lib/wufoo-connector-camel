package edu.umd.lib.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.Test;

import edu.umd.lib.AbstractTest;

public class TicketFieldTest extends AbstractTest {

    @Test
    public void testSimpleConfig() throws JsonParseException, JsonMappingException, IOException {
        List<TicketField> ticketFields = target.readValue(getStream("simple-config.yml"), collectionType);
        assertThat(ticketFields, IsCollectionWithSize.hasSize(1));
        TicketField field = ticketFields.get(0);
        assertNotNull(field);
        assertThat(field.getName(), is("firstName"));
        assertThat(field.getTemplate(), is("<Field1>"));
    }

    @Test
    public void testComplexConfig() throws JsonParseException, JsonMappingException, IOException {
        List<TicketField> ticketFields = target.readValue(getStream("complex-config.yml"), collectionType);
        assertThat(ticketFields, IsCollectionWithSize.hasSize(2));

        TicketField descriptionField = ticketFields.get(0);
        assertNotNull(descriptionField);
        assertThat(descriptionField.getName(), is("description"));
        assertThat(descriptionField.getMappedFields(), IsCollectionWithSize.hasSize(2));

        TicketField titleField = ticketFields.get(1);
        assertNotNull(titleField);
        assertThat(titleField.getName(), is("title"));
        assertThat(titleField.getMappedFields(), IsCollectionWithSize.hasSize(1));
    }

}
