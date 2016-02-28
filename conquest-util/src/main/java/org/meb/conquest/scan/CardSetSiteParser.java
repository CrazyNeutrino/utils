package org.meb.conquest.scan;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.meb.conquest.scan.model.CardSetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CardSetSiteParser {

	private static final Logger log = LoggerFactory.getLogger(CardSetSiteParser.class);
	private static final String SETS_SITE_URL = "http://www.cardgamedb.com/index.php/lotr/lord-of-the-rings-card-spoiler";

	public static void main(String[] args) {
		try {
			// List<CardSetInfo> csInfos = new
			// CardSetSiteParser().parseCardSetUrls();
			List<CardSetInfo> csInfos = new ArrayList<CardSetInfo>();
			csInfos.add(new CardSetInfo(
					"Boundless Hate",
					"http://www.cardgamedb.com/index.php/wh40kconquest/conquest.html/_/planetfall-cycle/boundless-hate/"));
			CardHandlerChain chain = new CardHandlerChain();
			chain.addCardHandler(new WriteLogCardHandler());
			WriteJsonCardHandler jsonHandler = new WriteJsonCardHandler("en");
			chain.addCardHandler(jsonHandler);
			new CardSiteParser().parseSites(csInfos, chain);
			PrintWriter writer = new PrintWriter("card.json");
			writer.write(jsonHandler.toString());
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<CardSetInfo> parseCardSetUrls() throws IOException {
		List<CardSetInfo> csInfos = new ArrayList<CardSetInfo>();

		Document siteDoc = Jsoup.connect(SETS_SITE_URL).timeout(20000).get();
		Elements tdSets = siteDoc.select("td.col_c_forum");

		int count = 0;

		Iterator<Element> iter = tdSets.iterator();
		while (iter.hasNext()/* && count++ < 2*/) {
			Element tdSet = iter.next();
			Elements aSets = tdSet.select("ol > li > a");
			if (aSets.size() == 0) {
				aSets = tdSet.select("a");
			}

			Iterator<Element> aSetIter = aSets.iterator();
			while (aSetIter.hasNext()) {
				Element aSet = aSetIter.next();
				String url = aSet.attr("href");
				String name = aSet.text();
				log.info("card set: {} -> {}", name, url);
				// String f =
				// "[{\"en\": [\"%1$s\", \"%2$s\"], \"pl\": [\"\", \"\"], \"de\": [\"\", \"\"]}]";
				// System.out.println(String.format(f,
				// cardSetNameToSymbol(aSet.text()), aSet.text()));
				csInfos.add(new CardSetInfo(name, url));
			}
		}
		return csInfos;
	}

	@SuppressWarnings("unused")
	private String cardSetNameToSymbol(String name) {
		String symbol = "";
		String[] tokens = name.split("[ -]");
		for (String token : tokens) {
			symbol += token.charAt(0);
		}
		return symbol;
	}
}
