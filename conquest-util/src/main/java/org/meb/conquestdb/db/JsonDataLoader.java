package org.meb.conquestdb.db;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.MathArrays;
import org.codehaus.jackson.JsonProcessingException;
import org.meb.conquest.db.dao.CardSetBaseDao;
import org.meb.conquest.db.dao.JpaDao;
import org.meb.conquest.db.model.CardBase;
import org.meb.conquest.db.model.CardLang;
import org.meb.conquest.db.model.CardSetBase;
import org.meb.conquest.db.model.CycleBase;
import org.meb.conquest.db.model.Deck;
import org.meb.conquest.db.model.DeckLink;
import org.meb.conquest.db.model.DeckMember;
import org.meb.conquest.db.model.DeckType;
import org.meb.conquest.db.model.DomainBase;
import org.meb.conquest.db.model.IBase;
import org.meb.conquest.db.model.ILang;
import org.meb.conquest.db.model.loc.Card;
import org.meb.conquest.db.query.CardQuery;
import org.meb.conquest.db.query.DeckQuery;
import org.meb.conquest.db.util.Transformers;
import org.meb.conquest.db.util.Utils;
import org.meb.conquestdb.db.util.JpaUtils;
import org.meb.conquestdb.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		JSON_BASE = DATA_BASE + "json-wlb2/";
		createDirectory(DATA_BASE);
		createDirectory(IMAGE_BASE);
		createDirectory(JSON_BASE);
	}

	private static void createDirectory(String name) {
		if (!new File(name).exists()) {
			new File(name).mkdir();
		}
	}

	private static final String DOMAIN_FILE_NAME = JSON_BASE + "domain.json";
	private static final String CYCLE_FILE_NAME = JSON_BASE + "cycle.json";
	private static final String CARD_SET_FILE_NAME = JSON_BASE + "card_set.json";
	private static final String CARD_FILE_NAME = JSON_BASE + "card.json";
	private static final boolean PROC_DOMAIN = Boolean
			.valueOf(System.getProperty("data.proc.domain"));
	private static final boolean PROC_CYCLE = Boolean
			.valueOf(System.getProperty("data.proc.cycle"));
	private static final boolean PROC_CARD_SET = Boolean
			.valueOf(System.getProperty("data.proc.cardset"));
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
			} else if (args[0].equals("--rename-borwol")) {
				loader.renameBorwol();
			} else if (args[0].equals("--generate-decks") && DEV_ENV) {
				loader.generateDecks(Integer.parseInt(args[1]), Long.parseLong(args[2]),
						Long.parseLong(args[3]));
			} else if (args[0].equals("--publish-decks") && DEV_ENV) {
				loader.publishDecks(Integer.parseInt(args[1]));
			} else if (args[0].equals("--update-trait-keyword")) {
				loader.updateTraitAndKeyword();
			} else if (args[0].equals("--update-trait-keyword")) {
				loader.updateTraitAndKeyword();
			} else {
				throw new IllegalArgumentException("Invalid argument");
			}
		} finally {
			loader.cleanUp();
		}
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

	private void renameBorwol() {
		String crstTechName = "what-lurks-below";
		CardSetBase crst = new CardSetBaseDao(em).findUnique(new CardSetBase(crstTechName));

		String dirName = IMAGE_BASE + "_raw_/card/pl/borwol/";
		dirName += StringUtils.leftPad(crst.getSequence().toString(), 2, '0');
		dirName += "-" + crst.getTechName();

		File dir = new File(dirName);
		String[] fileNames = dir.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.matches("[\\p{Digit}]{1,3}(b)?\\.png");
			}
		});

		JpaDao<Card, CardQuery> dao = new JpaDao<>(em);
		Card card = new Card();
		card.setCrstTechName(crstTechName);
		List<Card> cards = dao.find(card);
		HashMap<Integer, Card> map = new HashMap<Integer, Card>();
		MapUtils.populateMap(map, cards, new Transformer<Card, Integer>() {

			@Override
			public Integer transform(Card input) {
				return input.getNumber();
			}
		});

		for (String fileName : fileNames) {
			int dash = fileName.indexOf('-');
			Integer number = Integer.valueOf(fileName.substring(0, 2));
			String newFileName = StringUtils.leftPad(number.toString(), 3, '0') + "-"
					+ map.get(number).getTechName();
			if (dash != -1) {
				newFileName += "-b";
			}
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

	private void generateDecks(int deckCount, long userIdMin, long userIdMax) {

		// beginTransaction();
		//
		// List<Deck> decks = new JpaDao<Deck, DeckQuery>(em).find(new Deck());
		// for (Deck deck : decks) {
		// em.remove(deck);
		// em.flush();
		// }
		//
		// int maxDeckCount = 1000;
		// if (deckCount > maxDeckCount) {
		// throw new RuntimeException("Are you nuts?!");
		// }
		//
		// Card warlordExample = new Card();
		// warlordExample.setType(CardType.WARLORD);
		// JpaDao<Card, CardQuery> cardDao = new JpaDao<>(em);
		// List<Card> warlords = cardDao.find(warlordExample);
		//
		// int padSize = (int) Math.floor(Math.log10(maxDeckCount));
		//
		// for (long userId = userIdMin; userId <= userIdMax; userId++) {
		//
		// for (int i = 0; i < deckCount; i++) {
		// Card warlord = warlords.get(RandomUtils.nextInt(warlords.size()));
		//
		// Card cardExample = new Card();
		// CardQuery cardQuery = new CardQuery(cardExample);
		// cardQuery.setDeckFaction(warlord.getFaction());
		// cardQuery.setDeckWarlordId(warlord.getId());
		// List<Card> cards = new CardDao(em).find(cardQuery);
		// Collections.shuffle(cards);
		//
		// Deck deck = new Deck();
		// deck.setName(StringUtils.leftPad(String.valueOf(i), padSize, '0') +
		// " - "
		// + RandomStringUtils.randomAlphabetic(20));
		// log.info(deck.getName());
		// deck.setUserId(userId);
		// Date date = new Date();
		// deck.setCreateDate(date);
		// deck.setModifyDate(date);
		// deck.setWarlord(warlord);
		//
		// HashSet<Faction> validFactions = new HashSet<Faction>();
		// validFactions.add(warlord.getFaction());
		// validFactions.add(Faction.NEUTRAL);
		//
		// int totalQuantity = 0;
		// for (Card card : cards) {
		// if (validFactions.size() == 2) {
		// validFactions.add(card.getFaction());
		// } else if (!validFactions.contains(card.getFaction())) {
		// continue;
		// }
		//
		// double bonus = card.getFaction() == warlord.getFaction() ? 0.3 : 0.0;
		// double value = RandomUtils.nextDouble() + bonus;
		// int quantity = 0;
		//
		// Integer cardQuantity = (Integer) ObjectUtils.defaultIfNull(3, new
		// Integer(3));
		//
		// boolean flagWarlord = card.getId().equals(warlord.getId());
		// boolean flagSignatureSquad = card.getWarlordId() != null;
		// if (flagWarlord || flagSignatureSquad) {
		// quantity = 1;
		// } else if (value > 0.8) {
		// quantity = 3;
		// } else if (value > 0.6) {
		// quantity = 2;
		// } else if (value > 0.5) {
		// quantity = 1;
		// } else {
		// continue;
		// }
		//
		// if (totalQuantity < 50 || flagWarlord || flagSignatureSquad) {
		// quantity = Math.min(quantity, cardQuantity);
		// totalQuantity += quantity;
		//
		// DeckMember deckMember = new DeckMember();
		// deckMember.setQuantity(quantity);
		// deckMember.setCard(card);
		// deckMember.setDeck(deck);
		// deck.getDeckMembers().add(deckMember);
		// }
		// }
		//
		// em.persist(deck);
		// em.flush();
		// }
		// }
		//
		// endTransaction();
	}

	public void publishDecks(int deckCount) {
		beginTransaction();

		Deck example = new Deck(DeckType.SNAPSHOT);
		List<Deck> decks = new JpaDao<Deck, DeckQuery>(em).find(example);
		for (Deck deck : decks) {
			em.remove(deck);
			em.flush();
		}

		example = new Deck(DeckType.BASE);
		decks = new JpaDao<Deck, DeckQuery>(em).find(example);
		Collection<Integer> deckIds = CollectionUtils.collect(decks,
				new Transformer<Deck, Integer>() {

					@Override
					public Integer transform(Deck deck) {
						return deck.getId().intValue();
					}
				});

		Map<Long, Deck> decksMap = new HashMap<>();
		MapUtils.populateMap(decksMap, decks, Transformers.DECK_ID);

		int[] deckIdsArray = ArrayUtils.toPrimitive(deckIds.toArray(new Integer[deckIds.size()]));
		MathArrays.shuffle(deckIdsArray);
		int publishedCount = 0;
		for (int deckId : deckIdsArray) {
			Deck deck = decksMap.get(new Long(deckId));
			int quantity = 0;
			for (DeckMember deckMember : deck.getDeckMembers()) {
				quantity += deckMember.getQuantity();
			}
			if (quantity >= 50) {
				em.detach(deck);
				deck.setId(null);
				deck.setVersion(null);
				deck.setSnapshotBase(em.find(Deck.class, new Long(deckId)));
				deck.setSnapshotPublic(Boolean.TRUE);
				Date date = new Date();
				deck.setCreateDate(date);
				deck.setModifyDate(date);
				deck.setType(DeckType.SNAPSHOT);
				deck.setDeckLinks(new HashSet<DeckLink>());
				deck.setDeckMembers(new HashSet<DeckMember>(deck.getDeckMembers()));
				for (DeckMember deckMember : deck.getDeckMembers()) {
					deckMember.setId(null);
					deckMember.setVersion(null);
				}
				em.persist(deck);
				em.flush();

				if (++publishedCount >= deckCount) {
					break;
				}
			}
		}

		endTransaction();
	}

	public JsonDataLoader() {

	}

	public void importDatabaseFromJson() throws IOException {
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
		if (PROC_CARD) {
			writeCardsToDatabase();
		}
		endTransaction(true);
	}

	public void exportDatabaseToJson() throws IOException {
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
		if (PROC_CARD) {
			Predicate<CardBase> keepPredicate = new Predicate<CardBase>() {

				@Override
				public boolean evaluate(CardBase cb) {
					return cb.getCardSetBase().getTechName().equals("what-lurks-below");
					// return true;
				}

			};
			List<CardBase> cbList = readCardsFromDatabase(keepPredicate);
			for (CardBase cb : cbList) {
				if (cb.getLangItems().get("pl") == null) {
					CardLang cl = new CardLang("pl");
					cl.setName(" ");
					cl.setTrait(" ");
					cl.setImageLangCode("pl");
					cl.setRecordState("A");
					cb.getLangItems().put("pl", cl);
				}
			}
			writeCardsToJsonFile(cbList);
		}

	}

	public void writeDomainsToDatabase() throws IOException {
		DomainBase[] sources = JsonUtils.read(DOMAIN_FILE_NAME, DomainBase[].class);

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
		CycleBase[] sources = JsonUtils.read(CYCLE_FILE_NAME, CycleBase[].class);

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
		CardSetBase[] sources = JsonUtils.read(CARD_SET_FILE_NAME, CardSetBase[].class);

		for (CardSetBase source : sources) {
			log.info("writing: {}", source.getTechName());

			CardSetBase target = csbDao.findUnique(source);
			if (target == null) {
				target = source;
			}

			CycleBase cycleBase = source.getCycleBase();
			if (cycleBase != null) {
				cycleBase = ccbDao.findUnique(cycleBase);
				if (cycleBase == null) {
					throw new RuntimeException(
							"Unable to find cycle for card set: " + source.getTechName());
				}
			}
			target.setCycleBase(cycleBase);

			if (target == source) {
				persist(target);
			} else {
				merge(target, source);
			}
			em.flush();
		}
	}

	public void writeCardsToDatabase() throws IOException {
		CardBase[] sources = JsonUtils.read(CARD_FILE_NAME, CardBase[].class);

		for (CardBase source : sources) {
			log.info("writing: {}", source.getTechName());

			CardBase target = cbDao.findUnique(source);
			if (target == null) {
				target = source;
			}

			// update warlord
			CardBase warlordBase = source.getWarlordBase();
			if (warlordBase != null) {
				warlordBase = cbDao.findUnique(warlordBase);
				if (warlordBase == null) {
					throw new RuntimeException(
							"Unable to find warlord for card: " + source.getTechName());
				}
			}
			if (warlordBase != null) {
				target.setWarlordBase(warlordBase);
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
		JsonUtils.write(list, DOMAIN_FILE_NAME);
	}

	public void writeCyclesToJsonFile(List<CycleBase> list)
			throws JsonProcessingException, IOException {
		JsonUtils.write(list, CYCLE_FILE_NAME);
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
		JsonUtils.write(list, CARD_SET_FILE_NAME);
	}

	public void writeCardsToJsonFile(List<CardBase> list)
			throws JsonProcessingException, IOException {
		for (CardBase cardBase : list) {
			CardBase warlordBase = cardBase.getWarlordBase();
			if (warlordBase != null) {
				warlordBase = new CardBase();
				warlordBase.setTechName(cardBase.getWarlordBase().getTechName());
				cardBase.setWarlordBase(warlordBase);
			}
			CardSetBase cardSetBase = cardBase.getCardSetBase();
			if (cardSetBase != null) {
				cardSetBase = new CardSetBase();
				cardSetBase.setTechName(cardBase.getCardSetBase().getTechName());
				cardBase.setCardSetBase(cardSetBase);
			}
		}
		JsonUtils.write(list, CARD_FILE_NAME);
	}

	private <B extends IBase<L>, L extends ILang<B>> void persist(B target) {
		JpaUtils.smartPersist(em, target);
	}

	private <B extends IBase<L>, L extends ILang<B>> void merge(B target, B source) {
		JpaUtils.smartMerge(em, target, source, langItemsOnly);
	}
}
