package com.beem.TastyMap.Search;

import com.beem.TastyMap.Search.Data.GlobalSearchResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
public class GlobalSearchController {

    private final GlobalSearchService searchService;

    public GlobalSearchController(GlobalSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping()
    public GlobalSearchResult searchEverything(
            @RequestParam String searchText
    ){
        return searchService.searchEverything(searchText);
    }
}
