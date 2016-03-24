package org.meb.conquestdb.json;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.meb.conquest.db.converter.CardSetTypeConverter;
import org.meb.conquest.db.model.CardSetType;

public class CardSetTypeDeserializer extends JsonDeserializer<CardSetType> {

	@Override
	public CardSetType deserialize(JsonParser parser, DeserializationContext arg1)
			throws IOException, JsonProcessingException {
		return new CardSetTypeConverter().convertToEntityAttribute(parser.getText());
	}
}
