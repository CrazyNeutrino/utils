package org.meb.conquest.json;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.meb.conquest.db.converter.CardTypeConverter;
import org.meb.conquest.db.model.CardType;

public class CardTypeSerializer extends JsonSerializer<CardType> {

	@Override
	public void serialize(CardType value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {
		jgen.writeString(new CardTypeConverter().convertToDatabaseColumn(value));
	}
}
