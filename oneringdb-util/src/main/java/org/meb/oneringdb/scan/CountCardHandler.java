package org.meb.oneringdb.scan;

import org.meb.oneringdb.scan.model.Card;

import lombok.Getter;

public class CountCardHandler implements CardHandler {

	@Getter
	private int count = 0;

	@Override
	public void handle(Card card) {
		count++;
	}
}
