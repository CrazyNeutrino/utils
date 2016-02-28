package org.meb.conquest.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonIgnoreProperties(value = { "id", "version" })
@JsonPropertyOrder(value = { "techName", "type", "faction", "cardSetBase", "warlordBase", "number", "quantity", "cost",
		"shield", "command", "attack", "hitPoints", "startingHandSize", "startingResources", "unique", "loyal",
		"octgnId", "illustrator", "recordState", "langItems" })
public interface JsonMixIn_CardBase {

}
