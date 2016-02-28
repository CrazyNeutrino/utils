package org.meb.oneringdb.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonIgnoreProperties(value = { "id", "version" })
@JsonPropertyOrder(value = { "techName", "domain", "value", "recordState", "langItems" })
public interface JsonMixIn_StandardBase {

}
