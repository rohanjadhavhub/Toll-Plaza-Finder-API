package com.tollfinder.domain;

public record TollPlazaMatch(long id, String name, String state, GeoPoint location,
                             double distanceFromSourceMeters) {}
