package edu.umd.lib.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.Test;

import edu.umd.lib.AbstractTest;

public class MappedFieldTest extends AbstractTest {

    @Test
    public void testMappedField() throws JsonParseException, JsonMappingException, IOException {
        List<TicketField> fields = target.readValue(getStream("complex-config.yml"), collectionType);
        assertNotNull(fields);
        assertThat(fields, IsCollectionWithSize.hasSize(2));
        TicketField descriptionField = fields.get(0);
        assertNotNull(descriptionField);
        assertThat(descriptionField.getName(), is("description"));
        assertThat(descriptionField.getMappedFields(), IsCollectionWithSize.hasSize(2));

        MappedField fieldOne = descriptionField.getMappedFields().get(0);
        assertNotNull(fieldOne);
        assertThat(fieldOne.getName(), is("Fieldx_mapped_as_name"));
        assertThat(fieldOne.getWufooField(), is("Field1"));
        assertThat(fieldOne.getMapping(), IsMapContaining.hasEntry("ssdr", "Software Systems Development & Research"));
        assertThat(fieldOne.getMapping(), IsMapContaining.hasEntry("uss", "User Support & Servicess"));

        MappedField fieldTwo = descriptionField.getMappedFields().get(1);
        assertThat(fieldTwo.getName(), is("one_more_field"));
        assertThat(fieldTwo.getWufooField(), is("Field2"));
        assertThat(fieldTwo.getMapping(), IsMapContaining.hasEntry("McKeldin Library", "12"));
        assertThat(fieldTwo.getMapping(), IsMapContaining.hasEntry("Hornbake Library", "13"));
    }
}
