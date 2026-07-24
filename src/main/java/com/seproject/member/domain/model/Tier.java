package com.seproject.member.domain.model;

public enum Tier {
    BRONZE(0),
    SILVER(50),
    GOLD(200),
    PLATINUM(500),
    DIAMOND(1500);

    private final int minScore;

    Tier(int minScore) {
        this.minScore = minScore;
    }

    public static Tier of(long score) {
        Tier result = BRONZE;
        for (Tier tier : values()) {
            if (score >= tier.minScore) {
                result = tier;
            }
        }
        return result;
    }
}
