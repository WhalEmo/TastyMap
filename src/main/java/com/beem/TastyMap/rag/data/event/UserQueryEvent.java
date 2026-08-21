package com.beem.TastyMap.rag.data.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQueryEvent {
    private Long userId;
    private String userQuery;
    private boolean ignoreAllergies;
    private Boolean isOpenNow;
}