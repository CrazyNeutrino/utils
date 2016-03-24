package org.meb.conquestdb.json;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.meb.conquest.db.converter.CardTypeConverter;
import org.meb.conquest.db.model.CardType;

public class CardTypeDeserializer extends JsonDeserializer<CardType> {

	@Override
	public CardType deserialize(JsonParser parser, DeserializationContext arg1) throws IOException,
			JsonProcessingException {
		return new CardTypeConverter().convertToEntityAttribute(parser.getText());
	}
}
