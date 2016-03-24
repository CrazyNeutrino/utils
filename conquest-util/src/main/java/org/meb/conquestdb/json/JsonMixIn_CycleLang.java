package org.meb.conquestdb.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(value = { "id", "version", "base", "langCode" })
public interface JsonMixIn_CycleLang {

}
