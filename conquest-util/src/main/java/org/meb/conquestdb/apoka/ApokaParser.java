package org.meb.conquestdb.apoka;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.meb.conquestdb.scan.CardHandlerChain;
import org.meb.conquestdb.scan.WriteJsonCardHandler;
import org.meb.conquestdb.scan.WriteLogCardHandler;
import org.meb.conquestdb.scan.model.Card;

public class ApokaParser {

	protected static final String APOKA_BASE;

	private static final Map<String, String> correctedFactions;

	static {
		String home = System.getProperty("home");
		if (StringUtils.isBlank(home)) {
			throw new IllegalStateException("Home not set");
		}
		if (home.trim().endsWith("/")) {
			APOKA_BASE = home + "data/apoka/";
		} else {
			APOKA_BASE = home + "/data/apoka/";
		}
		createDirectory(APOKA_BASE);

		correctedFactions = new HashMap<String, String>();
		correctedFactions.put("de", "dark-eldar");
		correctedFactions.put("dark e", "dark-eldar");
		correctedFactions.put("sm", "space-marines");
		correctedFactions.put("necrons", "necron");
		correctedFactions.put("astra", "astra-militarum");
		// replaceMap.put("[C]", "[Resource]");
		// replaceMap.put("", "");
		// replaceMap.put("", "");
		// replaceMap.put("", "");
	}

	private static void createDirectory(String name) {
		if (!new File(name).exists()) {
			new File(name).mkdir();
		}
	}

	public static void main(String[] args) {
		try {
			CardHandlerChain chain = new CardHandlerChain();
			chain.addCardHandler(new WriteLogCardHandler());
			WriteJsonCardHandler jsonHandler = new WriteJsonCardHandler("en");
			chain.addCardHandler(jsonHandler);

			new ApokaParser().parseCardSet(chain, "Defenders of the Faith");

			PrintWriter writer = new PrintWriter("card.json");
			writer.write(jsonHandler.toString());
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void parseCardSet(CardHandlerChain handlerChain, String cardSetName)
			throws FileNotFoundException, IOException {

		Card warlord = null;

		List<String> lines = IOUtils.readLines(new FileInputStream(APOKA_BASE + "deluxe01.tsv"));
		for (String line : lines) {
			String[] tokens = line.split("\t");
			if (tokens.length < 16) {
				tokens = Arrays.copyOf(tokens, 16);
			}

			String type = tokens[3].toLowerCase();
			if ("army unit".equals(type)) {
				type = "army";
			}

			Card card = new Card(type);

			String number = tokens[0];
			card.setNumber(Integer.parseInt(number));

			String faction = tokens[1];
			card.setFaction(getCorrectedFaction(faction));

			String name = tokens[2];
			card.setName(name);

			String text = tokens[4];
			if (StringUtils.isNotBlank(text)) {
				text = text.replaceAll("\\[C\\]", "[Resource]");
				text = text.replaceAll(" F ", " [Faith] ");
			}
			card.setText(text);

			String cost = tokens[5];
			if (StringUtils.isNotBlank(cost) && !cost.trim().equals("-")) {
				card.setCost(Integer.parseInt(cost.trim()));
			}

			String command = tokens[6];
			if (StringUtils.isNotBlank(command) && !command.trim().equals("-")) {
				card.setCommand(Integer.parseInt(command.trim()));
			}

			String attack = tokens[7];
			if (StringUtils.isNotBlank(attack) && !attack.trim().equals("-")) {
				card.setAttack(Integer.parseInt(attack.trim()));
			}

			String hitPoints = tokens[8];
			if (StringUtils.isNotBlank(hitPoints) && !hitPoints.trim().equals("-")) {
				card.setHitPoints(Integer.parseInt(hitPoints.trim()));
			}

			String shield = tokens[9];
			if (StringUtils.isNotBlank(shield) && !shield.trim().equals("-")) {
				card.setShield(Integer.parseInt(shield.trim()));
			}

			String trait = tokens[10];
			if (StringUtils.isNotBlank(trait)) {
				card.setTrait(trait.trim());
			}

			String unique = tokens[11];
			if (StringUtils.isNoneBlank(unique) && unique.trim().toLowerCase().equals("yes")) {
				card.setUnique(true);
			} else {
				card.setUnique(false);
			}

			String loyal = tokens[12];
			if (StringUtils.isNoneBlank(loyal) && loyal.trim().toLowerCase().equals("yes")) {
				card.setLoyal(true);
			} else {
				card.setLoyal(false);
			}

			if ("warlord".equals(type)) {
				warlord = card;
				// card.setStartingHandSize(7);
				// card.setStartingResources(7);
			} else {
				String signatureSquad = tokens[13];
				if (StringUtils.isNoneBlank(signatureSquad)
						&& signatureSquad.trim().toLowerCase().equals("yes")) {
					card.setWarlordName(warlord.getName());
				}
			}

			String quantity = tokens[14];
			if (StringUtils.isNotBlank(quantity)) {
				if ("warlord".equals(type)) {
					String[] tmp = quantity.split("/");
					card.setQuantity(1);
					card.setStartingHandSize(Integer.valueOf(tmp[0]));
					card.setStartingResources(Integer.valueOf(tmp[1]));
				} else {
					card.setQuantity(Integer.valueOf(quantity));
				}
			} else {
				card.setQuantity(3);
			}

			String octgnId = tokens[15];
			if (StringUtils.isNotBlank(octgnId)) {
				card.setOctgnId(octgnId.trim());
			}

			card.setSetName(cardSetName);
			handlerChain.handle(card);
		}
	}

	private String getCorrectedFaction(String faction) {
		faction = faction.toLowerCase();
		if (correctedFactions.get(faction) != null) {
			faction = correctedFactions.get(faction);
		}
		return faction;
	}
}
