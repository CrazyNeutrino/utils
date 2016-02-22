package org.meb.oneringdb.db.util;

import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;

import org.meb.oneringdb.db.model.IBase;
import org.meb.oneringdb.db.model.ILang;
import org.meb.oneringdb.db.util.PropUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JpaUtils {

	private static final Logger log = LoggerFactory.getLogger(JpaUtils.class);

	private JpaUtils() {

	}

	public static <B extends IBase<L>, L extends ILang<B>> void smartPersist(EntityManager em,
			B baseNew) {
		for (Entry<String, L> entry : baseNew.getLangItems().entrySet()) {
			entry.getValue().setLangCode(entry.getKey());
			entry.getValue().setBase(baseNew);
		}
		log.info("persist: {}", baseNew);
		em.persist(baseNew);
	}

	public static <B extends IBase<L>, L extends ILang<B>> void smartMerge(EntityManager em,
			B base, B baseNew) {
		smartMerge(em, base, baseNew, false);
	}

	public static <B extends IBase<L>, L extends ILang<B>> void smartMerge(EntityManager em,
			B base, B baseNew, boolean langItemsOnly) {

		if (!langItemsOnly) {
			PropUtils.copyJpaData(base, baseNew);
		}

		Map<String, L> langItems = baseNew.getLangItems();
		if (langItems != null) {
			for (Entry<String, L> entry : langItems.entrySet()) {
				String langCode = entry.getKey();
				L langNew = entry.getValue();
				langNew.setLangCode(langCode);
				L lang = base.getLangItems().get(langCode);
				if (lang == null) {
					lang = langNew;
					lang.setBase(base);
					base.getLangItems().put(langCode, lang);
				} else {
					PropUtils.copyJpaData(lang, langNew);
				}
			}
		}
		log.info("merge: {}", base);
		em.merge(base);
	}
}
