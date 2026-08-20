package dao;

import adt.CollectionInterface;
import adt.DoublyLinkedList;
import entity.LoyaltyTier;
import entity.TierRequirement;
import java.util.Iterator;

public class TierRequirementDAO {
    private static final CollectionInterface<TierRequirement> TIER_REQUIREMENTS = seed();

    private static CollectionInterface<TierRequirement> seed() {
        CollectionInterface<TierRequirement> requirements = new DoublyLinkedList<>();
        requirements.add(new TierRequirement(LoyaltyTier.GUEST, -1, -1, "None", 1));
        requirements.add(new TierRequirement(LoyaltyTier.SILVER, -1, 5000, "Redemption points x2", 2));
        requirements.add(new TierRequirement(LoyaltyTier.GOLD, 4000, 12000, "Redemption points x5, Accommodation fee -50%", 5));
        requirements.add(new TierRequirement(LoyaltyTier.PLATINUM, 10000, -1, "Redemption points x5, Everything half off", 5));
        return requirements;
    }

    public CollectionInterface<TierRequirement> getAllTierRequirements() {
        return TIER_REQUIREMENTS;
    }

    public TierRequirement findByTier(LoyaltyTier loyaltyTier) {
        Iterator<TierRequirement> walker = TIER_REQUIREMENTS.getIterator();
        while (walker.hasNext()) {
            TierRequirement requirement = walker.next();
            if (requirement.getLoyaltyTier() == loyaltyTier) {
                return requirement;
            }
        }
        return null;
    }
}
