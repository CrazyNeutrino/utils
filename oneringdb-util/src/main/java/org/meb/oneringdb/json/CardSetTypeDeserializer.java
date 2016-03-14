package org.meb.oneringdb.json;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.meb.oneringdb.db.converter.CardSetTypeConverter;
import org.meb.oneringdb.db.model.CardSetType;

public class CardSetTypeDeserializer extends JsonDeserializer<CardSetType> {

	@Override
	public CardSetType deserialize(JsonParser parser, DeserializationContext arg1)
			throws IOException, JsonProcessingException {
		return new CardSetTypeConverter().convertToEntityAttribute(parser.getText());
	}
}
