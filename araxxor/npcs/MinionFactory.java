package com.rs.game.player.content.araxxor.npcs;

import com.rs.game.WorldTile;
import com.rs.utils.Utils;

import java.util.Objects;

/**
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)
 */
final class MinionFactory {

    private AraxyteMinion.MinionType lastSpawnedMinionType;

    MinionFactory() {
        final int l = AraxyteMinion.MinionType.values().length;
        lastSpawnedMinionType = AraxyteMinion.MinionType.values()[Utils.random(l)];
    }

    AraxyteMinion.MinionType getLastSpawnedMinionType() {
        return lastSpawnedMinionType;
    }

    AraxyteMinion spawnMinion(int npcId, WorldTile startTile, Araxyte parent) {

        AraxyteMinion minion = null;

        if(npcId == AraxyteMinion.MinionType.BLADED.npcId) {
            minion = new BladedMinion(npcId, startTile, parent);
            lastSpawnedMinionType = AraxyteMinion.MinionType.BLADED;
        }
        else if(npcId == AraxyteMinion.MinionType.IMBUED.npcId) {
            minion = new ImbuedMinion(npcId, startTile, parent);
            lastSpawnedMinionType = AraxyteMinion.MinionType.IMBUED;
        }
        else if(npcId == AraxyteMinion.MinionType.MIRRORBACK.npcId) {
            minion = new MirrorbackMinion(npcId, startTile, parent);
            lastSpawnedMinionType = AraxyteMinion.MinionType.MIRRORBACK;
        }
        else if(npcId == AraxyteMinion.MinionType.PULSING.npcId) {
            minion = new PulsingMinion(npcId, startTile, parent);
            lastSpawnedMinionType = AraxyteMinion.MinionType.PULSING;
        }
        else if(npcId == AraxyteMinion.MinionType.SPITTING.npcId) {
            minion = new SpittingMinion(npcId, startTile, parent);
            lastSpawnedMinionType = AraxyteMinion.MinionType.SPITTING;
        }

        Objects.requireNonNull(minion);
        return minion;
    }

}
