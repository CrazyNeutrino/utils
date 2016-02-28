package org.meb.oneringdb.db;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.beanutils.PropertyUtils;
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
import org.meb.oneringdb.db.model.Sphere;
import org.meb.oneringdb.db.util.Functions;
import org.meb.oneringdb.db.util.Utils;
import org.meb.oneringdb.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public class BeornDataLoader extends AbstractLoader {

	private static final Logger log = LoggerFactory.getLogger(BeornDataLoader.class);

	private class ImageInfo {
		private String url;
		private String fileName;
		private String dirName;

		// public ImageInfo(String url, String fileName, String dirName) {
		// super();
		// this.url = url;
		// this.fileName = fileName;
		// this.dirName = dirName;
		// }
	}

	private class ImageLoadTask implements Runnable {

		private ImageInfo imageInfo;

		private ImageLoadTask(ImageInfo imageInfo) {
			this.imageInfo = imageInfo;
		}

		@Override
		public void run() {
			try {
				URLConnection connection = new URL(imageInfo.url).openConnection();
				connection.setRequestProperty("Referer", "http://hallofbeorn.com/");
				connection.setRequestProperty("User-agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.120 Safari/537.36");
				InputStream inputStream = connection.getInputStream();

				File dir = new File(IMAGE_BASE + "_raw/card/en/" + imageInfo.dirName);
				File file = new File(dir, imageInfo.fileName);
				if (file.exists()) {
					return;
				}

				synchronized (BeornDataLoader.this) {
					if (!dir.exists()) {
						dir.mkdir();
					}
				}

				OutputStream outputStream = new FileOutputStream(file);
				IOUtils.copy(inputStream, outputStream);
				IOUtils.closeQuietly(outputStream);
				IOUtils.closeQuietly(inputStream);
			} catch (Exception e) {
				log.error("Unable to get {}\n\t{}", imageInfo.url, e.getMessage());
			}
		}
	};

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
		JSON_BASE = DATA_BASE + "json-test2/";
		createDirectory(DATA_BASE);
		createDirectory(IMAGE_BASE);
		createDirectory(JSON_BASE);
	}

	private static void createDirectory(String name) {
		if (!new File(name).exists()) {
			new File(name).mkdir();
		}
	}

	private Set<String> excludedCards;
	private Set<String> excludedCardSets;
	private ResourceBundle adjustCardNameBundle;

	public static void main(String[] args) throws IOException {
		BeornDataLoader loader = new BeornDataLoader();
		try {
			if (args[0].equals("--import-card-sets-beorn")) {
				loader.importCardSets();
			} else if (args[0].equals("--import-cards-beorn")) {
				loader.importCards();
			} else if (args[0].equals("--load-images-beorn")) {
				loader.loadImages();
			} else {
				throw new IllegalArgumentException("Invalid argument");
			}
		} finally {
			loader.emFinalize();
		}
	}

	public void importCardSets() {
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
					.createJsonParser(new FileReader(DATA_BASE + "beorn/card_set.json"));
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

	public void importCards() {
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
					.createJsonParser(new FileReader(DATA_BASE + "beorn/card.json"));
			Iterator<JsonNode> jsonSetsIter = parser.readValueAsTree().getElements();
			while (jsonSetsIter.hasNext()) {
				JsonNode json = jsonSetsIter.next();

				String crstName = json.get("CardSet").getTextValue();
				String cardName = json.get("Title").getTextValue();
				Integer number = json.get("Number").getIntValue();

				crstName = adjustCrstName(crstName);
				cardName = adjustCardName(crstName, cardName, number);

				String crstTechName = Utils.toTechName(crstName);
				String cardTechName = Utils.toTechName(cardName);

				CardSetBase csb = csbMap.get(crstTechName);
				if (csb == null) {
					continue;
				}

				CardBase cb = new CardBase(cardTechName);
				cb.setCardSetBase(csb);
				cb.setRecordState("A");
				cb.setNumber(number);
				cb.setQuantity(json.get("Quantity").getIntValue());

				String cbKey = Functions.CardBaseComposite.apply(cb);
				if (cbMap.containsKey(cbKey) || isExcludedCardSet(csb) || isExcludedCard(cb)) {
					continue;
				}

				cb.setType(parseCardType(json.get("CardType")));
				cb.setSphere(parseSphere(json.get("Sphere")));
				cb.setUnique(json.get("IsUnique").getBooleanValue());
				cb.setIllustrator(json.get("Artist").getTextValue());

				JsonNode jsonFront = json.get("Front");
				JsonNode jsonStats = jsonFront.get("Stats");
				String[] statNames = { "ThreatCost", "ResourceCost", "EngagementCost", "Willpower",
						"Attack", "Defense", "HitPoints", "QuestPoints" };
				for (String statName : statNames) {
					Integer statValue = parseStat(jsonStats.get(statName));
					try {
						PropertyUtils.setProperty(cb, StringUtils.uncapitalize(statName),
								statValue);
					} catch (IllegalAccessException | InvocationTargetException
							| NoSuchMethodException e) {
						throw new RuntimeException(e);
					}
				}

				CardLang cl = new CardLang("en");
				cl.setRecordState("A");
				cl.setBase(cb);
				cb.getLangItems().put(cl.getLangCode(), cl);
				cl.setName(cardName);
				cl.setTraits(parseTraits(jsonFront.get("Traits")));
				cl.setKeywords(parseKeywords(jsonFront.get("Keywords")));
				cl.setText(parseText(jsonFront.get("Text")));
				cl.setFlavorText(parseFlavourText(jsonFront.get("FlavorText")));

				cb = adjustCard(cb);

				myLog.append(crstTechName).append("\t\t").append(cardTechName).append("\n");
				count++;

				em.persist(cb);
				em.flush();
			}

			myLog.append("count: ").append(count).append("\n");

			endTransaction(true);
		} catch (IOException e) {
			endTransaction(false);
			throw new RuntimeException(e);
		} finally {
			log.info(myLog.toString());
		}
	}

	public void loadImages() {
		emInitialize();

		beginTransaction();

		StringBuilder myLog = new StringBuilder();

		List<CardSetBase> csbList = readCardSetsFromDatabase();
		Map<String, CardSetBase> csbMap = Maps.uniqueIndex(csbList, Functions.CardSetBaseTechName);
		List<CardBase> cbList = readCardsFromDatabase();
		Map<String, CardBase> cbMap = Maps.uniqueIndex(cbList, Functions.CardBaseComposite);

		final List<ImageInfo> imageInfos = new ArrayList<>();

		try {
			JsonParser parser = JsonUtils
					.createJsonParser(new FileReader(DATA_BASE + "beorn/card.json"));
			Iterator<JsonNode> jsonSetsIter = parser.readValueAsTree().getElements();
			int count = 0;
			while (jsonSetsIter.hasNext() && count < 3000) {
				JsonNode json = jsonSetsIter.next();

				String crstName = json.get("CardSet").getTextValue();
				String cardName = json.get("Title").getTextValue();
				Integer number = json.get("Number").getIntValue();

				crstName = adjustCrstName(crstName);
				cardName = adjustCardName(crstName, cardName, number);

				String crstTechName = Utils.toTechName(crstName);
				String cardTechName = Utils.toTechName(cardName);

				CardBase cb = new CardBase(cardTechName);
				cb.setCardSetBase(new CardSetBase(crstTechName));
				cb.setNumber(number);

				String cbKey = Functions.CardBaseComposite.apply(cb);
				if (!cbMap.containsKey(cbKey) || cbMap.get(cbKey).getType() != CardType.QUEST
						|| !crstTechName.equals("khazad-dum")) {
					continue;
				}

				Integer sequence = csbMap.get(crstTechName).getSequence();
				JsonNode imagePathFront = json.get("Front").get("ImagePath");
				if (imagePathFront != null && imagePathFront.isTextual()) {
					imageInfos.add(createImageInfo(crstTechName, sequence, cardTechName, number,
							imagePathFront, false));

					if (cbMap.get(cbKey).getType() == CardType.QUEST) {
						imageInfos.add(createImageInfo(crstTechName, sequence, cardTechName, number,
								imagePathFront, true));
					}
				}

				count++;
			}

			ExecutorService service = Executors.newFixedThreadPool(20);
			for (ImageInfo imageInfo : imageInfos) {
				service.execute(new ImageLoadTask(imageInfo));
			}

			try {
				service.shutdown();
				service.awaitTermination(30, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			// myLog.append("count: ").append(count).append("\n");

			endTransaction(false);
		} catch (IOException e) {
			endTransaction(false);
			throw new RuntimeException(e);
		} finally {
			log.info(myLog.toString());
		}
	}

	private ImageInfo createImageInfo(String crstTechName, Integer crstSequence,
			String cardTechName, Integer cardNumber, JsonNode imagePathFront, boolean back) {

		String tmpUrl = imagePathFront.getTextValue();
		String imagePart = tmpUrl.substring(tmpUrl.lastIndexOf('/') + 1);
		try {
			tmpUrl = tmpUrl.replace(imagePart, URLEncoder.encode(imagePart, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		if (back) {
			tmpUrl = tmpUrl.replaceFirst("\\-([0-9])A\\.", "-$1B.");
		}

		ImageInfo ii = new ImageInfo();
		ii.url = tmpUrl;
		ii.fileName = StringUtils.leftPad(cardNumber.toString(), 3, "0") + "-" + cardTechName;
		if (back) {

			ii.fileName += "-b";
		}
		ii.fileName += "." + ii.url.substring(ii.url.lastIndexOf('.') + 1).toLowerCase();
		ii.dirName = StringUtils.leftPad(crstSequence.toString(), 3, "0") + "-" + crstTechName;
		return ii;
	}

	private String adjustCrstName(String crstName) {
		if (crstName.startsWith("The Hobbit: ")) {
			crstName = crstName.replace("The Hobbit: ", "");
		} else if (crstName.endsWith(" Nightmare")) {
			crstName = crstName.replace(" Nightmare", " Nightmare Deck");
		}
		return crstName;
	}

	private String adjustCardName(String crstName, String cardName, Integer number) {
		if (adjustCardNameBundle == null) {
			adjustCardNameBundle = ResourceBundle.getBundle("beorn_adjust_card_name");
		}
		String key = Utils.toTechName(crstName) + "." + Utils.toTechName(cardName) + "."
				+ number.toString();
		try {
			return adjustCardNameBundle.getString(key);
		} catch (MissingResourceException e) {
			return cardName;
		}
	}

	private CardBase adjustCard(CardBase cb) {
		if ("savage-trollspawn".equals(cb.getTechName())) {
			cb.setNumber(72);
		} else if ("coldfell-giant".equals(cb.getTechName())) {
			cb.setNumber(73);
		}
		return cb;
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

	private Integer parseStat(JsonNode jsonStat) {
		Integer value = null;
		if (jsonStat != null && !jsonStat.isNull()) {
			if (jsonStat.isNumber()) {
				value = jsonStat.getIntValue();
			} else if (jsonStat.isTextual()) {
				String strValue = jsonStat.getTextValue();
				if ("X".equalsIgnoreCase(strValue)) {
					value = -1;
				} else {
					try {
						value = new Integer(strValue);
					} catch (NumberFormatException e) {
						throw new RuntimeException(e);
					}
				}
			} else {
				throw new RuntimeException("Illegal value");
			}
		}
		return value;
	}

	private String parseTraits(JsonNode jsonTraits) {
		return mergeArray(jsonTraits, " ");
	}

	private String parseKeywords(JsonNode jsonKeywords) {
		String textOut = mergeArray(jsonKeywords, " ");
		if (textOut != null) {
			textOut = textOut.replaceAll(" [0-9]+\\.", ".");
		}
		return textOut;
	}

	private String parseText(JsonNode jsonText) {
		String textOut = null;
		if (jsonText != null) {
			if (jsonText.isArray()) {
				textOut = mergeArray(jsonText, "\n");
			} else if (jsonText.isTextual()) {
				textOut = jsonText.getTextValue();
			}
		}
		if (textOut != null) {
			textOut = processText(textOut);
		}
		return textOut;
	}

	private String parseFlavourText(JsonNode jsonFlavourText) {
		String textOut = null;
		if (jsonFlavourText != null && jsonFlavourText.isTextual()) {
			textOut = processText(jsonFlavourText.getTextValue());
		}
		return textOut;
	}

	private String processText(String textIn) {
		String textOut = StringUtils.trimToNull(textIn);
		if (textOut != null) {
			textOut = textOut.replace("\r\n", "\n");
			textOut = textOut.replace("\n\n", "\n");
			textOut = textOut.replace("`", "\\\"");
		}
		return textOut;
	}

	private String mergeArray(JsonNode jsonArray, String delim) {
		String merged = "";
		if (jsonArray != null && jsonArray.isArray()) {
			Iterator<JsonNode> arrayIter = jsonArray.getElements();
			while (arrayIter.hasNext()) {
				merged += arrayIter.next().getTextValue() + delim;
			}
		}
		return StringUtils.trimToNull(merged);
	}
}