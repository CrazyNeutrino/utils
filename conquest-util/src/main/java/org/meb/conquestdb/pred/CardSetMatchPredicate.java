package org.meb.conquestdb.pred;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.Predicate;
import org.meb.conquest.db.model.CardBase;

public class CardSetMatchPredicate implements Predicate<CardBase> {
	
	private Set<String> crstNames = new HashSet<>(); 
	
	public CardSetMatchPredicate(String... crstNames) {
		for (String crstName : crstNames) {
			this.crstNames.add(crstName);
		}
	}

	@Override
	public boolean evaluate(CardBase cb) {
		return crstNames.contains(cb.getCardSetBase().getTechName());
	}
}
