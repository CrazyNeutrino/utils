package org.meb.conquest.json;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.meb.conquest.db.converter.FactionConverter;
import org.meb.conquest.db.model.Faction;

public class FactionDeserializer extends JsonDeserializer<Faction> {

	@Override
	public Faction deserialize(JsonParser parser, DeserializationContext arg1) throws IOException,
			JsonProcessingException {
		return new FactionConverter().convertToEntityAttribute(parser.getText());
	}
}
