package com.beem.TastyMap.userRelated.search;

import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.userRelated.block.BlockRepo;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.ArrayList;

@Service
public class UserSearchService {

    private final EntityManager entityManager;
    private final BlockRepo blockRepo;

    public UserSearchService(EntityManager entityManager, BlockRepo blockRepo) {
        this.entityManager = entityManager;
        this.blockRepo = blockRepo;
    }

    public List<UserSearchDTO> searchUsers(String keyword, Long myUserId, int page, int size) {
        // 1. Engellenen, engelleyen ve kullanıcının kendi ID'sini al
        List<Long> excludedIds = new ArrayList<>(blockRepo.findExcludedUserIds(myUserId));
        excludedIds.add(myUserId);

        SearchSession searchSession = Search.session(entityManager);
        String lowerKeyword = keyword.toLowerCase();

        return searchSession.search(UserEntity.class)
                .select(UserSearchDTO.class)
                .where(f -> f.bool(b -> {
                    // MUST NOT: Dışlanacak ID'leri indeks seviyesinde yoksay (Sayfalamayı korur)
                    if (!excludedIds.isEmpty()) {
                        b.mustNot(f.id().matchingAny(excludedIds));
                    }

                    // MUST: Aşağıdaki kural zincirinden (Should) en az biri tutmalı
                    b.must(f.bool(searchLogic -> {

                        // KURAL 1: Kullanıcı Adı Tam Eşleşme (En yüksek öncelik - Boost: 10.0)
                        searchLogic.should(f.match().field("username").matching(keyword).boost(10.0f));

                        // KURAL 2: Kullanıcı Adı Önek (Örn: "bey" yazınca "beyzatkc" bulur - Boost: 5.0)
                        searchLogic.should(f.wildcard().field("username").matching(lowerKeyword + "*").boost(5.0f));

                        // KURAL 3: İsim ve Soyisim Eşleşmeleri
                        searchLogic.should(f.match().field("name").matching(keyword).boost(4.0f));
                        searchLogic.should(f.wildcard().field("name").matching(lowerKeyword + "*").boost(3.0f));
                        searchLogic.should(f.wildcard().field("surname").matching(lowerKeyword + "*").boost(1.0f));

                        // KURAL 4: Fuzzy Search (Harf Hatası Toleransı)
                        // ÖNEMLİ: Sadece 3 harften uzun kelimelerde devreye sokarak performansı koruyoruz.
                        if (keyword.length() > 3) {
                            // fuzzy(1) -> "beyzz" yazıldığında 1 harf değiştirerek "beyza"yı bulur (Boost: 2.0)
                            searchLogic.should(f.match().field("username").matching(keyword).fuzzy(1).boost(2.0f));
                            searchLogic.should(f.match().field("name").matching(keyword).fuzzy(1).boost(1.0f));
                        }
                    }));
                }))
                .fetchHits(page * size, size); // Ofset ve limit ile hatasız sayfalama
    }
}
