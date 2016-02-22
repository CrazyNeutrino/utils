package org.meb.oneringdb.json;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.meb.oneringdb.db.converter.SphereConverter;
import org.meb.oneringdb.db.model.Sphere;

public class SphereDeserializer extends JsonDeserializer<Sphere> {

	@Override
	public Sphere deserialize(JsonParser parser, DeserializationContext arg1)
			throws IOException, JsonProcessingException {
		return new SphereConverter().convertToEntityAttribute(parser.getText());
	}
}
