package org.meb.conquestdb.apoka;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
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

	private static final Map<String, String> replaceMap;

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

		replaceMap = new HashMap<String, String>();
		replaceMap.put("sm", "Space-marines");
		replaceMap.put("necrons", "necron");
		replaceMap.put("astra", "astra-militarum");
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

			new ApokaParser().parseCardSet(chain, "Premise of war");

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

		List<String> lines = IOUtils.readLines(new FileInputStream(APOKA_BASE + "pack1.tsv"));
		for (String line : lines) {
			String[] tokens = line.split("\t");

			String type = tokens[4].toLowerCase();
			if ("army unit".equals(type)) {
				type = "army";
			}

			Card card = new Card(type);

			String number = tokens[0];
			card.setNumber(Integer.parseInt(number));

			String quantity = tokens[1];
			card.setQuantity(Integer.parseInt(quantity));

			String faction = tokens[2];
			card.setFaction(replaceMap.get(faction));

			String name = tokens[3];
			card.setName(name);

			String text = tokens[5];
			if (StringUtils.isNotBlank(text)) {
				text = text.replaceAll("\\[C\\]", "[Resource]");
			}
			card.setText(text);

			String cost = tokens[6];
			if (StringUtils.isNotBlank(cost) && !cost.trim().equals("-")) {
				card.setCost(Integer.parseInt(cost.trim()));
			}

			String command = tokens[7];
			if (StringUtils.isNotBlank(command) && !command.trim().equals("-")) {
				card.setCommand(Integer.parseInt(command.trim()));
			}

			String attack = tokens[8];
			if (StringUtils.isNotBlank(attack) && !attack.trim().equals("-")) {
				card.setAttack(Integer.parseInt(attack.trim()));
			}

			String hitPoints = tokens[9];
			if (StringUtils.isNotBlank(hitPoints) && !hitPoints.trim().equals("-")) {
				card.setHitPoints(Integer.parseInt(hitPoints.trim()));
			}

			String shield = tokens[10];
			if (StringUtils.isNotBlank(shield) && !shield.trim().equals("-")) {
				card.setShield(Integer.parseInt(shield.trim()));
			}

			String trait = tokens[11];
			if (StringUtils.isNotBlank(trait)) {
				card.setTrait(trait.trim());
			}

			String unique = tokens[12];
			if (StringUtils.isNoneBlank(unique) && unique.trim().toLowerCase().equals("yes")) {
				card.setUnique(true);
			} else {
				card.setUnique(false);
			}

			String loyal = tokens[13];
			if (StringUtils.isNoneBlank(loyal) && loyal.trim().toLowerCase().equals("yes")) {
				card.setLoyal(true);
			} else {
				card.setLoyal(false);
			}

			if ("warlord".equals(type)) {
				warlord = card;
			} else {
				String signatureSquad = tokens[14];
				if (StringUtils.isNoneBlank(signatureSquad)
						&& signatureSquad.trim().toLowerCase().equals("yes")) {
					card.setWarlordName(warlord.getName());
				}
			}

			card.setSetName(cardSetName);
			handlerChain.handle(card);
		}
	}
}
