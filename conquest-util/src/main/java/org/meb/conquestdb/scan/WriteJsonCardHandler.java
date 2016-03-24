package org.meb.conquestdb.scan;

import java.io.StringWriter;

import org.apache.commons.beanutils.PropertyUtils;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.meb.conquest.db.util.Utils;
import org.meb.conquestdb.json.JsonUtils;
import org.meb.conquestdb.scan.model.Card;

public class WriteJsonCardHandler implements CardHandler {

	private final String[] BASE_PROPS = { "techName", "type", "faction", "number", "quantity", "cost", "shield",
			"command", "attack", "hitPoints", "startingHandSize", "startingResources", "unique", "loyal", "illustrator" };
	private final String[] LANG_PROPS = { "name", "trait", "text", "flavourText" };

	private JsonNodeFactory jsonFactory;
	private ArrayNode cardsNode;
	private String langCode;
	private int count = 0;

	public WriteJsonCardHandler(String langCode) {
		this.langCode = langCode;
		jsonFactory = JsonNodeFactory.instance;
		cardsNode = jsonFactory.arrayNode();
	}

	@Override
	public void handle(Card card) {
		card.setTechName(Utils.toTechName(card.getName()));

		ObjectNode cardNode = cardsNode.addObject();
		for (String propertyName : BASE_PROPS) {
			try {
				Object propertyValue = PropertyUtils.getProperty(card, propertyName);
				if (propertyValue != null) {
					cardNode.put(propertyName, propertyValue.toString());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		cardNode.putObject("cardSetBase").put("techName", Utils.toTechName(card.getSetName()));
		if (card.getWarlordName() != null) {
			cardNode.putObject("warlordBase").put("techName", Utils.toTechName(card.getWarlordName()));
		}
		cardNode.put("recordState", "A");

		ObjectNode enNode = cardNode.putObject("langItems").putObject(langCode);
		for (String propertyName : LANG_PROPS) {
			try {
				Object propertyValue = PropertyUtils.getProperty(card, propertyName);
				if (propertyValue != null) {
					enNode.put(propertyName, propertyValue.toString());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		enNode.put("recordState", "A");
		count++;
	}

	public int getCount() {
		return count;
	}

	@Override
	public String toString() {
		StringWriter writer = new StringWriter();
		try {
			JsonUtils.createJsonGenerator(writer).writeTree(cardsNode);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return writer.toString();
	}
}
