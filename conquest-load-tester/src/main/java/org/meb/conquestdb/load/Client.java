package org.meb.conquestdb.load;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class Client implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(Client.class);

	private static final String ROOT_URL = "http://localhost:8080/en";
	private int context;

	public Client(int context) {
		this.context = context;
	}

	public static void main(String[] args) {

		ExecutorService service = Executors.newFixedThreadPool(40);
		for (int i = 0; i < 500; i++) {
			service.execute(new Client(i));
		}

		try {
			service.shutdown();
			service.awaitTermination(3600, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private WebClient client;

	public void run() {
		log.info("begin context: {}", context);

		initializeClient();
		try {
			getHomePage();
			doNothing(20);
			getCardSearchPage();
			doNothing(20);
			HtmlPage publicDecksPage = getPublicDecksPage();
			List<?> links = publicDecksPage.getByXPath("//div[class='deck']");
			for (Object link : links) {
				if (RandomUtils.nextInt(0, 10) < 4) {
					((HtmlAnchor) link).click();
				}
				doNothing(10);
			}
			doNothing(60);
		} catch (FailingHttpStatusCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log.info("end context: {}", context);
	}

	private void doNothing(int max) {
		int min = (int) (max / 4d);
		try {
			Thread.sleep(RandomUtils.nextInt(min, max) * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void initializeClient() {
		client = new WebClient(BrowserVersion.getDefault());

		WebClientOptions options = client.getOptions();
		options.setTimeout(10000);
		options.setRedirectEnabled(true);
		options.setJavaScriptEnabled(true);
		options.setThrowExceptionOnFailingStatusCode(false);
		options.setThrowExceptionOnScriptError(false);
		options.setCssEnabled(false);
		options.setUseInsecureSSL(true);
	}

	private void getHomePage()
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		client.getPage(ROOT_URL);
	}

	private void getCardSearchPage()
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		client.getPage(ROOT_URL + "/card/search");
	}

	private HtmlPage getPublicDecksPage()
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		return client.getPage(ROOT_URL + "/public/deck");
	}

	private void getPublicDeckPage(Long id)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		client.getPage(ROOT_URL + "/public/deck/" + id.toString());
	}
}
