package org.meb.conquest.json;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.meb.conquest.db.converter.CardSetTypeConverter;
import org.meb.conquest.db.model.CardSetType;

public class CardSetTypeSerializer extends JsonSerializer<CardSetType> {

	@Override
	public void serialize(CardSetType value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {
		jgen.writeString(new CardSetTypeConverter().convertToDatabaseColumn(value));
	}
}
