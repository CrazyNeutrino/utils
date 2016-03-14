package org.meb.conquest.scan;

import lombok.Getter;

import org.meb.conquest.scan.model.Card;

public class CountCardHandler implements CardHandler {

	@Getter
	private int count = 0;

	@Override
	public void handle(Card card) {
		count++;
	}
}
