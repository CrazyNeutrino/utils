package org.meb.conquest.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonIgnoreProperties(value = { "version", "cardSetBaseItems" })
@JsonPropertyOrder(value = { "code", "techName" })
public interface JsonMixIn_CycleBase {

}
