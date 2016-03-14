package org.meb.oneringdb.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonIgnoreProperties(value = { "id", "version" })
@JsonPropertyOrder(value = { "techName", "number", "type", "sphere", "unique", "cardSetBase",
		"encounterSetBase", "threatCost", "resourceCost", "engagementCost", "willpower", "threat",
		"attack", "defense", "hitPoints", "questPoints", "victoryPoints", "quantity", "illustrator",
		"octgnId", "recordState", "langItems" })
public interface JsonMixIn_CardBase {

}
