package org.meb.oneringdb.scan;

import java.util.ArrayList;
import java.util.List;

import org.meb.oneringdb.scan.model.Card;

public class CardHandlerChain implements CardHandler {
	
	private List<CardHandler> handlers;
	
	public CardHandlerChain() {
		handlers = new ArrayList<CardHandler>();
	}

	@Override
	public void handle(Card card) {
		for (CardHandler handler : handlers) {
			handler.handle(card);
		}
	}
	
	public void addCardHandler(CardHandler handler) {
		handlers.add(handler);
	}
}
