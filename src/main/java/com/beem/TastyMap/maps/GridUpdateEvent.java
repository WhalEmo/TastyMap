package com.beem.TastyMap.maps;

import com.beem.TastyMap.maps.geo.GridCell;

public record GridUpdateEvent(Long gridId, GridCell cell) {
}
