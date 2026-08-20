package dao;

import adt.CollectionInterface;
import adt.DoublyLinkedList;
import entity.Reward;
import java.util.Iterator;

public class RewardDAO {
    // Reward IDs come from a static counter, so the seeding order fixes them
    private static final CollectionInterface<Reward> REWARDS = seed();

    private static CollectionInterface<Reward> seed() {
        CollectionInterface<Reward> rewards = new DoublyLinkedList<>();
        rewards.add(new Reward("RM20 Dining Voucher", "RM20 voucher for selected resort dining outlets.", 500));
        rewards.add(new Reward("RM50 Dining Voucher", "RM50 voucher for selected resort dining outlets.", 1000));
        rewards.add(new Reward("RM100 Dining Voucher", "RM100 voucher for selected resort dining outlets.", 1800));
        rewards.add(new Reward("Complimentary Breakfast", "Complimentary breakfast for one guest.", 750));
        rewards.add(new Reward("Afternoon Tea for Two", "Afternoon tea experience for two guests.", 900));
        rewards.add(new Reward("Welcome Drink", "Complimentary welcome drink during your stay.", 300));
        rewards.add(new Reward("RM30 Spa Voucher", "RM30 voucher for selected spa treatments.", 800));
        rewards.add(new Reward("RM60 Spa Voucher", "RM60 voucher for selected spa treatments.", 1400));
        rewards.add(new Reward("Late Checkout", "Late checkout subject to availability.", 600));
        rewards.add(new Reward("Room Upgrade", "Complimentary room upgrade subject to availability.", 1500));
        rewards.add(new Reward("Deluxe Room Night", "One complimentary night in a Deluxe Room.", 2500));
        rewards.add(new Reward("RM100 Resort Credit", "RM100 credit usable at selected resort facilities.", 1800));
        rewards.add(new Reward("Airport Transfer", "One complimentary airport transfer.", 1200));
        rewards.add(new Reward("Poolside Cabana", "One complimentary poolside cabana session.", 1000));
        rewards.add(new Reward("Dinner for Two", "Complimentary dinner for two guests.", 1600));
        rewards.add(new Reward("RM50 Resort Credit", "RM50 credit usable at selected resort facilities.", 1000));
        rewards.add(new Reward("Complimentary Dessert", "Complimentary dessert at selected dining outlets.", 400));
        rewards.add(new Reward("Movie Night Package", "Movie night package for two guests.", 700));
        rewards.add(new Reward("Family Activity Pass", "Activity pass for selected resort family activities.", 1100));
        rewards.add(new Reward("Premium Room Night", "One complimentary night in a Premium Room.", 3500));
        return rewards;
    }

    public CollectionInterface<Reward> getRewards() {
        return REWARDS;
    }

    public Reward findByID(int ID) {
        String rewardID = String.format("RW%05d", ID);
        Iterator<Reward> walker = REWARDS.getIterator();
        while (walker.hasNext()) {
            Reward reward = walker.next();
            if (reward.getRewardID().equals(rewardID)) {
                return reward;
            }
        }
        return null;
    }
}
