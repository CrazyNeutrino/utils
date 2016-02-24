package org.meb.oneringdb.json;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.meb.oneringdb.db.model.EncounterSetBase;
import org.meb.oneringdb.db.model.ScenEnstLink;
import org.meb.oneringdb.db.model.ScenarioBase;

public class ScenEnstLinkSerializer extends JsonSerializer<ScenEnstLink> {

	@Override
	public void serialize(ScenEnstLink value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {
		
		jgen.writeStartObject();
		
		ScenarioBase sb = value.getScenarioBase();
		if (sb != null && sb.getTechName() != null) {
			jgen.writeObjectField("scenario", sb.getTechName());
		}
		EncounterSetBase eb = value.getEncounterSetBase();
		if (eb != null && eb.getTechName() != null) {
			jgen.writeObjectField("encounterSet", eb.getTechName());
		}
		if (value.getSequence() != null) {
			jgen.writeObjectField("sequence", value.getSequence());
		}
		
		jgen.writeEndObject();
	}
}
