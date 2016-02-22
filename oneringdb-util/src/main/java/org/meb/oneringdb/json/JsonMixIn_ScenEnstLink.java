package org.meb.oneringdb.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonIgnoreProperties(value = { "id", "version" })
@JsonPropertyOrder(value = { "scenarioBase", "encounterSetBase", "sequence" })
public interface JsonMixIn_ScenEnstLink {

}
