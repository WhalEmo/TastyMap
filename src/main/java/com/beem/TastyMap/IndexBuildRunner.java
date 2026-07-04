package com.beem.TastyMap;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IndexBuildRunner implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        SearchSession searchSession = Search.session(entityManager);

        searchSession.massIndexer()
                .startAndWait();

        System.out.println(">>>> Hibernate Search İndeksleme Tamamlandı!");
    }
}
