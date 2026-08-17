package dao;

import entity.LoyaltyTier;
import entity.TierRequirement;

public class TierRequirementDAO {
    private static final TierRequirement[] TIER_REQUIREMENTS = {
        new TierRequirement(LoyaltyTier.GUEST, -1, -1, "None", 1),
        new TierRequirement(LoyaltyTier.SILVER, -1, 5000, "Redemption points x2", 2),
        new TierRequirement(LoyaltyTier.GOLD, 4000, 12000, "Redemption points x5, Accommodation fee -50%", 5),
        new TierRequirement(LoyaltyTier.PLATINUM, 10000, -1, "Redemption points x5, Everything half off", 5)
    };
    public TierRequirement[] getAllTierRequirements() { return TIER_REQUIREMENTS; }
    public TierRequirement findByTier(LoyaltyTier loyaltyTier) {
        for (TierRequirement tierRequirement : TIER_REQUIREMENTS) {
            if (tierRequirement.getLoyaltyTier() == loyaltyTier) {
                return tierRequirement;
            }
        }
        return null;
    }
}
