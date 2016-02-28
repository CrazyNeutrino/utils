package org.meb.oneringdb.db;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonProcessingException;
import org.meb.oneringdb.db.dao.JpaDao;
import org.meb.oneringdb.db.model.CardBase;
import org.meb.oneringdb.db.model.CardLang;
import org.meb.oneringdb.db.model.CardSetBase;
import org.meb.oneringdb.db.model.CycleBase;
import org.meb.oneringdb.db.model.DomainBase;
import org.meb.oneringdb.db.model.EncounterSetBase;
import org.meb.oneringdb.db.model.IBase;
import org.meb.oneringdb.db.model.ILang;
import org.meb.oneringdb.db.model.ScenEnstLink;
import org.meb.oneringdb.db.model.ScenarioBase;
import org.meb.oneringdb.db.model.loc.Card;
import org.meb.oneringdb.db.query.CardQuery;
import org.meb.oneringdb.db.util.JpaUtils;
import org.meb.oneringdb.db.util.Utils;
import org.meb.oneringdb.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import lombok.Setter;

public class JsonDataLoader extends AbstractLoader {

	private static final Logger log = LoggerFactory.getLogger(JsonDataLoader.class);

	protected static final String DATA_BASE;
	protected static final String JSON_BASE;
	protected static final String IMAGE_BASE;

	static {
		String home = System.getProperty("home");
		if (StringUtils.isBlank(home)) {
			throw new IllegalStateException("Home not set");
		}
		if (home.trim().endsWith("/")) {
			DATA_BASE = home + "data/";
			IMAGE_BASE = home + "image/";
		} else {
			DATA_BASE = home + "/data/";
			IMAGE_BASE = home + "/image/";
		}
		JSON_BASE = DATA_BASE + "json-domain/";
		createDirectory(DATA_BASE);
		createDirectory(IMAGE_BASE);
		createDirectory(JSON_BASE);
	}

	private static void createDirectory(String name) {
		if (!new File(name).exists()) {
			new File(name).mkdir();
		}
	}

	private static final String FILE_DOMAIN = JSON_BASE + "domain.json";
	private static final String FILE_CYCLE = JSON_BASE + "cycle.json";
	private static final String FILE_CARD_SET = JSON_BASE + "card_set.json";
	private static final String FILE_ENCO_SET = JSON_BASE + "encounter_set.json";
	private static final String FILE_SCENARIO = JSON_BASE + "scenario.json";
	private static final String FILE_SCEN_ENST_LINK = JSON_BASE + "scen_enst_link.json";
	private static final String FILE_CARD = JSON_BASE + "card.json";

	private static final boolean PROC_DOMAIN = Boolean
			.valueOf(System.getProperty("data.proc.domain"));
	private static final boolean PROC_CYCLE = Boolean
			.valueOf(System.getProperty("data.proc.cycle"));
	private static final boolean PROC_CARD_SET = Boolean
			.valueOf(System.getProperty("data.proc.cardset"));
	private static final boolean PROC_ENCO_SET = Boolean
			.valueOf(System.getProperty("data.proc.encoset"));
	private static final boolean PROC_SCENARIO = Boolean
			.valueOf(System.getProperty("data.proc.scenario"));
	private static final boolean PROC_SCEN_ENST = Boolean
			.valueOf(System.getProperty("data.proc.scenenst"));
	private static final boolean PROC_CARD = Boolean.valueOf(System.getProperty("data.proc.card"));
	private static final boolean DEV_ENV = Boolean.valueOf(System.getProperty("dev.env"));

	@Setter
	private boolean langItemsOnly = false;

	public static void main(String[] args) throws IOException {
		JsonDataLoader loader = new JsonDataLoader();
		try {
			if (args[0].equals("--import-json")) {
				loader.importDatabaseFromJson();
			} else if (args[0].equals("--export-json")) {
				loader.exportDatabaseToJson();
			} else if (args[0].equals("--rename") && DEV_ENV) {
				loader.rename();
			} else if (args[0].equals("--update-octgn-ids")) {
				loader.updateOctgnIds();
			} else if (args[0].equals("--update-octgn-texts")) {
				loader.updateOctgnTexts();
			} else if (args[0].equals("--update-trait-keyword")) {
				loader.updateTraitAndKeyword();
			} else {
				throw new IllegalArgumentException("Invalid argument");
			}
		} finally {
			loader.emFinalize();
		}
	}

	private void rename() {
		String dirName = DATA_BASE + "../image/_raw_/card/pl/galakta/04-zc";
		File dir = new File(dirName);
		String[] fileNames = dir.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				// return name.matches("WL_[\\p{Digit}]{1,4}\\.((jpg)|(png))");
				return name.matches("[\\p{Digit}]{1,3}[^b]+\\.(png)");
			}
		});

		JpaDao<Card, CardQuery> dao = new JpaDao<>(em);
		Card card = new Card();
		card.setCrstTechName("zogworts-curse");
		List<Card> cards = dao.find(card);
		HashMap<Integer, Card> map = new HashMap<Integer, Card>();
		MapUtils.populateMap(map, cards, new Transformer<Card, Integer>() {

			@Override
			public Integer transform(Card input) {
				return input.getNumber();
			}
		});

		for (String fileName : fileNames) {
			int underscore = fileName.indexOf('_');
			int dot = fileName.indexOf('.') - 1;
			Integer number = Integer.valueOf(fileName.substring(0, 3));
			String newFileName = StringUtils.leftPad(number.toString(), 3, '0') + "-"
					+ map.get(number).getTechName();
			if (fileName.endsWith("jpg")) {
				newFileName += ".jpg";
			} else if (fileName.endsWith("png")) {
				newFileName += ".png";
			}
			System.out.println(fileName + " -> " + newFileName);
			File file = new File(dir, fileName);
			file.renameTo(new File(dir, newFileName));
		}
	}

	public void importDatabaseFromJson() throws IOException {
		emInitialize();

		langItemsOnly = false;

		beginTransaction();
		if (PROC_DOMAIN) {
			writeDomainsToDatabase();
		}
		if (PROC_CYCLE) {
			writeCyclesToDatabase();
		}
		if (PROC_CARD_SET) {
			writeCardSetsToDatabase();
		}
		if (PROC_ENCO_SET) {
			writeEncounterSetsToDatabase();
		}
		if (PROC_SCENARIO) {
			writeScenariosToDatabase();
		}
		if (PROC_SCEN_ENST) {
			writeScenEnstLinksToDatabase();
		}
		if (PROC_CARD) {
			writeCardsToDatabase();
		}
		endTransaction(true);
	}

	public void exportDatabaseToJson() throws IOException {
		emInitialize();

		if (PROC_DOMAIN) {
			// Predicate<DomainBase> keepPredicate = new Predicate<DomainBase>()
			// {
			//
			// @Override
			// public boolean evaluate(DomainBase db) {
			// // return !db.getLangItems().containsKey("pl");
			// return db.getDomain().equals("trait");
			// }
			//
			// };
			List<DomainBase> dbList = readDomainsFromDatabase();
			writeDomainsToJsonFile(dbList);
		}
		if (PROC_CYCLE) {
			List<CycleBase> ccbList = readCyclesFromDatabase();
			writeCyclesToJsonFile(ccbList);
		}
		if (PROC_CARD_SET) {
			List<CardSetBase> csbList = readCardSetsFromDatabase();
			writeCardSetsToJsonFile(csbList);
		}
		if (PROC_ENCO_SET) {
			List<EncounterSetBase> esbList = readEncounterSetsFromDatabase();
			writeEncounterSetsToJsonFile(esbList);
		}
		if (PROC_SCENARIO) {
			List<ScenarioBase> ebList = readScenariosFromDatabase();
			writeScenariosToJsonFile(ebList);
		}
		if (PROC_SCEN_ENST) {
			List<ScenEnstLink> selList = readScenEnstLinksFromDatabase();
			writeScenEnstLinksToJsonFile(selList);
		}
		if (PROC_CARD) {
			Predicate<CardBase> keepPredicate = new Predicate<CardBase>() {

				@Override
				public boolean evaluate(CardBase cb) {
					// return
					// cb.getCardSetBase().getTechName().equals("the-great-devourer")
					// ||
					// cb.getCardSetBase().getTechName().equals("descendants-of-isha");
					return true;
				}

			};
			List<CardBase> cbList = readCardsFromDatabase(keepPredicate);
			// for (CardBase cb : cbList) {
			// if (cb.getLangItems().get("pl") == null) {
			// CardLang cl = new CardLang("pl");
			// cl.setName(" ");
			// cl.setTrait(" ");
			// cl.setImageLangCode("pl");
			// cl.setRecordState("A");
			// cb.getLangItems().put("pl", cl);
			// }
			// if (cb.getLangItems().get("en") != null) {
			// cb.getLangItems().get("en").setImageLangCode("en");
			// }
			// }
			writeCardsToJsonFile(cbList);
		}
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
						|| crstOrCycleTechName.equals(cycleTechName));
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

		endTransaction(false);
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
						card.setTraits(StringUtils.trimToNull(propertyElem.getAttribute("value")));
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
				return dbCrstTechName.equals(crstTechName);
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

			if (StringUtils.isNotBlank(cl.getTraits())) {
				stats[3]++;
				myLog.append(techName).append(": not updating - target not blank").append("\n");
			} else if (StringUtils.isBlank(octgnCard.getTraits())) {
				stats[4]++;
				myLog.append(techName).append(": not updating - source blank").append("\n");
			} else {
				stats[5]++;
				myLog.append(techName).append(": updating").append("\n");
				cl.setTraits(octgnCard.getTraits());
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

	private void updateTraitAndKeyword() {
		beginTransaction();

		Map<String, DomainBase> domainsByValue = new HashMap<String, DomainBase>();

		List<DomainBase> dbList = readDomainsFromDatabase();
		for (DomainBase db : dbList) {
			domainsByValue.put(db.getValue(), db);
		}

		StringBuilder myLog = new StringBuilder();

		String sql = "select card.id card_id, card.tech_name card_tn, crst.id crst_id, crst.tech_name crst_tn"
				+ ", card_en.trait trait_en, card_pl.trait trait_pl" + " from cqt_card card"
				+ " join cqt_card_set crst on card.crst_id = crst.id"
				+ " left outer join cqt_card_l card_en on card.id = card_en.card_id and card_en.lang_code = 'en'"
				+ " left outer join cqt_card_l card_pl on card.id = card_pl.card_id and card_pl.lang_code = 'pl'";
		List<Object[]> results = em.createNativeQuery(sql).getResultList();

		// List<CardBase> cbList = readCardsFromDatabase();
		for (Object[] result : results) {
			// if (!cb.getLangItems().containsKey("en") ||
			// !cb.getLangItems().containsKey("pl")) {
			// continue;
			// }

			String enTraitString = (String) result[4];
			String plTraitString = (String) result[5];
			if (StringUtils.isBlank(enTraitString) || StringUtils.isBlank(plTraitString)) {
				continue;
			}

			String[] enTraits = enTraitString.split("\\.");
			for (int i = 0; i < enTraits.length; i++) {
				enTraits[i] = enTraits[i].trim();
			}

			String[] plTraits = plTraitString.split("\\.");
			for (int i = 0; i < plTraits.length; i++) {
				plTraits[i] = plTraits[i].trim();
			}

			if (enTraits.length != plTraits.length) {
				myLog.append("mismatch: ").append(enTraitString).append(" # ").append(plTraitString)
						.append("\n");
				continue;
			}

			for (String enTrait : enTraits) {
				String value = Utils.toTechName(enTrait);
				DomainBase db = domainsByValue.get(value);
				if (db != null) {
					if (!db.getLangItems().containsKey("en")) {
						myLog.append("missing en: ").append(value).append(" -> ").append(enTrait)
								.append("\n");
					}
					if (!db.getLangItems().containsKey("pl")) {
						myLog.append("missing pl: ").append(value).append("\n");
					}
				}
			}
		}

		// for (String description : descriptions) {
		// String value = Utils.toTechName(description);
		//
		// DomainBase db = new DomainBase();
		// db.setDomain("trait");
		// db.setValue(value);
		// db = dbDao.findUnique(db);
		// if (db == null) {
		// db = new DomainBase();
		// db.setDomain("trait");
		// db.setValue(value);
		// db.setRecordState("A");
		// DomainLang dl = new DomainLang();
		// dl.setDescription(description);
		// dl.setLangCode("en");
		// dl.setRecordState("A");
		// dl.setBase(db);
		// db.getLangItems().put("en", dl);
		// log.info("persisting domain: {}, {}", description, value);
		// myLog.append(description).append("\t->\t").append(value).append("\n");
		// // em.persist(db);
		// }
		// }

		log.info(myLog.toString());

		endTransaction();

		// if (DEV_ENV) {
		//
		// StringBuilder keywordInfo = new StringBuilder();
		// StringBuilder keywordUpdate = new StringBuilder();
		//
		// DatabaseUtils.executeSetUserLang(em, "en");
		// List<Domain> domains = new JpaDao<Domain>(em).find(new
		// Domain("keyword"));
		// Set<String> domainSet = new HashSet<>();
		// for (Domain domain : domains) {
		// domainSet.add(domain.getDescription());
		// }
		//
		// int counter = 0;
		// Map<String, Integer> counters = new HashMap<>();
		// for (Domain domain : domains) {
		// counters.put(domain.getDescription().toLowerCase(), 0);
		// }
		//
		// List<Card> cards = new JpaDao<Card>(em).find(new Card());
		// for (Card card : cards) {
		// String text = card.getText();
		// StringBuilder keywords = new StringBuilder();
		// if (StringUtils.isNotBlank(text)) {
		// text = text.toLowerCase();
		//
		// for (Domain domain : domains) {
		// String keyword = domain.getDescription();
		//
		// Pattern p = Pattern.compile("^" + keyword.toLowerCase() + ".*$",
		// Pattern.MULTILINE);
		// if (p.matcher(text).find()) {
		// keywords.append(keyword).append(". ");
		// }
		// }
		//
		// if (!keywords.toString().contains("No Attachments.") &&
		// text.contains("attachments.")) {
		// keywords.append("No Attachments").append(". ");
		// }
		//
		// if (keywords.length() > 0) {
		// keywordInfo.append(card.getName()).append("\t->\t").append(keywords).append("\n");
		// keywordUpdate.append("update cqt_card_l set keyword = '");
		// keywordUpdate.append(keywords.toString().trim());
		// keywordUpdate.append("' where lang_code = 'en' and card_id = ");
		// keywordUpdate.append(card.getId());
		// keywordUpdate.append(" and keyword is null;\n");
		//
		// counter++;
		//
		// for (Domain domain : domains) {
		// String keyword = domain.getDescription().toLowerCase();
		// if (keywords.toString().toLowerCase().contains(keyword)) {
		// counters.put(keyword, counters.get(keyword) + 1);
		// }
		// }
		// }
		// }
		// }
		//
		// log.info("counter: {}", counter);
		// for (Domain domain : domains) {
		// String keyword = domain.getDescription().toLowerCase();
		// log.info("counter: {}, value: {}", keyword, counters.get(keyword));
		// }
		// log.info(keywordInfo.toString());
		// log.info(keywordUpdate.toString());
		// }

	}

	public void writeDomainsToDatabase() throws IOException {
		DomainBase[] sources = JsonUtils.read(FILE_DOMAIN, DomainBase[].class);

		for (DomainBase source : sources) {
			log.info("writing: {}/{}", source.getDomain(), source.getValue());

			DomainBase target = dbDao.findUnique(source);
			if (target == null) {
				persist(source);
			} else {
				merge(target, source);
			}
			em.flush();
		}
	}

	public void writeCyclesToDatabase() throws IOException {
		CycleBase[] sources = JsonUtils.read(FILE_CYCLE, CycleBase[].class);

		for (CycleBase source : sources) {
			CycleBase target = ccbDao.findUnique(source);
			if (target == null) {
				persist(source);
			} else {
				merge(target, source);
			}
			em.flush();
		}
	}

	public void writeCardSetsToDatabase() throws IOException {
		CardSetBase[] sources = JsonUtils.read(FILE_CARD_SET, CardSetBase[].class);

		// load into persistence context
		readCyclesFromDatabase();

		for (CardSetBase source : sources) {
			log.info("writing: {}", source.getTechName());

			CardSetBase target = csbDao.findUnique(source);
			if (target == null) {
				target = source;
			}

			CycleBase ccb = source.getCycleBase();
			if (ccb != null) {
				ccb = ccbDao.findUnique(ccb);
				if (ccb == null) {
					throw new RuntimeException(
							"Unable to find cycle for card set: " + source.getTechName());
				}
			}
			target.setCycleBase(ccb);

			if (target == source) {
				persist(target);
			} else {
				merge(target, source);
			}
			em.flush();
		}
	}

	private void writeEncounterSetsToDatabase() throws IOException {
		EncounterSetBase[] sources = JsonUtils.read(FILE_ENCO_SET, EncounterSetBase[].class);

		for (EncounterSetBase source : sources) {
			log.info("writing: {}", source.getTechName());

			EncounterSetBase target = esbDao.findUnique(source.cloneWithIdentity());
			if (target == null) {
				target = source;
			}

			CardSetBase csb = source.getCardSetBase();
			if (csb != null) {
				csb = csbDao.findUnique(csb);
				if (csb == null) {
					throw new RuntimeException(
							"Unable to find card set for encounter set: " + source.getTechName());
				}
			}
			target.setCardSetBase(csb);

			if (target == source) {
				persist(target);
			} else {
				merge(target, source);
			}
			em.flush();
		}
	}

	private void writeScenariosToDatabase() throws IOException {
		ScenarioBase[] sources = JsonUtils.read(FILE_SCENARIO, ScenarioBase[].class);

		for (ScenarioBase source : sources) {
			log.info("writing: {}", source.getTechName());

			ScenarioBase target = sbDao.findUnique(source.cloneWithIdentity());
			if (target == null) {
				target = source;
			}

			CardSetBase csb = source.getCardSetBase();
			if (csb != null) {
				csb = csbDao.findUnique(csb);
				if (csb == null) {
					throw new RuntimeException(
							"Unable to find card set for scenario: " + source.getTechName());
				}
			}
			target.setCardSetBase(csb);

			if (target == source) {
				persist(target);
			} else {
				merge(target, source);
			}
			em.flush();
		}
	}

	private void writeScenEnstLinksToDatabase() throws IOException {
		ScenEnstLink[] sources = JsonUtils.read(FILE_SCEN_ENST_LINK, ScenEnstLink[].class);

		for (ScenEnstLink source : sources) {
			log.info("writing: {}, {}", source.getScenarioBase().getTechName(),
					source.getEncounterSetBase().getTechName());

			ScenEnstLink target = selDao.findUnique(source.cloneWithIdentity());
			if (target == null) {
				target = source;
			}

			// update scenario
			ScenarioBase sb = source.getScenarioBase();
			if (sb != null) {
				sb = sbDao.findUnique(sb);
				if (sb == null) {
					throw new RuntimeException("Unable to find scenarion for scen enst link: "
							+ source.getScenarioBase().getTechName());
				}
			}
			target.setScenarioBase(sb);

			// update encounter set
			EncounterSetBase esb = source.getEncounterSetBase();
			if (esb != null) {
				esb = esbDao.findUnique(esb);
				if (esb == null) {
					throw new RuntimeException("Unable to find encouter set for scen enst link: "
							+ source.getEncounterSetBase().getTechName());
				}
			}
			target.setEncounterSetBase(esb);

			if (target == source) {
				em.persist(target);
			} else {
				target.setSequence(source.getSequence());
				em.merge(target);
			}
			em.flush();
		}
	}

	public void writeCardsToDatabase() throws IOException {
		CardBase[] sources = JsonUtils.read(FILE_CARD, CardBase[].class);

		for (CardBase source : sources) {
			log.info("writing: {}", source.getTechName());

			CardBase target = cbDao.findUnique(source);
			if (target == null) {
				target = source;
			}

			// update card set
			CardSetBase csb = source.getCardSetBase();
			if (csb != null) {
				csb = csbDao.findUnique(csb);
				if (csb == null) {
					throw new RuntimeException(
							"Unable to find card set for card: " + source.getTechName());
				}
			}
			target.setCardSetBase(csb);

			// update encounter set
			EncounterSetBase esb = source.getEncounterSetBase();
			if (esb != null) {
				esb = esbDao.findUnique(esb);
				if (esb == null) {
					throw new RuntimeException(
							"Unable to find encouter set for card: " + source.getTechName());
				}
			}
			target.setEncounterSetBase(esb);

			if (target == source) {
				persist(target);
			} else {
				merge(target, source);
			}
			em.flush();
		}
	}

	public void writeDomainsToJsonFile(List<DomainBase> list)
			throws JsonProcessingException, IOException {
		JsonUtils.write(list, FILE_DOMAIN);
	}

	public void writeCyclesToJsonFile(List<CycleBase> list)
			throws JsonProcessingException, IOException {
		JsonUtils.write(list, FILE_CYCLE);
	}

	public void writeCardSetsToJsonFile(List<CardSetBase> list)
			throws JsonProcessingException, IOException {
		for (CardSetBase cardSetBase : list) {
			CycleBase cycleBase = cardSetBase.getCycleBase();
			if (cycleBase != null) {
				cycleBase = new CycleBase();
				cycleBase.setTechName(cardSetBase.getCycleBase().getTechName());
				cardSetBase.setCycleBase(cycleBase);
			}
		}
		JsonUtils.write(list, FILE_CARD_SET);
	}

	public void writeEncounterSetsToJsonFile(List<EncounterSetBase> list)
			throws JsonProcessingException, IOException {
		for (EncounterSetBase encounterSetBase : list) {
			CardSetBase cardSetBase = encounterSetBase.getCardSetBase();
			if (cardSetBase != null) {
				cardSetBase = new CardSetBase();
				cardSetBase.setTechName(encounterSetBase.getCardSetBase().getTechName());
				encounterSetBase.setCardSetBase(cardSetBase);
			}
		}
		JsonUtils.write(list, FILE_ENCO_SET);
	}

	public void writeScenariosToJsonFile(List<ScenarioBase> list)
			throws JsonProcessingException, IOException {
		for (ScenarioBase scenarioBase : list) {
			CardSetBase cardSetBase = scenarioBase.getCardSetBase();
			if (cardSetBase != null) {
				cardSetBase = new CardSetBase();
				cardSetBase.setTechName(scenarioBase.getCardSetBase().getTechName());
				scenarioBase.setCardSetBase(cardSetBase);
			}
		}
		JsonUtils.write(list, FILE_SCENARIO);
	}

	public void writeScenEnstLinksToJsonFile(List<ScenEnstLink> list)
			throws JsonProcessingException, IOException {
		for (ScenEnstLink scenEnstLink : list) {
			ScenarioBase scenarioBase = scenEnstLink.getScenarioBase();
			if (scenarioBase != null) {
				scenarioBase = new ScenarioBase();
				scenarioBase.setTechName(scenEnstLink.getScenarioBase().getTechName());
				scenEnstLink.setScenarioBase(scenarioBase);
			}
			EncounterSetBase encounterSetBase = scenEnstLink.getEncounterSetBase();
			if (encounterSetBase != null) {
				encounterSetBase = new EncounterSetBase();
				encounterSetBase.setTechName(scenEnstLink.getEncounterSetBase().getTechName());
				scenEnstLink.setEncounterSetBase(encounterSetBase);
			}
		}
		JsonUtils.write(list, FILE_SCEN_ENST_LINK);
	}

	public void writeCardsToJsonFile(List<CardBase> list)
			throws JsonProcessingException, IOException {
		for (CardBase cardBase : list) {
			CardSetBase cardSetBase = cardBase.getCardSetBase();
			if (cardSetBase != null) {
				cardSetBase = new CardSetBase();
				cardSetBase.setTechName(cardBase.getCardSetBase().getTechName());
				cardBase.setCardSetBase(cardSetBase);
			}
			EncounterSetBase encounterSetBase = cardBase.getEncounterSetBase();
			if (encounterSetBase != null) {
				encounterSetBase = new EncounterSetBase();
				encounterSetBase.setTechName(cardBase.getEncounterSetBase().getTechName());
				cardBase.setEncounterSetBase(encounterSetBase);
			}
		}
		JsonUtils.write(list, FILE_CARD);
	}

	private <B extends IBase<L>, L extends ILang<B>> void persist(B target) {
		JpaUtils.smartPersist(em, target);
	}

	private <B extends IBase<L>, L extends ILang<B>> void merge(B target, B source) {
		JpaUtils.smartMerge(em, target, source, langItemsOnly);
	}
}
