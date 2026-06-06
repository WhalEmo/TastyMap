package com.beem.TastyMap.search;

import com.beem.TastyMap.BaseApiResponse;
import com.beem.TastyMap.search.data.GlobalSearchResult;
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

    @GetMapping
    public BaseApiResponse<GlobalSearchResult> searchEverything(
            @RequestParam String searchText,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        return BaseApiResponse.success(
                searchService.searchEverything(
                        searchText,
                        page,
                        size
                ),
                "Search successfully."
        );
    }
}
