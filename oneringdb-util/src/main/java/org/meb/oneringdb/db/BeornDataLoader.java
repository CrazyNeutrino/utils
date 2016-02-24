package org.meb.oneringdb.db;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.meb.oneringdb.db.model.CardBase;
import org.meb.oneringdb.db.model.CardLang;
import org.meb.oneringdb.db.model.CardSetBase;
import org.meb.oneringdb.db.model.CardSetLang;
import org.meb.oneringdb.db.model.CardSetType;
import org.meb.oneringdb.db.model.CardType;
import org.meb.oneringdb.db.model.CycleBase;
import org.meb.oneringdb.db.model.IBase;
import org.meb.oneringdb.db.model.ILang;
import org.meb.oneringdb.db.model.Sphere;
import org.meb.oneringdb.db.util.Functions;
import org.meb.oneringdb.db.util.JpaUtils;
import org.meb.oneringdb.db.util.Utils;
import org.meb.oneringdb.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import lombok.Setter;

public class BeornDataLoader extends AbstractLoader {

	private static final Logger log = LoggerFactory.getLogger(BeornDataLoader.class);

	protected static final String DATA_BASE;
	protected static final String JSON_BASE;
	protected static final String RAW_BASE;

	static {
		String tmpDataBase = System.getProperty("data.home");
		if (StringUtils.isBlank(tmpDataBase)) {
			throw new IllegalStateException("Data home not set");
		}
		if (tmpDataBase.trim().endsWith("/")) {
			DATA_BASE = tmpDataBase;
		} else {
			DATA_BASE = tmpDataBase + "/";
		}

		RAW_BASE = DATA_BASE + "_raw/";
		JSON_BASE = DATA_BASE + "json-test2/";
		if (!new File(JSON_BASE).exists()) {
			new File(JSON_BASE).mkdir();
		}
	}

	@Setter
	private boolean langItemsOnly = false;

	private Set<String> excludedCards;
	private Set<String> excludedCardSets;

	public static void main(String[] args) throws IOException {
		BeornDataLoader loader = new BeornDataLoader();
		try {
			if (args[0].equals("--update-card-sets-from-beorn")) {
				loader.updateCardSetsFromBeorn();
			} else if (args[0].equals("--update-cards-from-beorn")) {
				loader.updateCardsFromBeorn();
			} else {
				throw new IllegalArgumentException("Invalid argument");
			}
		} finally {
			loader.emFinalize();
		}
	}

	public void updateCardSetsFromBeorn() {
		emInitialize();

		beginTransaction();

		Map<String, CardSetType> typeMapping = new HashMap<>();
		typeMapping.put("Core", CardSetType.CORE);
		typeMapping.put("Adventure_Pack", CardSetType.AP);
		typeMapping.put("Deluxe_Expansion", CardSetType.DELUXE);
		typeMapping.put("Saga_Expansion", CardSetType.SAGA);
		typeMapping.put("Nightmare_Expansion", CardSetType.ND);
		typeMapping.put("GenCon_Expansion", CardSetType.GC);
		typeMapping.put("GenConSaga_Expansion", CardSetType.GC_SAGA);
		typeMapping.put("Fellowship_Deck", CardSetType.FD);

		StringBuilder myLog = new StringBuilder();

		List<CardSetBase> csbList = readCardSetsFromDatabase();
		Map<String, CardSetBase> csbMap = Maps.uniqueIndex(csbList, Functions.CardSetBaseTechName);

		try {
			JsonParser parser = JsonUtils
					.createJsonParser(new FileReader(DATA_BASE + "beorn/sets.json"));
			Iterator<JsonNode> jsonSetsIter = parser.readValueAsTree().getElements();
			while (jsonSetsIter.hasNext()) {
				JsonNode jsonSet = jsonSetsIter.next();
				String name = jsonSet.get("Name").getTextValue();
				String techName = Utils.toTechName(name);

				String cycleName = jsonSet.get("Cycle").getTextValue();
				if ("NIGHTMARE".equals(cycleName) || "GenCon".equals(cycleName)) {
					cycleName = null;
				}
				String cycleTechName = null;
				if (cycleName != null) {
					cycleTechName = Utils.toTechName(cycleName);
				}

				String typeString = jsonSet.get("SetType").asText();
				if (typeString.equals("CUSTOM")) {
					continue;
				}
				CardSetType type = typeMapping.get(typeString);
				if (type == null) {
					throw new RuntimeException("No mapping for: " + typeString);
				}

				myLog.append(techName);
				myLog.append(", ").append(cycleTechName);
				myLog.append(", ").append(type);
				myLog.append("\n");

				if (csbMap.containsKey(techName)) {
					myLog.append("exists").append("\n");
					continue;
				}

				CardSetBase csb = new CardSetBase();
				csb.setTechName(techName);
				csb.setTypeCode(type);
				csb.setRecordState("A");
				csb.setReleased(true);

				String langCode = "en";
				CardSetLang csl = csb.getLangItems().get(langCode);
				if (csl == null) {
					csl = new CardSetLang();
					csl.setBase(csb);
					csb.getLangItems().put(langCode, csl);
					csl.setLangCode(langCode);
					csl.setName(name);
					csl.setRecordState("A");
				}

				if (cycleTechName != null) {
					CycleBase ccb = ccbDao.findUnique(new CycleBase(cycleTechName));
					if (ccb == null) {
						throw new RuntimeException(
								"Unable to find cycle for card set: " + csb.getTechName());
					}
					csb.setCycleBase(ccb);
				}

				myLog.append("persist").append("\n");

				em.persist(csb);
				em.flush();
			}

			endTransaction(false);
		} catch (IOException e) {
			endTransaction(false);
			throw new RuntimeException(e);
		} finally {
			log.info(myLog.toString());
		}
	}

	public void updateCardsFromBeorn() {
		emInitialize();

		beginTransaction();

		StringBuilder myLog = new StringBuilder();

		List<CardSetBase> csbList = readCardSetsFromDatabase();
		Map<String, CardSetBase> csbMap = Maps.uniqueIndex(csbList, Functions.CardSetBaseTechName);
		List<CardBase> cbList = readCardsFromDatabase();
		Map<String, CardBase> cbMap = Maps.uniqueIndex(cbList, Functions.CardBaseComposite);

		int count = 0;
		try {
			JsonParser parser = JsonUtils
					.createJsonParser(new FileReader(DATA_BASE + "beorn/cards.json"));
			Iterator<JsonNode> jsonSetsIter = parser.readValueAsTree().getElements();
			while (jsonSetsIter.hasNext()) {
				JsonNode json = jsonSetsIter.next();

				String name = json.get("Title").getTextValue();
				String techName = Utils.toTechName(name);
				String crstName = json.get("CardSet").getTextValue();
				String crstTechName = Utils.toTechName(crstName);
				
				crstTechName = adjustCrstTechName(crstTechName);

				CardSetBase csb = new CardSetBase(crstTechName);
				CardBase cb = new CardBase(techName);
				cb.setCardSetBase(csb);
				cb.setRecordState("A");
				cb.setNumber(json.get("Number").getIntValue());

				String cbKey = Functions.CardBaseComposite.apply(cb);
				if (cbMap.containsKey(cbKey) || isExcludedCardSet(csb) || isExcludedCard(cb)) {
					continue;
				}

				cb.setType(parseCardType(json.get("CardType")));
				cb.setSphere(parseSphere(json.get("Sphere")));
				cb.setUnique(json.get("IsUnique").getBooleanValue());
				cb.setIllustrator(json.get("Artist").getTextValue());

				JsonNode jsonStats = json.get("Front").get("Stats");
				String[] statNames = { "ThreatCost", "ResourceCost", "EngagementCost", "Willpower",
						"Attack", "Defense", "HitPoints", "QuestPoints", "Quantity" };
				for (String statName : statNames) {
					JsonNode jsonStat = jsonStats.get(statName);
					if (jsonStat != null && !jsonStat.isNull()) {
						Integer statValue = jsonStat.getIntValue();
						try {
							BeanUtils.setProperty(cb, StringUtils.uncapitalize(statName),
									statValue);
						} catch (IllegalAccessException | InvocationTargetException e) {
							throw new RuntimeException(e);
						}
					}
				}

				CardLang cl = new CardLang("en");
				cl.setRecordState("A");
				cl.setBase(cb);
				cb.getLangItems().put(cl.getLangCode(), cl);
				cl.setName(name);
				cl.setTraits(parseArray(json.get("Traits"), " "));
				cl.setKeywords(parseArray(json.get("Keywords"), " "));
//				cl.setText(parseArray(json.get("Text"), "\n"));
//				cl.setFlavourText(parseArray(json.get("FlavourText"), "\n"));

				myLog.append(crstTechName).append("\t\t").append(techName).append("\n");
				count++;

				// em.persist(cb);
				// em.flush();
			}

			myLog.append("count: ").append(count).append("\n");

			endTransaction(false);
		} catch (IOException e) {
			endTransaction(false);
			throw new RuntimeException(e);
		} finally {
			log.info(myLog.toString());
		}
	}

	private String adjustCrstTechName(String crstTechName) {
		if (crstTechName.startsWith("the-hobbit-")) {
			crstTechName = crstTechName.replace("the-hobbit-", "");
		} else if (crstTechName.endsWith("-nightmare")) {
			crstTechName = crstTechName.replace("-nightmare", "-nightmare-deck");
		}
		return crstTechName;
	}

	private boolean isExcludedCardSet(CardSetBase csb) {

		if (excludedCardSets == null) {
			initializeExcludedCardSets();
		}

		if (excludedCardSets.contains(csb.getTechName())) {
			return true;
		}

		return false;
	}

	private boolean isExcludedCard(CardBase cb) {

		if (excludedCards == null) {
			initializeExcludedCards();
		}

		String cbKey = cb.getCardSetBase().getTechName() + "#" + cb.getTechName();
		if (excludedCards.contains(cbKey)) {
			return true;
		}

		return false;
	}

	private void initializeExcludedCardSets() {
		try {
			List<String> excluded = IOUtils.readLines(
					this.getClass().getResourceAsStream("/beorn_excluded_card_sets.txt"));
			excludedCardSets = new HashSet<>();
			excludedCardSets.addAll(excluded);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void initializeExcludedCards() {
		try {
			List<String> excluded = IOUtils
					.readLines(this.getClass().getResourceAsStream("/beorn_excluded_cards.txt"));
			excludedCards = new HashSet<>();
			excludedCards.addAll(excluded);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private CardType parseCardType(JsonNode jsonCardType) {
		CardType cardType;
		if (jsonCardType != null && !jsonCardType.isNull()) {
			String typeStr = jsonCardType.getTextValue();
			if ("Objective_Ally".equals(typeStr) || "Objective_Location".equals(typeStr)) {
				typeStr = "OBJECTIVE";
			}
			cardType = CardType.valueOf(typeStr.toUpperCase());
		} else {
			cardType = null;
		}
		return cardType;
	}

	private Sphere parseSphere(JsonNode jsonSphere) {
		Sphere sphere;
		if (jsonSphere != null && !jsonSphere.isNull()) {
			String strSphere = jsonSphere.getTextValue();
			sphere = Sphere.valueOf(strSphere.toUpperCase());
		} else {
			sphere = null;
		}
		return sphere;
	}

	private String parseArray(JsonNode jsonArray, String delim) {
		String joined = "";
		if (jsonArray != null && jsonArray.isArray()) {
			Iterator<JsonNode> arrayIter = jsonArray.getElements();
			while (arrayIter.hasNext()) {
				joined += arrayIter.next().getTextValue() + delim;
			}
		}
		return StringUtils.trimToNull(joined);
	}

	private <B extends IBase<L>, L extends ILang<B>> void persist(B target) {
		JpaUtils.smartPersist(em, target);
	}

	private <B extends IBase<L>, L extends ILang<B>> void merge(B target, B source) {
		JpaUtils.smartMerge(em, target, source, langItemsOnly);
	}
}
