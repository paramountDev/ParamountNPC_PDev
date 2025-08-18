package dev.paramountdev.paramountNpc_PDev.npc.quests;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class RewardRegistry {

    private static final Map<String, Function<Integer, Reward>> rewardFactories = new HashMap<>();

    static {
        rewardFactories.put("MONEY", amount -> player -> player.sendMessage("Вы получили $" + amount));
        rewardFactories.put("EXPERIENCE", amount -> player -> player.giveExp(amount));
    }

    public static Reward create(String type, int amount) {
        return rewardFactories.getOrDefault(type, a -> null).apply(amount);
    }
}

