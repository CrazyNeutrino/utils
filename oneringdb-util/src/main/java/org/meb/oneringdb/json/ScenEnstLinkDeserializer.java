package org.meb.oneringdb.json;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.meb.oneringdb.db.model.EncounterSetBase;
import org.meb.oneringdb.db.model.ScenEnstLink;
import org.meb.oneringdb.db.model.ScenarioBase;

public class ScenEnstLinkDeserializer extends JsonDeserializer<ScenEnstLink> {

	@Override
	public ScenEnstLink deserialize(JsonParser parser, DeserializationContext context)
			throws IOException, JsonProcessingException {
		ScenEnstLink scenEnstLink = new ScenEnstLink();

		JsonToken token = null;
		while ((token = parser.nextToken()) != null) {
			if (!token.isScalarValue()) {
				continue;
			}
			
			String name = parser.getCurrentName();
			if ("scenario".equals(name)) {
				scenEnstLink.setScenarioBase(new ScenarioBase(parser.getText()));
			} else if ("encounterSet".equals(name)) {
				scenEnstLink.setEncounterSetBase(new EncounterSetBase(parser.getText()));
			} else if ("sequence".equals(name)) {
				scenEnstLink.setSequence(new Integer(parser.getIntValue()));
			}
		}

		return scenEnstLink;
	}
}
