package org.meb.conquest.db;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.apache.commons.collections4.Predicate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.meb.conquest.db.model.CardBase;
import org.meb.conquest.db.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import com.gargoylesoftware.htmlunit.BrowserVersion;
// import com.gargoylesoftware.htmlunit.Page;
// import com.gargoylesoftware.htmlunit.WebClient;
// import com.gargoylesoftware.htmlunit.html.HtmlImage;

public class ImageLoader extends AbstractLoader {

	private static final Logger log = LoggerFactory.getLogger(ImageLoader.class);

	protected static final String IMAGE_BASE;
	protected static final String IMAGE_BASE_BORWOL;
	protected static final String IMAGE_BASE_CARDGAME_DB;

	static {
		String tmpImageBase = System.getProperty("image.home");
		if (StringUtils.isBlank(tmpImageBase)) {
			throw new IllegalStateException("Image home not set");
		}
		if (tmpImageBase.trim().endsWith("/")) {
			IMAGE_BASE = tmpImageBase;
		} else {
			IMAGE_BASE = tmpImageBase + "/";
		}
		IMAGE_BASE_BORWOL = IMAGE_BASE + "_raw_/card/pl/borwol/";
		IMAGE_BASE_CARDGAME_DB = IMAGE_BASE + "_raw_/card/en/cgdb/";
	}

	public static void main(String[] args) throws IOException {
		ImageLoader loader = new ImageLoader();
		try {
			if (args[0].equals("--borwol")) {
				loader.loadFromBorwol();
			} else if (args[0].equals("--cardgamedb")) {
				loader.loadFromCardgameDB();
			} else {
				throw new IllegalArgumentException("Invalid argument");
			}
		} finally {
			loader.cleanUp();
		}
	}

	public void loadFromBorwol() throws MalformedURLException, IOException {
		// WebClient client = new WebClient(BrowserVersion.CHROME);

		List<CardBase> cards = readCardsFromDatabase();
		for (CardBase card : cards) {
			String crstPathPart = StringUtils.leftPad(card.getCardSetBase().getSequence().toString(), 2, "0");
			crstPathPart += "-" + card.getCardSetBase().getTechName();
			String urlCardPathPart = StringUtils.leftPad(card.getNumber().toString(), 3, "0") + ".png";
			String fileCardPathPart = StringUtils.leftPad(card.getNumber().toString(), 3, "0");
			fileCardPathPart += "-" + card.getTechName() + ".png";

			String urlString = "http://podboj.znadplanszy.pl/wp-content/uploads/sites/56/2014/09/" + urlCardPathPart;
			// InputStream inputStream = new URL(urlString).openStream();

			File file = new File(IMAGE_BASE_BORWOL + crstPathPart + "/" + fileCardPathPart);
			if (/*card.getNumber() < 50 || card.getNumber() > 80 || */file.exists()) {
				continue;
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			try {

				// Page page = client.getPage(urlString);
				// page.getWebResponse().get
				URLConnection connection = new URL(urlString).openConnection();
				connection.setRequestProperty("Referer",
						"http://podboj.znadplanszy.pl/2014/09/15/warhammer-40000-podboj-tau-pl/");
				connection
						.setRequestProperty("User-agent",
								"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.120 Safari/537.36");
				InputStream inputStream = connection.getInputStream();

				OutputStream outputStream = new FileOutputStream(file);
				IOUtils.copy(inputStream, outputStream);
				IOUtils.closeQuietly(outputStream);
				IOUtils.closeQuietly(inputStream);
			} catch (Exception e) {
				e.printStackTrace();
				// log.warn("Resource not loaded: {}, cause: {}", urlString,
				// e.getClass().getCanonicalName() + " " + e.getMessage());
			}
		}
	}

	public void loadFromCardgameDB() throws MalformedURLException, IOException {
		Predicate<CardBase> keepPredicate = new Predicate<CardBase>() {

			@Override
			public boolean evaluate(CardBase cb) {
				return cb.getCardSetBase().getTechName().equals("boundless-hate");
			}
			
		};
		List<CardBase> cards = readCardsFromDatabase(keepPredicate);
		for (CardBase card : cards) {
			String crstPathPart = StringUtils.leftPad(card.getCardSetBase().getSequence().toString(), 2, "0");
			crstPathPart += "-" + Utils.techNameToAcronym(card.getCardSetBase().getTechName());
			String urlCardPathPart = "med_WHK10_" + card.getNumber().toString() + ".jpg";
			String fileCardPathPart = StringUtils.leftPad(card.getNumber().toString(), 3, "0");
			fileCardPathPart += "-" + card.getTechName() + ".jpg";

			String urlString = "http://s3.amazonaws.com/LCG/40kconquest/" + urlCardPathPart;

			File file = new File(IMAGE_BASE_CARDGAME_DB + crstPathPart + "/" + fileCardPathPart);
			if (file.exists()) {
				continue;
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			try {
				URLConnection connection = new URL(urlString).openConnection();
				InputStream inputStream = connection.getInputStream();

				OutputStream outputStream = new FileOutputStream(file);
				IOUtils.copy(inputStream, outputStream);
				IOUtils.closeQuietly(outputStream);
				IOUtils.closeQuietly(inputStream);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
