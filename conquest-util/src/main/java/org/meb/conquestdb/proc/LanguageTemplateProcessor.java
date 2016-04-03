package org.meb.conquestdb.proc;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.meb.conquest.db.model.CardBase;
import org.meb.conquest.db.model.CardLang;

public class LanguageTemplateProcessor implements CardBaseProcessor {

	private String langCode;

	public LanguageTemplateProcessor(String langCode) {
		this.langCode = langCode;
	}

	@Override
	public CardBase process(CardBase cb) {
		CardLang cl = cb.getLangItems().get(langCode);
		CardLang enCl = cb.getLangItems().get("en");

		CardBase output = cb.cloneWithIdentity();
		CardLang outputCl = new CardLang(langCode);
		CardLang outputEnCl = new CardLang("en");

		if (StringUtils.isNotBlank(enCl.getName())) {
			outputEnCl.setName(enCl.getName());
			if (cl != null && StringUtils.isNotBlank(cl.getName())) {
				outputCl.setName(cl.getName());
			} else {
				outputCl.setName(" ");
			}
		}

		if (StringUtils.isNotBlank(enCl.getTrait())) {
			outputEnCl.setTrait(enCl.getTrait());
			if (cl != null && StringUtils.isNotBlank(cl.getTrait())) {
				outputCl.setTrait(cl.getTrait());
			} else {
				outputCl.setTrait(" ");
			}
		}

		if (StringUtils.isNotBlank(enCl.getKeyword())) {
			outputEnCl.setKeyword(enCl.getKeyword());
			if (cl != null && StringUtils.isNotBlank(cl.getKeyword())) {
				outputCl.setKeyword(cl.getKeyword());
			} else {
				outputCl.setKeyword(" ");
			}
		}
		
		outputCl.setBase(output);
		output.setLangItems(new HashMap<String, CardLang>());
		output.getLangItems().put(langCode, outputCl);
		output.getLangItems().put("en", outputEnCl);
		return output;
	}
}
