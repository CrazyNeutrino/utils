package org.meb.oneringdb.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonIgnoreProperties(value = { "id", "version" })
@JsonPropertyOrder(value = { "techName", "number", "cardSetBase", "encounterSetBase", "type", "sphere", "cost", "startingThreat",
		"engageThreat", "willpower", "threat", "attack", "defense", "hitPoints", "unique", "quantity", "illustrator",
		"octgnId", "recordState", "langItems" })
public interface JsonMixIn_CardBase {

}
