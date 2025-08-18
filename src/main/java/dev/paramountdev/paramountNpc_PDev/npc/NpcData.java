package dev.paramountdev.paramountNpc_PDev.npc;

import com.bnstra.npclib.api.NPC;
import com.bnstra.npclib.api.skin.Skin;

import java.util.ArrayList;
import java.util.List;

public class NpcData {
    private final int id;
    private final NPC npc;
    private final String name;
    private Skin skin;
    private final String subName;
    private List<String> phrases = new ArrayList<>();
    private long lastTalkTime = 0;
    private int cooldown = 10;

    public NpcData(int id, NPC npc, String name, String subName) {
        this.id = id;
        this.npc = npc;
        this.name = name;
        this.subName = subName;
    }

    public int getId() { return id; }
    public NPC getNpc() { return npc; }
    public String getName() { return name; }
    public String getSubName() { return subName; }
    public Skin getSkin() { return skin; }
    public void setSkin(Skin skin) { this.skin = skin; }

    public List<String> getPhrases() { return phrases; }
    public void setPhrases(List<String> phrases) { this.phrases = phrases; }

    public long getLastTalkTime() { return lastTalkTime; }
    public void setLastTalkTime(long time) { this.lastTalkTime = time; }

    public int getCooldown() { return cooldown; }
    public void setCooldown(int cooldown) { this.cooldown = cooldown; }
}

