package org.meb.oneringdb.db;

import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.commons.collections4.Predicate;
import org.meb.oneringdb.db.dao.CardBaseDao;
import org.meb.oneringdb.db.dao.CardSetBaseDao;
import org.meb.oneringdb.db.dao.CycleBaseDao;
import org.meb.oneringdb.db.dao.DomainBaseDao;
import org.meb.oneringdb.db.dao.EncounterSetBaseDao;
import org.meb.oneringdb.db.dao.ScenEnstLinkDao;
import org.meb.oneringdb.db.dao.ScenarioBaseDao;
import org.meb.oneringdb.db.model.CardBase;
import org.meb.oneringdb.db.model.CardSetBase;
import org.meb.oneringdb.db.model.CycleBase;
import org.meb.oneringdb.db.model.DomainBase;
import org.meb.oneringdb.db.model.EncounterSetBase;
import org.meb.oneringdb.db.model.ScenEnstLink;
import org.meb.oneringdb.db.model.ScenarioBase;
import org.meb.oneringdb.db.query.EncounterSetBaseQuery;
import org.meb.oneringdb.db.query.ScenEnstLinkQuery;
import org.meb.oneringdb.db.query.ScenarioBaseQuery;

public abstract class AbstractLoader {

	protected EntityManager em;
	protected DomainBaseDao dbDao;
	protected CycleBaseDao ccbDao;
	protected CardSetBaseDao csbDao;
	protected EncounterSetBaseDao esbDao;
	protected ScenarioBaseDao sbDao;
	protected ScenEnstLinkDao selDao;
	protected CardBaseDao cbDao;

	private boolean globalRollback = true;

	protected void emInitialize() {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("oneringdb-pu");
		em = emf.createEntityManager();
		dbDao = new DomainBaseDao(em);
		ccbDao = new CycleBaseDao(em);
		csbDao = new CardSetBaseDao(em);
		cbDao = new CardBaseDao(em);
		esbDao = new EncounterSetBaseDao(em);
		sbDao = new ScenarioBaseDao(em);
		selDao = new ScenEnstLinkDao(em);
	}
	
	protected void emFinalize() {
		if (em != null) {
			em.getEntityManagerFactory().close();
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

	public List<EncounterSetBase> readEncounterSetsFromDatabase() {
		EncounterSetBaseQuery query = new EncounterSetBaseQuery();
		query.getSorting().setSortingAsc("cardSetBase.sequence");
		query.getSorting().setSortingAsc("techName");
		return esbDao.find(query);
	}

	public List<ScenarioBase> readScenariosFromDatabase() {
		ScenarioBaseQuery query = new ScenarioBaseQuery();
		query.getSorting().setSortingAsc("cardSetBase.sequence");
		query.getSorting().setSortingAsc("sequence");
		return sbDao.find(query);
	}

	public List<ScenEnstLink> readScenEnstLinksFromDatabase() {
		ScenEnstLinkQuery query = new ScenEnstLinkQuery();
		query.getSorting().setSortingAsc("scenarioBase.cardSetBase.sequence");
		query.getSorting().setSortingAsc("scenarioBase.sequence");
		query.getSorting().setSortingAsc("sequence");
		return selDao.find(query);
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