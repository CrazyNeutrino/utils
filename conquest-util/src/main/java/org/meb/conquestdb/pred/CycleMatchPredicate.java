package org.meb.conquestdb.pred;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.Predicate;
import org.meb.conquest.db.model.CardBase;
import org.meb.conquest.db.model.CycleBase;

public class CycleMatchPredicate implements Predicate<CardBase> {
	
	private Set<String> cycleNames = new HashSet<>(); 
	
	public CycleMatchPredicate(String... crstNames) {
		for (String crstName : crstNames) {
			this.cycleNames.add(crstName);
		}
	}

	@Override
	public boolean evaluate(CardBase cb) {
		CycleBase ccb = cb.getCardSetBase().getCycleBase();
		if (ccb == null) {
			return false;
		} else {
			return cycleNames.contains(ccb.getTechName());
		}
	}
}
