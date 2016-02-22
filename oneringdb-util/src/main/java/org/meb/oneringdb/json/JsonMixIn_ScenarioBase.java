package org.meb.oneringdb.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonIgnoreProperties(value = { "id", "version", "scenEnstLinkItems" })
@JsonPropertyOrder(value = { "techName", "sequence", "difficulty", "recordState" })
public interface JsonMixIn_ScenarioBase {

}
