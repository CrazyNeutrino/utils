package org.meb.conquest.json;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.meb.conquest.db.converter.FactionConverter;
import org.meb.conquest.db.model.Faction;

public class FactionSerializer extends JsonSerializer<Faction> {

	@Override
	public void serialize(Faction value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {
		jgen.writeString(new FactionConverter().convertToDatabaseColumn(value));
	}
}
