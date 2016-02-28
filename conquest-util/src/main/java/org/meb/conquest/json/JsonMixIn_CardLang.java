package org.meb.conquest.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonIgnoreProperties(value = { "id", "version", "base", "langCode" })
@JsonPropertyOrder(value = { "name", "trait", "keyword", "text" })
public interface JsonMixIn_CardLang {

}
