package org.meb.oneringdb.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonIgnoreProperties(value = { "id", "version" })
@JsonPropertyOrder(value = { "techName", "typeCode", "cycleBase", "sequence", "released",
		"releaseDate", "recordState", "langItems" })
public interface JsonMixIn_CardSetBase {

}
