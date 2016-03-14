package org.meb.oneringdb.scan;

import org.meb.oneringdb.scan.model.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//private static final Logger log = LoggerFactory.getLogger(${enclosing_type}.class);
public class WriteLogCardHandler implements CardHandler {
	
	private static final Logger log = LoggerFactory.getLogger(WriteLogCardHandler.class);
	
	@Override
	public void handle(Card card) {
		log.info("card: type: {}, name: {}", card.getType(), card.getName());
	}
}