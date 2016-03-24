package org.meb.conquestdb.db;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.commons.collections4.Predicate;
import org.meb.conquest.db.dao.CardBaseDao;
import org.meb.conquest.db.dao.CardSetBaseDao;
import org.meb.conquest.db.dao.CycleBaseDao;
import org.meb.conquest.db.dao.DomainBaseDao;
import org.meb.conquest.db.model.CardBase;
import org.meb.conquest.db.model.CardSetBase;
import org.meb.conquest.db.model.CycleBase;
import org.meb.conquest.db.model.DomainBase;

public abstract class AbstractLoader {

	protected EntityManager em;
	protected DomainBaseDao dbDao;
	protected CycleBaseDao ccbDao;
	protected CardSetBaseDao csbDao;
	protected CardBaseDao cbDao;

	private boolean globalRollback = true;

	public AbstractLoader() {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("conquest-pu");
		em = emf.createEntityManager();
		dbDao = new DomainBaseDao(em);
		ccbDao = new CycleBaseDao(em);
		csbDao = new CardSetBaseDao(em);
		cbDao = new CardBaseDao(em);
	}

	protected static void mkdir(String name) {
		if (!new File(name).exists()) {
			new File(name).mkdir();
		}
	}

	protected void beginTransaction() {
		EntityTransaction tx = em.getTransaction();
		if (tx.isActive()) {
			throw new IllegalStateException("Transaction is active");
		}
		tx.begin();
	}

	protected void endTransaction(boolean commit) {
		if (commit) {
			em.getTransaction().commit();
		} else {
			em.getTransaction().rollback();
		}
	}

	protected void endTransaction() {
		endTransaction(globalRollback);
	}

	protected void cleanUp() {
		em.getEntityManagerFactory().close();
	}

	public List<DomainBase> readDomainsFromDatabase() {
		return readDomainsFromDatabase(null);
	}

	public List<DomainBase> readDomainsFromDatabase(Predicate<DomainBase> keepPredicate) {
		List<DomainBase> dbList = dbDao.find(new DomainBase());
		if (keepPredicate != null) {
			Iterator<DomainBase> iter = dbList.iterator();
			while (iter.hasNext()) {
				DomainBase db = iter.next();
				if (!keepPredicate.evaluate(db)) {
					iter.remove();
				}
			}
		}
		return dbList;
	}

	public List<CycleBase> readCyclesFromDatabase() {
		return ccbDao.find(new CycleBase());
	}

	public List<CardSetBase> readCardSetsFromDatabase() {
		return csbDao.find(new CardSetBase());
	}

	public List<CardBase> readCardsFromDatabase() {
		return readCardsFromDatabase(null);
	}

	public List<CardBase> readCardsFromDatabase(Predicate<CardBase> keepPredicate) {
		List<CardBase> cbList = cbDao.find(new CardBase());
		if (keepPredicate != null) {
			Iterator<CardBase> iter = cbList.iterator();
			while (iter.hasNext()) {
				CardBase cb = iter.next();
				if (!keepPredicate.evaluate(cb)) {
					iter.remove();
				}
			}
		}
		return cbList;
	}
}