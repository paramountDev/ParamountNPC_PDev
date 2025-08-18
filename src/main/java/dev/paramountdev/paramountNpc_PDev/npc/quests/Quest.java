package dev.paramountdev.paramountNpc_PDev.npc.quests;

import org.bukkit.entity.Player;

import java.util.List;

public class Quest {
    private final String id, name, description, npcId;
    private final List<Requirement> requirements;
    private final List<Reward> rewards;
    private final List<String> dependencies;
    private final int cooldown;

    public Quest(String id, String name, String description, String npcId,
                 List<Requirement> requirements, List<Reward> rewards,
                 List<String> dependencies, int cooldown) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.npcId = npcId;
        this.requirements = requirements;
        this.rewards = rewards;
        this.dependencies = dependencies;
        this.cooldown = cooldown;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getNpcId() { return npcId; }
    public List<Requirement> getRequirements() { return requirements; }
    public List<Reward> getRewards() { return rewards; }
    public List<String> getDependencies() { return dependencies; }
    public int getCooldown() { return cooldown; }

    public boolean canBeCompletedBy(Player player) {
        return requirements.stream().allMatch(req -> req.isMetBy(player));
    }

    public void complete(Player player) {
        requirements.forEach(req -> req.consumeFrom(player));
        rewards.forEach(reward -> reward.giveTo(player));
    }
}
