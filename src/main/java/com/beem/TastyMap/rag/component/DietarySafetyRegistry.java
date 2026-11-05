package com.beem.TastyMap.rag.component;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DietarySafetyRegistry {
    private DietarySafetyRegistry() {
    }


    public static final Map<String, String> WORD_ROOT_ALIASES = Map.of(
            "hamburger", "burger",
            "kahve", "coffee",
            "dondurmacı", "dondurma",
            "köfteci", "köfte"
    );

    public static final Set<String> NON_DINING_EXCLUSIONS = Set.of(
            "okey salonu", "okey", "kıraathane", "kiraathane", "kiraathanesi",
            "oyun salonu", "simulator", "simülatör", "espor", "e-spor",
            "internet cafe", "internet kafe", "playstation", "ps cafe",
            "afacan park", "parkı", "parki", "lunapark", "e spor", "spor merkezi",
            "kıraathanesi"
    );

    public static final Set<String> MEAT_EXCLUSIONS = Set.of(
            "etliekmek", "etli ekmek", "kebap", "kebapçı", "kebapci", "ciğer", "ciger",
            "ciğerci", "cigerci", "kokoreç", "kokorec", "döner", "doner", "dönerci",
            "donerci", "köfte", "kofte", "köfteci", "kofteci", "tandır", "tandir",
            "tavuk", "wings", "tavukçu", "tavukcu", "aspava", "sucuk", "kavurma",
            "büryan", "iskender", "pöç", "sakadat", "sakatat", "işkembe", "iskembe", "paça", "paca",
            "ocakbaşı", "ocakbasi", "dürüm", "ızgara", "izgara", "steak", "antrikot", "bonfile",
            "pirzola", "küşleme", "balık", "balik", "balıkçı", "balikci", "hamsi", "levrek", "çipura", "cipura",
            "somon", "alabalık", "alabalik", "mezgit", "kalamar", "karides", "midye",
            "ahtapot", "ıstakoz", "istakoz", "yengeç", "yengec", "deniz ürünleri"
    );

    public static final Set<String> DIABETES_EXCLUSIONS = Set.of(
            "tatlı", "tatli", "tatlıcı", "tatlici", "baklava", "baklavacı", "kadayıf",
            "kadayif", "waffle", "pasta", "pastane", "lokma", "lokmacı", "dondurma",
            "dondurmacı", "çikolata", "cikolata", "şerbet",
            "peynir", "kasar", "kaşar", "tereyağ", "tereyag", "yoğurt", "yogurt",
            "kaymak", "krema", "süt", "sut", "yumurta", "omlet", "menemen",
            "patisserie", "pastry"
    );

    public static final Map<String, List<String>> ALLERGEN_KEYWORD_MAP = Map.ofEntries(
            // 1. Fıstık & Yer Fıstığı (Peanuts)
            Map.entry("fıstık", List.of("fıstık", "fistik", "yer fıstığı", "antep fıstığı", "baklava", "ezme", "kaju")),
            Map.entry("fistik", List.of("fıstık", "fistik", "yer fıstığı", "antep fıstığı", "baklava", "ezme", "kaju")),

            // 2. Ağaç Kuruyemişleri (Tree Nuts - Fındık, Ceviz, Badem vb.)
            Map.entry("kuruyemiş", List.of("fındık", "findik", "ceviz", "badem", "kaju", "antep fıstığı", "çam fıstığı", "nuga", "pralin")),
            Map.entry("fındık", List.of("fındık", "findik", "nutella", "pralin")),
            Map.entry("findik", List.of("fındık", "findik", "nutella", "pralin")),
            Map.entry("ceviz", List.of("ceviz", "baklava", "kadayıf")),
            Map.entry("badem", List.of("badem", "marzipan", "keşkül")),

            // 3. Gluten & Tahıllar (Çölyak / Buğday, Arpa, Çavdar)
            Map.entry("gluten", List.of("ekmek", "fırın", "firin", "unlu mamul", "börek", "borek", "pide", "simit",
                    "mantı", "manti", "makarna", "erişte", "bulgur", "un", "hamur", "pizza", "lavaş", "lavas", "gözleme", "gozleme",
                    "dürüm", "patisserie", "pastry", "bakery", "baker", "pasta", "tatlı", "waffle"
            )),
            Map.entry("buğday", List.of("buğday", "bugday", "un", "ekmek", "makarna", "bulgur", "irmik")),

            // 4. Süt & Laktoz (Süt Ürünleri)
            Map.entry("laktoz", List.of("süt", "sut", "peynir", "yoğurt", "yogurt", "kaymak", "krema",
                    "tereyağ", "tereyag", "kaşar", "kasar", "labne", "lor", "çökelek", "ayran", "cacık")),
            Map.entry("süt", List.of("süt", "sut", "peynir", "yoğurt", "yogurt", "kaymak", "krema",
                    "tereyağ", "tereyag", "kaşar", "ayran")),
            Map.entry("sut", List.of("süt", "sut", "peynir", "yoğurt", "kaymak", "krema", "tereyağ")),

            // 5. Yumurta (Egg)
            Map.entry("yumurta", List.of("yumurta", "omlet", "menemen", "mayonez", "beze", "pasta")),

            // 6. Kabuklu Deniz Ürünleri & Yumuşakçalar (Crustaceans & Molluscs)
            Map.entry("deniz", List.of("balık", "balik", "karides", "kalamar", "midye", "ahtapot", "havyar", "ıstakoz", "istakoz", "yengeç")),
            Map.entry("balık", List.of("balık", "balik", "balıkçı", "hamsi", "levrek", "çipura", "mezgit", "somon", "alabalık", "ton balığı")),
            Map.entry("balik", List.of("balık", "balik", "balıkçı", "karides", "kalamar", "midye")),
            Map.entry("kabuklu", List.of("karides", "kalamar", "midye", "ıstakoz", "istakoz", "yengeç", "kokteyl")),

            // 7. Soya (Soybeans) - Asya mutfağı ve hazır soslar
            Map.entry("soya", List.of("soya", "tofu", "edamame", "soya sosu", "miso")),

            // 8. Susam (Sesame) - Fırın, simit ve tahin türevleri
            Map.entry("susam", List.of("susam", "tahin", "simit", "humus", "tahinli")),
            Map.entry("tahin", List.of("tahin", "tahinli", "humus", "piyaz")),

            // 9. Hardal (Mustard) - Soslar ve marine etler
            Map.entry("hardal", List.of("hardal", "sos")),

            // 10. Kereviz (Celery)
            Map.entry("kereviz", List.of("kereviz")),

            // 11. Bakliyat / Bakla (Favizm G6PD eksikliği)
            Map.entry("bakla", List.of("bakla", "fava")),

            // 12. Kükürt Dioksit & Sülfitler (Kuru meyveler, şarap, fermente ürünler)
            Map.entry("sülfit", List.of("şarap", "sarap", "kuru meyve", "sirke"))
    );

    public static final Set<String> DAIRY_AND_EGG_EXCLUSIONS = Set.of(
            "peynir", "kasar", "kaşar", "tereyağ", "tereyag", "yoğurt", "yogurt",
            "kaymak", "krema", "süt", "sut", "yumurta", "omlet", "menemen",
            "çikolata", "cikolata", "patisserie", "pastry", "pasta", "waffle"
    );

    // Güvenli kılan niteleyiciler (False-positive önleyiciler)
    public static final Set<String> PLANT_BASED_MODIFIERS = Set.of(
            "vegan", "bitkisel", "vejetaryen", "vejeteryan", "falafel", "tofu", "soya"
    );
}
