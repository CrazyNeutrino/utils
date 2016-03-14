package org.meb.oneringdb.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonIgnoreProperties(value = { "id", "version", "cardSetBaseItems" })
@JsonPropertyOrder(value = { "techName", "recordState" })
public interface JsonMixIn_CycleBase {

}
