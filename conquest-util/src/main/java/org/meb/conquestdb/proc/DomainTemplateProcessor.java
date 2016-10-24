package org.meb.conquestdb.proc;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.meb.conquest.db.model.DomainBase;
import org.meb.conquest.db.model.DomainLang;

public class DomainTemplateProcessor implements DomainBaseProcessor {

	private String langCode;

	public DomainTemplateProcessor(String langCode) {
		this.langCode = langCode;
	}

	@Override
	public DomainBase process(DomainBase db) {
		DomainLang cl = db.getLangItems().get(langCode);
		DomainLang enCl = db.getLangItems().get("en");

		DomainBase output = db.cloneWithIdentity();
		DomainLang outputCl = new DomainLang(langCode);
		DomainLang outputEnCl = new DomainLang("en");

		if (StringUtils.isNotBlank(enCl.getDescription())) {
			outputEnCl.setDescription(enCl.getDescription());
			if (cl != null && StringUtils.isNotBlank(cl.getDescription())) {
				outputCl.setDescription(cl.getDescription());
			} else {
				outputCl.setDescription(" ");
			}
		}

		outputCl.setBase(output);
		output.setLangItems(new HashMap<String, DomainLang>());
		output.getLangItems().put(langCode, outputCl);
		output.getLangItems().put("en", outputEnCl);
		return output;
	}
}
