package org.meb.conquest.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(value = { "id", "version" })
public interface JsonMixIn_StandardBase {

}
