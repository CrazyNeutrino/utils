package org.meb.conquestdb.db;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.meb.conquest.db.model.CardBase;
import org.meb.conquest.db.model.CardLang;
import org.meb.conquest.db.model.CardType;
import org.meb.conquest.db.model.IBase;
import org.meb.conquest.db.model.ILang;
import org.meb.conquest.db.model.loc.Card;
import org.meb.conquest.db.util.Utils;
import org.meb.conquestdb.db.util.JpaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.Setter;

public class OctgnDataLoader extends AbstractLoader {

	private static final Logger log = LoggerFactory.getLogger(OctgnDataLoader.class);

	protected static final String DATA_BASE;

	static {
		String home = System.getProperty("home");
		if (StringUtils.isBlank(home)) {
			throw new IllegalStateException("Home not set");
		}
		if (home.trim().endsWith("/")) {
			DATA_BASE = home + "data/";
		} else {
			DATA_BASE = home + "/data/";
		}

		AbstractLoader.mkdir(DATA_BASE);
	}

	@Setter
	private boolean langItemsOnly = false;

	public static void main(String[] args)
			throws IOException, ParserConfigurationException, SAXException {
		OctgnDataLoader loader = new OctgnDataLoader();
		try {
			if (args[0].equals("--update-octgn-ids")) {
				loader.updateOctgnIdsNew();
			} else if (args[0].equals("--update-octgn-texts")) {
				loader.updateOctgnTexts();
			} else {
				throw new IllegalArgumentException("Invalid argument");
			}
		} finally {
			loader.cleanUp();
		}
	}

	public OctgnDataLoader() {

	}

	public void updateOctgnIdsNew() throws ParserConfigurationException, SAXException, IOException {
		emInitialize();

		beginTransaction();

		StringBuilder myLog = new StringBuilder();
		Map<String, String> map = new HashMap<>();
		final String crstOrCycleName = "Death World Cycle";
		final String crstOrCycleTechName = Utils.toTechName(crstOrCycleName);

		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(DATA_BASE + "/octgn/ids.xml");
		NodeList setNodes = doc.getElementsByTagName("set");
		Element setElem = null;
		for (int i = 0; i < setNodes.getLength(); i++) {
			setElem = (Element) setNodes.item(i);
			if (setElem.getAttribute("name").equals(crstOrCycleName)) {
				break;
			} else {
				setElem = null;
			}
		}

		if (setElem == null) {
			throw new IllegalStateException("Cycle or set not found");
		}

		NodeList cardNodes = setElem.getElementsByTagName("card");
		for (int i = 0; i < cardNodes.getLength(); i++) {
			Element cardElem = (Element) cardNodes.item(i);
			String id = cardElem.getAttribute("id");
			String name = cardElem.getAttribute("name");
			String techName = Utils.toTechName(name);
			System.out.println(id + "\t" + techName + "\t" + name);
			if (StringUtils.isBlank(id)) {
				continue;
			}
			if (map.containsKey(techName)) {
				throw new IllegalStateException("Duplicate id");
			}
			map.put(techName, id);
		}

		Predicate<CardBase> keepPredicate = new Predicate<CardBase>() {

			@Override
			public boolean evaluate(CardBase cb) {
				String crstTechName = cb.getCardSetBase().getTechName();
				String cycleTechName = null;
				if (cb.getCardSetBase().getCycleBase() != null) {
					cycleTechName = cb.getCardSetBase().getCycleBase().getTechName();
				}
				return (crstOrCycleTechName.equals(crstTechName)
						|| crstOrCycleTechName.equals(cycleTechName))
						&& CardType.PLANET != cb.getType() && CardType.TOKEN != cb.getType();
			}

		};
		List<CardBase> cbList = readCardsFromDatabase(keepPredicate);

		String format = "%-25s";
		int[] stats = new int[4];

		for (CardBase cb : cbList) {
			String techName = cb.getTechName();
			String newOctgnId = map.get(techName);
			String oldOctgnId = cb.getOctgnId();

			if (StringUtils.isBlank(newOctgnId)) {
				stats[0]++;
				myLog.append(String.format(format, "missing new octgn id: "));
				myLog.append(techName).append("\n");
			} else if (oldOctgnId == null) {
				stats[1]++;
				cb.setOctgnId(map.get(techName));
				myLog.append(String.format(format, "ids update: "));
				myLog.append(techName).append(" -> ").append(newOctgnId).append("\n");
				try {
					em.flush();
				} catch (RuntimeException e) {
					System.out.println(myLog.toString());
					throw e;
				}
			} else if (!newOctgnId.equals(oldOctgnId)) {
				stats[2]++;
				myLog.append(String.format(format, "ids mismatch: "));
				myLog.append(techName).append(", was: ").append(oldOctgnId).append(", is: ")
						.append(newOctgnId).append("\n");
			} else {
				stats[3]++;
				myLog.append(String.format(format, "ids match: "));
				myLog.append(techName).append("\n");
			}
		}

		myLog.append("\n[SUMMARY]\n");
		myLog.append(String.format(format, "missing new octgn id: ")).append(stats[0]).append("\n");
		myLog.append(String.format(format, "ids update: ")).append(stats[1]).append("\n");
		myLog.append(String.format(format, "ids mismatch: ")).append(stats[2]).append("\n");
		myLog.append(String.format(format, "ids match: ")).append(stats[3]).append("\n");

		System.out.println(myLog.toString());

		endTransaction(true);
	}

	public void updateOctgnIds() {
		beginTransaction();

		StringBuilder myLog = new StringBuilder();
		Map<String, String> map = new HashMap<>();
		final String crstOrCycleTechName = "planetfall-cycle";

		try {
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = db.parse(DATA_BASE + "/octgn/04-" + crstOrCycleTechName + "/set.xml");
			NodeList cardNodes = doc.getElementsByTagName("card");
			for (int i = 0; i < cardNodes.getLength(); i++) {
				Element cardElem = (Element) cardNodes.item(i);
				String id = cardElem.getAttribute("id");
				String name = cardElem.getAttribute("name");
				String techName = Utils.toTechName(name);
				System.out.println(id + "\t" + techName + "\t" + name);
				if (StringUtils.isBlank(id)) {
					continue;
				}
				if (map.containsKey(techName)) {
					throw new IllegalStateException("Duplicate id");
				}
				map.put(techName, id);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		Predicate<CardBase> keepPredicate = new Predicate<CardBase>() {

			@Override
			public boolean evaluate(CardBase cb) {
				String crstTechName = cb.getCardSetBase().getTechName();
				String cycleTechName = null;
				if (cb.getCardSetBase().getCycleBase() != null) {
					cycleTechName = cb.getCardSetBase().getCycleBase().getTechName();
				}
				return (crstOrCycleTechName.equals(crstTechName)
						|| crstOrCycleTechName.equals(cycleTechName))
						&& CardType.PLANET != cb.getType() && CardType.TOKEN != cb.getType();
			}

		};
		List<CardBase> cbList = readCardsFromDatabase(keepPredicate);

		String format = "%-25s";
		int[] stats = new int[4];

		for (CardBase cb : cbList) {
			String techName = cb.getTechName();
			String newOctgnId = map.get(techName);
			String oldOctgnId = cb.getOctgnId();

			if (StringUtils.isBlank(newOctgnId)) {
				stats[0]++;
				myLog.append(String.format(format, "missing new octgn id: "));
				myLog.append(techName).append("\n");
			} else if (oldOctgnId == null) {
				stats[1]++;
				cb.setOctgnId(map.get(techName));
				myLog.append(String.format(format, "ids update: "));
				myLog.append(techName).append(" -> ").append(newOctgnId).append("\n");
				try {
					em.flush();
				} catch (RuntimeException e) {
					System.out.println(myLog.toString());
					throw e;
				}
			} else if (!newOctgnId.equals(oldOctgnId)) {
				stats[2]++;
				myLog.append(String.format(format, "ids mismatch: "));
				myLog.append(techName).append(", was: ").append(oldOctgnId).append(", is: ")
						.append(newOctgnId).append("\n");
			} else {
				stats[3]++;
				myLog.append(String.format(format, "ids match: "));
				myLog.append(techName).append("\n");
			}
		}

		myLog.append("\n[SUMMARY]\n");
		myLog.append(String.format(format, "missing new octgn id: ")).append(stats[0]).append("\n");
		myLog.append(String.format(format, "ids update: ")).append(stats[1]).append("\n");
		myLog.append(String.format(format, "ids mismatch: ")).append(stats[2]).append("\n");
		myLog.append(String.format(format, "ids match: ")).append(stats[3]).append("\n");

		System.out.println(myLog.toString());

		endTransaction(true);
	}

	public void updateOctgnTexts() {
		beginTransaction();

		StringBuilder myLog = new StringBuilder();
		Map<String, Card> octgnCards = new HashMap<>();
		// final String crstOrCycleTechName = "warlord-cycle";
		// final String crstTechName = "descendants-of-isha";

		// final String crstOrCycleTechName = "the-great-devourer";
		// final String crstTechName = "the-great-devourer";

		final String crstOrCycleTechName = "planetfall-cycle";
		// final String crstTechName = "decree-of-ruin";
		final String crstTechName = "boundless-hate";

		try {
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = db
					.parse(DATA_BASE + "/card-hunter/pl/04-" + crstOrCycleTechName + "/set.xml");
			NodeList cardNodes = doc.getElementsByTagName("card");
			for (int i = 0; i < cardNodes.getLength(); i++) {
				Element cardElem = (Element) cardNodes.item(i);
				Card card = new Card();
				card.setOctgnId(cardElem.getAttribute("id"));
				card.setName(cardElem.getAttribute("name"));
				card.setTechName(Utils.toTechName(card.getName()));
				card.setNumber(Integer.valueOf(cardElem.getAttribute("id").substring(3, 6)));
				NodeList propertyNodes = cardElem.getElementsByTagName("property");
				for (int j = 0; j < propertyNodes.getLength(); j++) {
					Element propertyElem = (Element) propertyNodes.item(j);
					if (propertyElem.getAttribute("name").equals("Text")) {
						card.setText(StringUtils.trimToNull(propertyElem.getAttribute("value")));
					}
					if (propertyElem.getAttribute("name").equals("Traits")) {
						card.setTrait(StringUtils.trimToNull(propertyElem.getAttribute("value")));
					}
				}
				System.out.println(card.getId() + "\t" + card.getNumber() + "\t" + card.getName()
						+ "\t" + card.getText());
				if (octgnCards.containsKey(card.getTechName())) {
					throw new IllegalStateException("Duplicate id");
				}
				octgnCards.put(card.getNumber().toString(), card);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Predicate<CardBase> keepPredicate = new Predicate<CardBase>() {

			@Override
			public boolean evaluate(CardBase cb) {
				String dbCrstTechName = cb.getCardSetBase().getTechName();
				String dbCycleTechName = null;
				if (cb.getCardSetBase().getCycleBase() != null) {
					dbCycleTechName = cb.getCardSetBase().getCycleBase().getTechName();
				}
				// return (crstOrCycleTechName.equals(crstTechName) ||
				// crstOrCycleTechName.equals(cycleTechName))
				// && CardType.PLANET != cb.getType() && CardType.TOKEN !=
				// cb.getType();
				return dbCrstTechName.equals(crstTechName) && CardType.PLANET != cb.getType()
						&& CardType.TOKEN != cb.getType();
			}

		};
		List<CardBase> cbList = readCardsFromDatabase(keepPredicate);

		String format = "%-25s";
		int[] stats = new int[9];

		for (CardBase cb : cbList) {
			String techName = cb.getNumber().toString();
			Card octgnCard = octgnCards.get(techName);

			String langCode = "pl";
			CardLang cl = cb.getLangItems().get(langCode);
			if (cl == null) {
				cl = new CardLang(langCode);
				cl.setBase(cb);
				cb.getLangItems().put(langCode, cl);
				cl.setRecordState("A");
				cl.setImageLangCode("en");
			}

			if (StringUtils.isNotBlank(cl.getText())) {
				stats[0]++;
				myLog.append(techName).append(": not updating - target not blank").append("\n");
			} else if (StringUtils.isBlank(octgnCard.getText())) {
				stats[1]++;
				myLog.append(techName).append(": not updating - source blank").append("\n");
			} else {
				stats[2]++;
				myLog.append(techName).append(": updating").append("\n");
				cl.setText(octgnCard.getText());
			}

			if (StringUtils.isNotBlank(cl.getTrait())) {
				stats[3]++;
				myLog.append(techName).append(": not updating - target not blank").append("\n");
			} else if (StringUtils.isBlank(octgnCard.getTrait())) {
				stats[4]++;
				myLog.append(techName).append(": not updating - source blank").append("\n");
			} else {
				stats[5]++;
				myLog.append(techName).append(": updating").append("\n");
				cl.setTrait(octgnCard.getTrait());
			}

			if (StringUtils.isNotBlank(cl.getName())) {
				stats[6]++;
				myLog.append(techName).append(": not updating - target not blank").append("\n");
			} else if (StringUtils.isBlank(octgnCard.getName())) {
				stats[7]++;
				myLog.append(techName).append(": not updating - source blank").append("\n");
			} else {
				stats[8]++;
				myLog.append(techName).append(": updating").append("\n");
				cl.setName(octgnCard.getName());
			}
		}

		myLog.append("\n[SUMMARY text]\n");
		myLog.append(String.format(format, "target not blank: ")).append(stats[0]).append("\n");
		myLog.append(String.format(format, "source blank: ")).append(stats[1]).append("\n");
		myLog.append(String.format(format, "updated: ")).append(stats[2]).append("\n");

		myLog.append("\n[SUMMARY trait]\n");
		myLog.append(String.format(format, "target not blank: ")).append(stats[3]).append("\n");
		myLog.append(String.format(format, "source blank: ")).append(stats[4]).append("\n");
		myLog.append(String.format(format, "updated: ")).append(stats[5]).append("\n");

		System.out.println(myLog.toString());

		endTransaction(false);
	}

	private <B extends IBase<L>, L extends ILang<B>> void persist(B target) {
		JpaUtils.smartPersist(em, target);
	}

	private <B extends IBase<L>, L extends ILang<B>> void merge(B target, B source) {
		JpaUtils.smartMerge(em, target, source, langItemsOnly);
	}
}
