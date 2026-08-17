package dao;

import entity.Reward;

public class RewardDAO {
    private static final Reward[] REWARDS = {
            new Reward("RM20 Dining Voucher", "RM20 voucher for selected resort dining outlets.", 500),
            new Reward("RM50 Dining Voucher", "RM50 voucher for selected resort dining outlets.", 1000),
            new Reward("RM100 Dining Voucher", "RM100 voucher for selected resort dining outlets.", 1800),
            new Reward("Complimentary Breakfast", "Complimentary breakfast for one guest.", 750),
            new Reward("Afternoon Tea for Two", "Afternoon tea experience for two guests.", 900),
            new Reward("Welcome Drink", "Complimentary welcome drink during your stay.", 300),
            new Reward("RM30 Spa Voucher", "RM30 voucher for selected spa treatments.", 800),
            new Reward("RM60 Spa Voucher", "RM60 voucher for selected spa treatments.", 1400),
            new Reward("Late Checkout", "Late checkout subject to availability.", 600),
            new Reward("Room Upgrade", "Complimentary room upgrade subject to availability.", 1500),
            new Reward("Deluxe Room Night", "One complimentary night in a Deluxe Room.", 2500),
            new Reward("RM100 Resort Credit", "RM100 credit usable at selected resort facilities.", 1800),
            new Reward("Airport Transfer", "One complimentary airport transfer.", 1200),
            new Reward("Poolside Cabana", "One complimentary poolside cabana session.", 1000),
            new Reward("Dinner for Two", "Complimentary dinner for two guests.", 1600),
            new Reward("RM50 Resort Credit", "RM50 credit usable at selected resort facilities.", 1000),
            new Reward("Complimentary Dessert", "Complimentary dessert at selected dining outlets.", 400),
            new Reward("Movie Night Package", "Movie night package for two guests.", 700),
            new Reward("Family Activity Pass", "Activity pass for selected resort family activities.", 1100),
            new Reward("Premium Room Night", "One complimentary night in a Premium Room.", 3500)
    };
    public Reward[] getRewards() {
        return REWARDS;
    }
    public Reward findByID(int ID) {
        for (Reward reward : REWARDS) {
            if (reward.getRewardID().equals(String.format("RW%05d", ID))) {
                return reward;
            }
        }
        return null;
    }
}
