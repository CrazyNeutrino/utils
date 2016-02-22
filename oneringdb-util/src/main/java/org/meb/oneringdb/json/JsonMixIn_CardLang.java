package org.meb.oneringdb.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonIgnoreProperties(value = { "id", "version", "base", "langCode" })
@JsonPropertyOrder(value = { "name", "trait", "keyword", "text", "flavourText", "faqVersion", "faqDate", "faqText",
		"imageLangCode", "recordState" })
public interface JsonMixIn_CardLang {

}
