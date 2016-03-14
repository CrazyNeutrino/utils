package org.meb.conquest.scan;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.meb.conquest.db.DataLoader;
import org.meb.conquest.db.model.CardBase;
import org.meb.conquest.db.model.CardSetBase;

public class ImageLoader {

	private static final String CARD_URL_BASE = "http://www.cardgamedb.com/forums/uploads/lotr/med_${name}-${set}.jpg";
	private static final String ENST_URL_BASE = "http://deckbauer.telfador.net/assets/cardgames/spheres/lotr-${name}.png";

	public static void main(String[] args) {
		// loadCardImagesFromCardgamedb();
	}

	private static void loadCardImagesFromCardgamedb() {
		List<CardBase> cards = new DataLoader().readCardsFromDatabase();

		int count = 0;

		final ArrayList<String> urls = new ArrayList<String>();
		for (CardBase card : cards) {
			String cbTechName = card.getTechName();
			CardSetBase csb = card.getCardSetBase();
			String cslSymbol = csb.getLangItems().get("en").getSymbol().toLowerCase();
			if (cslSymbol.equals("cs")) {
				cslSymbol = "core";
			}
			String path = System.getProperty("dropbox.home")
					+ "/workspace/lotrlcg/images/cards/med_" + cbTechName + "-" + cslSymbol
					+ ".jpg";
			if (!new File(path).exists()) {
				urls.add(CARD_URL_BASE.replace("${name}", cbTechName).replace("${set}", cslSymbol));
				System.out.println(path);
			}
		}

		for (String url : urls) {
			System.out.println(url);
		}

		final AtomicInteger idx = new AtomicInteger(0);

		Thread[] threads = new Thread[4];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(new Runnable() {

				@Override
				public void run() {

					int localIdx;

					while ((localIdx = idx.getAndIncrement()) < urls.size()) {
						String url = urls.get(localIdx);
						try {
							Response response = Jsoup.connect(url).ignoreContentType(true)
									.execute();
							FileOutputStream stream = new FileOutputStream("d:/_lotr_img/"
									+ url.substring(url.lastIndexOf('/') + 1));
							stream.write(response.bodyAsBytes());
							stream.flush();
							stream.close();
						} catch (IOException e) {
							System.err.println(url);
						}
					}
				}
			});
		}

		for (Thread thread : threads) {
			thread.start();
		}

		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
