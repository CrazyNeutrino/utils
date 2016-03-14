package org.meb.oneringdb.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonIgnoreProperties(value = { "id", "version", "scenEnstLinkItems" })
@JsonPropertyOrder(value = { "techName", "cardSetBase", "recordState" })
public interface JsonMixIn_EncounterSetBase {

}
