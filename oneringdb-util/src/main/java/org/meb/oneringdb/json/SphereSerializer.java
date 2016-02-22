package org.meb.oneringdb.json;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.meb.oneringdb.db.converter.SphereConverter;
import org.meb.oneringdb.db.model.Sphere;

public class SphereSerializer extends JsonSerializer<Sphere> {

	@Override
	public void serialize(Sphere value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {
		jgen.writeString(new SphereConverter().convertToDatabaseColumn(value));
	}
}
