package org.meb.conquest.scan.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Card {

	private Integer attack;
	private Integer command;
	private Integer cost;
	private Integer shield;
	private String faction;
	private Integer hitPoints;
	private String illustrator;
	private String signatureLoyalty;
	private String name;
	private String trait;
	private String type;
	private String setName;
	private String warlordName;
	private Integer number;
	private Integer quantity;
	private Integer startingHandSize;
	private Integer startingResources;
	private String techName;
	private String text;
	private String flavourText;
	private Boolean unique = false;
	private Boolean loyal = false;

	public Card(String type) {
		this.type = type;
	}
}
