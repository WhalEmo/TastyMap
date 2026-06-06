package com.beem.TastyMap.Maps;

import com.beem.TastyMap.Maps.Geo.GridCell;

public record GridUpdateEvent(Long gridId, GridCell cell) {
}
