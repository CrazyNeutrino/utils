package org.meb.conquestdb.db;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.Predicate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.meb.conquest.db.model.CardBase;
import org.meb.conquest.db.model.CardSetBase;
import org.meb.conquest.db.model.CardType;
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

	private class ImageInfo {
		private String url;
		private String fileName;
		private String dirName;
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
				connection.setRequestProperty("Referer", "http://deckbauer.telfador.net/");
				connection.setRequestProperty("User-agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.120 Safari/537.36");
				InputStream inputStream = connection.getInputStream();

				File dir = new File(IMAGE_BASE + "_raw/card/de/" + imageInfo.dirName);
				File file = new File(dir, imageInfo.fileName);
				if (file.exists()) {
					return;
				}

				synchronized (ImageLoader.this) {
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

	static {
		String home = System.getProperty("home");
		if (StringUtils.isBlank(home)) {
			throw new IllegalStateException("Home not set");
		}

		if (home.trim().endsWith("/")) {
			IMAGE_BASE = home.trim() + "image/";
		} else {
			IMAGE_BASE = home.trim() + "/image/";
		}
		IMAGE_BASE_BORWOL = IMAGE_BASE + "_raw/card/pl/borwol/";
		IMAGE_BASE_CARDGAME_DB = IMAGE_BASE + "_raw/card/en/cgdb/";
	}

	public static void main(String[] args) throws IOException {
		ImageLoader loader = new ImageLoader();
		try {
			if (args[0].equals("--load-images-borwol")) {
				loader.loadFromBorwol();
			} else if (args[0].equals("--load-images-cgdb")) {
				loader.loadFromCGDB();
			} else if (args[0].equals("--load-images-deckbauer")) {
				loader.loadFromDeckbauer();
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
			String crstPathPart = StringUtils
					.leftPad(card.getCardSetBase().getSequence().toString(), 2, "0");
			crstPathPart += "-" + card.getCardSetBase().getTechName();
			String urlCardPathPart = StringUtils.leftPad(card.getNumber().toString(), 3, "0")
					+ ".png";
			String fileCardPathPart = StringUtils.leftPad(card.getNumber().toString(), 3, "0");
			fileCardPathPart += "-" + card.getTechName() + ".png";

			String urlString = "http://podboj.znadplanszy.pl/wp-content/uploads/sites/56/2014/09/"
					+ urlCardPathPart;
			// InputStream inputStream = new URL(urlString).openStream();

			File file = new File(IMAGE_BASE_BORWOL + crstPathPart + "/" + fileCardPathPart);
			if (/* card.getNumber() < 50 || card.getNumber() > 80 || */file.exists()) {
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
				connection.setRequestProperty("User-agent",
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

	public void loadFromCGDB() throws MalformedURLException, IOException {
		emInitialize();

		Predicate<CardBase> keepPredicate = new Predicate<CardBase>() {

			@Override
			public boolean evaluate(CardBase cb) {
				return cb.getCardSetBase().getTechName().equals("the-warp-unleashed");
			}

		};
		List<CardBase> cards = readCardsFromDatabase(keepPredicate);
		for (CardBase card : cards) {
			String crstPathPart = StringUtils
					.leftPad(card.getCardSetBase().getSequence().toString(), 2, "0");
			crstPathPart += "-" + Utils.techNameToAcronym(card.getCardSetBase().getTechName());
			String urlCardPathPart = "med_WHK21_" + card.getNumber().toString() + ".jpg";
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

	public void loadFromDeckbauer() throws MalformedURLException, IOException {
		emInitialize();

		beginTransaction();

		StringBuilder myLog = new StringBuilder();

		List<CardSetBase> csbList = readCardSetsFromDatabase();
		// Map<String, CardSetBase> csbMap = Maps.uniqueIndex(csbList,
		// Functions.CardSetBaseTechName);
		List<CardBase> cbList = readCardsFromDatabase();
		// Map<String, CardBase> cbMap = Maps.uniqueIndex(cbList,
		// Functions.CardBaseComposite);

		final List<ImageInfo> imageInfos = new ArrayList<>();

		try {
			for (CardBase cb : readCardsFromDatabase()) {
				CardSetBase csb = cb.getCardSetBase();
				String crstTechName = csb.getTechName();
				String cycleTechName = null;
				if (csb.getCycleBase() != null) {
					cycleTechName = csb.getCycleBase().getTechName();
				}

				String packNumber;
				if (crstTechName.equals("core-set")) {
					packNumber = "001";
				} else if ("warlord-cycle".equals(cycleTechName)) {
					packNumber = "002";
				} else if (crstTechName.equals("the-great-devourer")) {
					packNumber = "003";
				} else if ("planetfall-cycle".equals(cycleTechName)) {
					packNumber = "004";
				} else {
					continue;
				}

				boolean warlord = cb.getType() == CardType.WARLORD;
				imageInfos.add(createImageInfo(crstTechName, csb.getSequence(), cb.getTechName(),
						cb.getNumber(), packNumber, warlord, false));
				if (warlord) {
					imageInfos.add(createImageInfo(crstTechName, csb.getSequence(),
							cb.getTechName(), cb.getNumber(), packNumber, warlord, true));
				}
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

			endTransaction(false);
		} finally {
			log.info(myLog.toString());
		}
	}

	private ImageInfo createImageInfo(String crstTechName, Integer crstSequence,
			String cardTechName, Integer cardNumber, String packPrefix, boolean warlord,
			boolean back) {

		String tmp = warlord ? (back ? "b" : "a") : "";
		String url = "http://deckbauer.telfador.net/assets/cardgames/whc/" + packPrefix + "/"
				+ StringUtils.leftPad(cardNumber.toString(), 3, "0") + tmp + ".png";

		ImageInfo ii = new ImageInfo();
		ii.url = url;
		ii.fileName = StringUtils.leftPad(cardNumber.toString(), 3, "0") + "-" + cardTechName;
		if (back) {
			ii.fileName += "-b";
		}
		ii.fileName += "." + ii.url.substring(ii.url.lastIndexOf('.') + 1).toLowerCase();
		ii.dirName = StringUtils.leftPad(crstSequence.toString(), 2, "0") + "-" + crstTechName;
		return ii;
	}
}
