package com.beem.TastyMap.maps.listeners.events;

import com.beem.TastyMap.maps.geo.GridCell;

public record GridUpdateEvent(Long gridId, GridCell cell) {
}
