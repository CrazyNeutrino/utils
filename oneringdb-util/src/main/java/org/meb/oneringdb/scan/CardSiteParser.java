package org.meb.oneringdb.scan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.meb.oneringdb.db.model.CardType;
import org.meb.oneringdb.db.util.Utils;
import org.meb.oneringdb.scan.model.Card;
import org.meb.oneringdb.scan.model.CardProperty;
import org.meb.oneringdb.scan.model.CardSetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CardSiteParser {

	private static final Logger log = LoggerFactory.getLogger(CardSiteParser.class);

	private static final Map<String, String> mappings;
	private static final Map<String, String> replaceMap;

	static {
		mappings = new HashMap<String, String>();
		mappings.put("attack value", "attack");
		mappings.put("cost", "cost");
		mappings.put("command icons", "command");
		mappings.put("hit points", "hitPoints");
		mappings.put("illustrator", "illustrator");
		mappings.put("number", "number");
		mappings.put("quantity", "quantity");
		mappings.put("faction", "faction");
		mappings.put("starting hand size", "startingHandSize");
		mappings.put("starting resources", "startingResources");
		mappings.put("shields", "shield");
		mappings.put("traits", "trait");
		mappings.put("signature/loyalty", "signatureLoyalty");

		replaceMap = new HashMap<String, String>();
		for (String string : new String[] { "SPACE MARINE", "ASTRA MILITARUM", "TAU", "ELDAR", "DARK ELDAR", "CHAOS",
				"ORK", "RESOURCE" }) {
			replaceMap.put("[" + string + "]", "${" + Utils.toTechName(string) + "}");
		}
	}

	public void parseSites(List<CardSetInfo> csInfos, CardHandler handler) throws IOException {
		for (CardSetInfo csInfo : csInfos) {
			parseSite(csInfo, handler);
		}
	}

	public void parseSite(CardSetInfo csInfo, CardHandler handler) throws IOException {
		List<Document> docs = new ArrayList<Document>();
		log.info("parseSite(): base doc: {}", csInfo.getUrl());
		docs.add(Jsoup
				.connect(csInfo.getUrl())
				.timeout(20000)
				.header("Referer", "www.cardgamedb.com")
				.header("User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.86 Safari/537.36")
				.get());
		Elements aElems = docs.get(0).select("div.topic_controls li.page a");
		Iterator<Element> iter = aElems.iterator();
		while (iter.hasNext()) {
			String nextUrl = iter.next().attr("href");
			log.info("next doc: {}", nextUrl);
			docs.add(Jsoup.connect(nextUrl).timeout(20000).get());
		}

		int total = 0;
		for (int i = 0; i < docs.size(); i++) {
			List<Card> cards = parseCards(docs.get(i));
			log.info("parseSite(): site cards count: {}", cards.size());
			total += cards.size();
			for (Card card : cards) {
				card.setSetName(csInfo.getName());
				handler.handle(card);
			}
		}
		log.info("parseSite(): set cards total: {}", total);
	}

	private List<Card> parseCards(Document siteDoc) {
		List<Card> cards = new ArrayList<Card>();

		Elements tdCards = siteDoc.select("div.ipsBox table.ipb_table > tbody > tr > td");
		Iterator<Element> iter = tdCards.iterator();
		while (iter.hasNext()) {
			Element tdCard = iter.next();
			Element tdCardInfo = tdCard.select("tr:eq(0) > td").get(1);
			Card card = parseCard(tdCardInfo);
			if (card != null) {
				cards.add(card);
			}
		}

		Map<String, Card> warlords = new HashMap<>();
		for (Card card : cards) {
			if (card.getType().equals("warlord")) {
				warlords.put(card.getFaction(), card);
			}
		}

		for (Card card : cards) {
			System.out.println(Utils.toTechName(card.getName()));
			if (card.getSignatureLoyalty() != null) {
				if (card.getSignatureLoyalty().toLowerCase().contains("signature")) {
					card.setWarlordName(warlords.get(card.getFaction()).getName());
				}
				if (card.getSignatureLoyalty().toLowerCase().contains("loyal")) {
					card.setLoyal(true);
				}
			}
		}

		return cards;
	}

	private Card parseCard(Element tdCardInfo) {
		String name = tdCardInfo.child(0).text().trim();
		String typeString = tdCardInfo.child(2).nextSibling().toString().trim().toUpperCase();
		if (typeString.equals("ARMY UNIT")) {
			typeString = "ARMY";
		} else if (typeString.equals("WARLORD UNIT")) {
			typeString = "WARLORD";
		}
		CardType type = CardType.valueOf(typeString);

		log.debug("parseCard(): type: {}, name: {}", type, name);

		Card card = new Card(type.toString());
		switch (type) {
			case ALLY:
				parseAllyCardInfo(card, tdCardInfo);
				break;
			case ATTACHMENT:
				parseAttachmentCardInfo(card, tdCardInfo);
				break;
			case EVENT:
				parseEventCardInfo(card, tdCardInfo);
				break;
			default:
				card = null;
		}

		card.setType(Utils.toTechName(card.getType()));
		if (card.getFaction() != null) {
			String faction = Utils.toTechName(card.getFaction());
			if (faction.equals("orks")) {
				faction = "ork";
			}
			card.setFaction(faction);
		}

		log.debug("card: {}", card);

		return card;
	}

	private void parseCommonCardInfo(Card card, Element tdCardInfo) {
		String name = tdCardInfo.child(0).text().trim();
		if (name.contains("♦")) {
			card.setUnique(true);
			name = name.replace("♦", "").trim();
		}
		card.setName(name);
		List<CardProperty> properties = new ArrayList<CardProperty>();
		properties.addAll(extractKeyValueInfo(tdCardInfo, mappings));
		properties.addAll(extractTextProperties(tdCardInfo));
		setCardProperties(card, properties);
	}

	private void parseAllyCardInfo(Card card, Element tdCardInfo) {
		parseCommonCardInfo(card, tdCardInfo);
	}

	private void parseAttachmentCardInfo(Card card, Element tdCardInfo) {
		parseCommonCardInfo(card, tdCardInfo);
	}

	private void parseEventCardInfo(Card card, Element tdCardInfo) {
		parseCommonCardInfo(card, tdCardInfo);
	}

	private void parseWarlordCardInfo(Card card, Element tdCardInfo) {
		parseCommonCardInfo(card, tdCardInfo);
		card.setUnique(true);
	}

	private void parsePlanetCardInfo(Card card, Element tdCardInfo) {
		parseCommonCardInfo(card, tdCardInfo);
	}

	private void parseSupportCardInfo(Card card, Element tdCardInfo) {
		parseCommonCardInfo(card, tdCardInfo);
	}

	private void parseTokenCardInfo(Card card, Element tdCardInfo) {
		parseCommonCardInfo(card, tdCardInfo);
	}

	private List<CardProperty> extractKeyValueInfo(Element tdCardInfo, Map<String, String> mappings) {
		List<CardProperty> properties = new ArrayList<CardProperty>();
		Elements bInfoNames = tdCardInfo.select("> b");
		Iterator<Element> iter = bInfoNames.iterator();
		while (iter.hasNext()) {
			Element bInfoName = iter.next();
			String infoName = bInfoName.text();
			if (StringUtils.isBlank(infoName)) {
				continue;
			}

			infoName = infoName.trim().toLowerCase();
			infoName = StringUtils.removeEnd(infoName, ":").trim();

			if (mappings.containsKey(infoName)) {
				String infoValue = bInfoName.nextSibling().toString().trim();
				if (StringUtils.isBlank(infoValue)) {
					continue;
				}

				infoValue = StringUtils.removeStart(infoValue, ":").trim();

				String propertyName = mappings.get(infoName);
				properties.add(new CardProperty(propertyName, infoValue));
				log.debug("extractKeyValueInfo(): propertyName: {}, infoName: {}, infoValue: {}", new Object[] {
						propertyName, infoName, infoValue });
			}
		}
		return properties;
	}

	private void setCardProperties(Card card, List<CardProperty> properties) {
		for (CardProperty property : properties) {
			Object value = property.getValue();
			if (((String) value).equals("X")) {
				value = new Integer(-1);
			}

			try {
				String name = property.getName();
				log.debug("setCardProperties(): set property: {} <- {}", name, value);
				BeanUtils.setProperty(card, name, value);
				value = PropertyUtils.getProperty(card, name);
				log.debug("setCardProperties(): get property: {} -> {}", name, value);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private List<CardProperty> extractTextProperties(Element tdCardInfo) {

		String text = "";
		String flavourText = "";

		boolean textMode = false;
		int brCount = 0;

		Iterator<Node> iter = tdCardInfo.childNodes().iterator();
		while (iter.hasNext()) {
			Node node = iter.next();

			Element element = null;
			String elementTagName = null;
			if (node instanceof Element) {
				element = (Element) node;
				elementTagName = element.tagName();
				if (!textMode) {
					if (elementTagName.equals("span") || elementTagName.equals("strong") || elementTagName.equals("i")) {
						textMode = true;
					}
				}
			}

			if (!textMode) {
				continue;
			} else if (textMode && element != null && elementTagName.equals("b")) {
				break;
			}

			if (element == null) {
				text += node.toString();
			} else if (elementTagName.equals("br")) {
				text = StringUtils.stripEnd(text, null) + "\n";
			} else if (elementTagName.equals("i")) {
				flavourText = extractText(element);
				break;
			} else if (elementTagName.equals("span") || elementTagName.equals("strong")) {
				text += extractText(element);
			}
		}

		List<CardProperty> properties = new ArrayList<CardProperty>();
		if (StringUtils.isNotBlank(text)) {
			text = text.trim();
			Set<Entry<String, String>> entries = replaceMap.entrySet();
			for (Entry<String, String> entry : entries) {
				text = text.replace(entry.getKey(), entry.getValue());
			}
			properties.add(new CardProperty("text", StringEscapeUtils.unescapeHtml(text)));
		}
		if (StringUtils.isNotBlank(flavourText)) {
			flavourText = flavourText.trim();
			flavourText = flavourText.replace("”", "\"");
			flavourText = flavourText.replace("“", "\"");
			flavourText = flavourText.replace("‘", "\"");
			properties.add(new CardProperty("flavourText", StringEscapeUtils.unescapeHtml(flavourText)));
		}
		// CardProperty traitProperty = extractTraitProperty(tdCardInfo);
		// if (traitProperty != null) {
		// properties.add(traitProperty);
		// }
		return properties;
	}

	private String extractText(Element parent) {
		String text = "";

		Iterator<Node> iter = parent.childNodes().iterator();
		while (iter.hasNext()) {
			Node node = iter.next();
			if (node instanceof Element) {
				Element element = (Element) node;
				if (element.tagName().equals("br")) {
					text = StringUtils.stripEnd(text, null) + "\n";
				}
			} else {
				text += node.toString();
			}
		}

		log.debug("text: {}", text);

		return text;
	}

	// private String extractText(Element parent) {
	// String text = "";
	//
	// int brCount = 0;
	// Iterator<Node> iter = parent.childNodes().iterator();
	// while (iter.hasNext()) {
	// Node node = iter.next();
	// if (node instanceof Element) {
	// Element element = (Element) node;
	// if (element.tagName().equals("br")) {
	// brCount++;
	// }
	// } else {
	// String newText = node.toString().trim();
	// if (StringUtils.isNotBlank(text) && StringUtils.isNotBlank(newText)) {
	// text += StringUtils.repeat("\n", brCount);
	// }
	// text += newText;
	// brCount = 0;
	// }
	// }
	//
	// log.debug("text: {}", text);
	//
	// return text;
	// }

	// private CardProperty extractTraitProperty(Element tdCardInfo) {
	// String trait = null;
	//
	// Iterator<Element> iter = tdCardInfo.children().iterator();
	// while (iter.hasNext()) {
	// Element element = iter.next();
	// if (element.tagName().equals("b")) {
	// Node sibling = element.nextSibling();
	// Element elementSibling = element.nextElementSibling();
	// if (sibling == null) {
	// continue;
	// } else if (sibling == elementSibling ||
	// StringUtils.isBlank(sibling.toString())) {
	// trait = element.text().trim();
	// break;
	// }
	// }
	// }
	//
	// log.debug("trait: {}", trait);
	// CardProperty property = null;
	// if (StringUtils.isNotBlank(trait)) {
	// property = new CardProperty("trait",
	// StringEscapeUtils.unescapeHtml(trait));
	// }
	// return property;
	// }
}
