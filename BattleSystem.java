package backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.List;


public class BattleSystem {
    private Party playerParty;
    private Party enemyParty;
    private boolean isBattleOngoing;
    private Random random;

    private ArrayList<String> battleLog;
    private int currentPlayerTurnIndex;

    public BattleSystem(Party playerParty, Party enemyParty) {
        this.playerParty = playerParty;
        this.enemyParty = enemyParty;
        this.isBattleOngoing = true;
        this.random = new Random();
        this.battleLog = new ArrayList<>();
        this.currentPlayerTurnIndex = 0;
    }

    // --- METODE UNTUK GUI ---
    public ArrayList<String> getAndClearBattleLog() {
        if(this.battleLog.isEmpty()) return new ArrayList<>();
        ArrayList<String> log = new ArrayList<>(this.battleLog);
        this.battleLog.clear();
        return log;
    }
    private void log(String message) {
        if (message == null || message.trim().isEmpty() || message.startsWith("---") || message.startsWith("===")) {
            return;
        }
        System.out.println("BATTLE LOG: " + message);
        this.battleLog.add(message);
    }
    private void log(ArrayList<String> messages) {
        for (String msg : messages) {
            log(msg);
        }
    }
    public Party getPlayerParty() { return this.playerParty; }
    public Party getEnemyParty() { return this.enemyParty; }
    public boolean isBattleOngoing() { return this.isBattleOngoing; }

    public ArrayList<Character> getAliveAllies() {
        ArrayList<Character> aliveAllies = new ArrayList<>();
        for (Character ally : playerParty.getMembers()) if (!ally.isDead()) aliveAllies.add(ally);
        return aliveAllies;
    }
    public ArrayList<Character> getDeadAllies() {
        ArrayList<Character> deadAllies = new ArrayList<>();
        for (Character ally : playerParty.getMembers()) if (ally.isDead()) deadAllies.add(ally);
        return deadAllies;
    }
    public ArrayList<Character> getAliveEnemies() {
        ArrayList<Character> aliveEnemies = new ArrayList<>();
        for (Character enemy : enemyParty.getMembers()) if (!enemy.isDead()) aliveEnemies.add(enemy);
        return aliveEnemies;
    }

    public Character getActivePlayer() {
        ArrayList<Character> aliveAllies = getAliveAllies();
        if (aliveAllies.isEmpty()) return null;

        while (currentPlayerTurnIndex < playerParty.getMembers().size()) {
            Character activePlayer = playerParty.getMembers().get(currentPlayerTurnIndex);

            if (activePlayer.isDead()) {
                currentPlayerTurnIndex++;
                continue;
            }

            return activePlayer;
        }
        return null;
    }

    // --- LOGIKA ALUR PERTARUNGAN ---

    public boolean startBattle() {
        log("Pertarungan dimulai!");
        Character firstPlayer = getActivePlayer();
        if (firstPlayer != null) {
            log("--- Giliran " + firstPlayer.getName() + " ---");
        }
        return true;
    }

    public void incrementPlayerTurnIndex() {
        this.currentPlayerTurnIndex++;
    }

    public void resetPlayerTurnIndex() {
        this.currentPlayerTurnIndex = 0;
    }


    public boolean handlePlayerAction(int actionChoice, int targetChoice, int subChoice) {
        Player player = (Player) getActivePlayer();
        if (player == null) return true;

        log(player.handleStatusEffects());
        player.resetStatus();

        if (player.isDead()) { log(player.getName() + " telah gugur!"); return true; }
        if (player.isStunned) { log(player.getName() + " masih terkena Stun!"); return true; }

        ArrayList<Character> aliveEnemies = getAliveEnemies();
        ArrayList<Character> aliveAllies = getAliveAllies();
        ArrayList<Character> deadAllies = getDeadAllies();

        Character targetEnemy = null;
        if (targetChoice >= 0 && targetChoice < aliveEnemies.size()) {
            targetEnemy = aliveEnemies.get(targetChoice);
        }

        Character targetAlly = null;
        if (targetChoice >= 0 && targetChoice < aliveAllies.size()) {
            targetAlly = aliveAllies.get(targetChoice);
        }

        switch (actionChoice) {
            case 1: // Menyerang
                if (targetEnemy != null) {
                    log(player.attack(targetEnemy));
                } else { log("Target tidak valid!"); return false; }
                break;
            case 2: // Bertahan
                log(player.defend());
                break;

            case 3: // Gunakan Skill
                Skill skill = player.getSkills().get(subChoice);
                if (!player.hasEnoughManaPoint(skill.getMpCost())) {
                    log(player.getName() + " tidak punya cukup MP!");
                    return false;
                }
                player.useManaPoint(skill.getMpCost());
                log(player.getName() + " menggunakan " + skill.getName() + "!");

                // [PERBAIKAN] Logika validasi target
                boolean requiresEnemyTarget = (skill.getType() == Skill.SkillType.ATTACK || skill.getType() == Skill.SkillType.DEBUFF);
                boolean requiresAllyTarget = (skill.getType() == Skill.SkillType.HEAL ||
                        (skill.getType() == Skill.SkillType.BUFF &&
                                // Cek apakah ini skill self-cast/AoE
                                !skill.getName().equals("Defensive Stance") &&
                                !skill.getName().equals("Iron Fortress") &&
                                !skill.getName().equals("Eagle Eye") &&
                                !skill.getName().equals("Fade") &&
                                !skill.getName().equals("Meditate") &&
                                !skill.getName().equals("Hero’s Will") &&
                                !skill.getName().equals("Magic Barrier") &&
                                !skill.getName().equals("Taunting Roar")
                        ));

                if (requiresEnemyTarget && targetEnemy == null) {
                    log("Target musuh tidak valid!"); return false;
                }
                if (requiresAllyTarget && targetAlly == null) {
                    log("Target rekan tidak valid!"); return false;
                }

                // Logika Skill (Berdasarkan NAMA)
                switch (skill.getName()) {

                    case "Ultimate Slash":
                        log(targetEnemy.takeDamage(skill.getPower(), 0));
                        break;
                    case "Brave Slash":
                        int actualPower = skill.getPower();
                        if (player.getHealth() < (player.getMaxHealth() / 2.0)) {
                            actualPower = (int)(actualPower * 1.2);
                            log(player.getName() + " merasakan kekuatan tambahan!");
                        }
                        log(targetEnemy.takeDamage(actualPower, 0));
                        break;
                    case "Shield Bash":
                        log(targetEnemy.takeDamage(skill.getPower(), 0));
                        if (random.nextInt(100) < 20) {
                            log(targetEnemy.applyStun(1));
                        }
                        break;
                    case "Piercing Arrow":
                        log(targetEnemy.takeDamage(skill.getPower(), 20));
                        break;
                    case "Shadow Strike":
                        log(targetEnemy.takeDamage(skill.getPower(), 0));
                        break;
                    case "Poison Blade":
                        log(targetEnemy.takeDamage(skill.getPower(), 0));
                        log(targetEnemy.applyPoison(5, 3));
                        break;
                    case "Fireball":
                        log(targetEnemy.takeDamage(skill.getPower(), 0));
                        if (random.nextInt(100) < 10) {
                            log(targetEnemy.applyBurn(10, 2));
                        }
                        break;

                    case "Rain of Arrows":
                        log("Serangan mengenai semua musuh!");
                        for (Character enemy : aliveEnemies) {
                            log(enemy.takeDamage(skill.getPower(), 0));
                        }
                        break;

                    case "Light Heal":
                    case "Heal":
                    case "Ultra Heal":
                        log(targetAlly.heal(skill.getPower()));
                        break;

                    case "Defensive Stance":
                        log(player.applyBuff(0, 15, 2));
                        break;
                    case "Iron Fortress":
                        log(player.applyBuff(0, 30, 2));
                        break;
                    case "Eagle Eye":
                        log(player.applyCritBuff(30, 2));
                        break;
                    case "Fade":
                        log(player.applyDodge());
                        break;
                    case "Meditate":
                        log(player.restoreManaPoint(25));
                        break;

                    case "Hero’s Will":
                        log("Semua rekan merasakan kekuatan bertambah!");
                        for (Character ally : aliveAllies) {
                            log(ally.applyBuff(15, 0, 3));
                        }
                        break;
                    case "Magic Barrier":
                        log("Semua rekan dilindungi pelindung sihir!");
                        for (Character ally : aliveAllies) {
                            log(ally.applyBuff(0, 20, 2));
                        }
                        break;

                    case "Taunting Roar":
                        log(player.getName() + " memprovokasi semua musuh!");
                        for (Character enemy : aliveEnemies) {
                            log(enemy.applyTaunt(player, 2));
                        }
                        break;

                    default:
                        log("Skill " + skill.getName() + " belum memiliki efek unik!");
                        break;
                }
                break;

            case 4: // Gunakan Item
                Item item = playerParty.getInventory().getItems().get(subChoice);
                log(player.getName() + " menggunakan " + item.getName() + "!");

                switch (item.getType()) {
                    case ATTACK:
                    case DEBUFF:
                        if (targetEnemy != null) log(targetEnemy.takeDamage(item.getPower(), 0));
                        break;
                    case HEAL_HP:
                        if (targetAlly != null) log(targetAlly.heal(item.getPower()));
                        break;
                    case HEAL_MP:
                        if (targetAlly != null) log(targetAlly.restoreManaPoint(item.getPower()));
                        break;
                    case BUFF:
                        if (targetAlly != null) log(targetAlly.applyBuff(item.getPower(), item.getPower(), 3));
                        break;
                    case REVIVE:
                        Character targetDead = null;
                        if (targetChoice >= 0 && targetChoice < deadAllies.size()) {
                            targetDead = deadAllies.get(targetChoice);
                            log(targetDead.revive(item.getPower()));
                        } else {
                            log("Target tidak valid!");
                        }
                        break;
                    case MYSTERY:
                        log("Item misterius itu tidak melakukan apa-apa...");
                        break;
                }
                playerParty.getInventory().removeItem(item);
                break;
        }

        if (checkBattleEnd()) {
            this.isBattleOngoing = false;
            processBattleEnd();
        }

        return true;
    }

    /**
     * [PERBAIKAN BESAR]
     * Mengembalikan List berisi: [0] Attacker, [1] Target, [2] TipeAksi (String)
     */
    public List<Object> handleSingleEnemyTurn(Character enemy) {
        Character cinematicAttacker = null;
        Character cinematicTarget = null;
        String cinematicActionType = "NONE";

        log("--- Giliran " + enemy.getName() + " ---");

        if (enemy.isStunned) {
            log(enemy.getName() + " masih terkena Stun!");
            log(enemy.handleStatusEffects());
            return List.of(null, null, "NONE"); // Tidak ada aksi
        }
        log(enemy.handleStatusEffects());
        enemy.resetStatus();
        if (enemy.isDead()) return List.of(null, null, "NONE");

        ArrayList<Character> aliveAllies = getAliveAllies();
        if (aliveAllies.isEmpty()) return List.of(null, null, "NONE");

        boolean skillUsed = false;

        Character tauntTarget = null;
        if (enemy.isTaunted() && enemy.getTauntTarget() != null && !enemy.getTauntTarget().isDead()) {
            tauntTarget = enemy.getTauntTarget();
        }

        if (enemy instanceof Enemy) {
            Enemy enemyWithSkills = (Enemy) enemy;
            if (!enemyWithSkills.getSkills().isEmpty()) {
                Collections.shuffle(enemyWithSkills.getSkills());

                for (Skill skill : enemyWithSkills.getSkills()) {
                    if (enemy.hasEnoughManaPoint(skill.getMpCost())) {

                        switch (skill.getType()) {
                            case HEAL:
                                Character healTarget = findMostWoundedAlly(enemyParty);
                                if (healTarget != null && healTarget.getHealth() < healTarget.getMaxHealth()) {
                                    enemy.useManaPoint(skill.getMpCost());
                                    log(enemy.getName() + " menggunakan " + skill.getName() + " pada " + healTarget.getName() + "!");
                                    log(healTarget.heal(skill.getPower()));
                                    cinematicAttacker = enemy;
                                    cinematicTarget = healTarget;
                                    cinematicActionType = "HEAL"; // Tipe aksi
                                    skillUsed = true;
                                }
                                break;

                            case BUFF:
                                enemy.useManaPoint(skill.getMpCost());
                                log(enemy.getName() + " menggunakan " + skill.getName() + "!");
                                log(enemy.applyBuff(skill.getPower(), skill.getPower(), 3));
                                cinematicAttacker = enemy;
                                cinematicTarget = enemy; // Target diri sendiri
                                cinematicActionType = "BUFF"; // Tipe aksi
                                skillUsed = true;
                                break;

                            case ATTACK:
                            case DEBUFF:
                                Character playerTarget;
                                if (tauntTarget != null) {
                                    playerTarget = tauntTarget;
                                    log(enemy.getName() + " terprovokasi oleh " + playerTarget.getName() + "!");
                                } else {
                                    playerTarget = aliveAllies.get(random.nextInt(aliveAllies.size()));
                                }

                                enemy.useManaPoint(skill.getMpCost());
                                log(enemy.getName() + " menggunakan " + skill.getName() + " pada " + playerTarget.getName() + "!");
                                log(playerTarget.takeDamage(skill.getPower(), 0));
                                cinematicAttacker = enemy;
                                cinematicTarget = playerTarget;
                                cinematicActionType = "ATTACK"; // Tipe aksi
                                skillUsed = true;
                                break;
                        }
                    }
                    if (skillUsed) break;
                }
            }
        }

        if (!skillUsed) {
            Character targetPlayer;
            if (tauntTarget != null) {
                targetPlayer = tauntTarget;
                log(enemy.getName() + " terprovokasi dan menyerang " + targetPlayer.getName() + "!");
            } else {
                targetPlayer = aliveAllies.get(random.nextInt(aliveAllies.size()));
            }
            log(enemy.attack(targetPlayer));
            cinematicAttacker = enemy;
            cinematicTarget = targetPlayer;
            cinematicActionType = "ATTACK"; // Tipe aksi
        }

        if (checkBattleEnd()) {
            this.isBattleOngoing = false;
            processBattleEnd();
        }

        return List.of(cinematicAttacker, cinematicTarget, cinematicActionType);
    }

    private Character findMostWoundedAlly(Party party) {
        Character mostWounded = null;
        double lowestHpPercentage = 1.0;

        for (Character member : party.getMembers()) {
            if (!member.isDead()) {
                double hpPercentage = (double) member.getHealth() / member.getMaxHealth();
                if (hpPercentage < lowestHpPercentage) {
                    lowestHpPercentage = hpPercentage;
                    mostWounded = member;
                }
            }
        }
        if (mostWounded == null && !party.getMembers().isEmpty()) {
            mostWounded = party.getMembers().get(0);
        }
        return mostWounded;
    }

    private boolean checkBattleEnd() {
        if (enemyParty.isDefeated()) return true;
        if (playerParty.isDefeated()) return true;
        return false;
    }

    public boolean processBattleEnd() {
        log("================ PERTARUNGAN SELESAI ================");

        if (playerParty.isDefeated()) {
            log("Tim kamu telah dikalahkan...");
            log("GAME OVER");
            this.isBattleOngoing = false;
            return true;

        } else if (enemyParty.isDefeated()) {
            log("Kamu berhasil mengalahkan semua musuh!");
            int totalExp = 0;
            int totalCoins = 0;
            for (Character enemy : enemyParty.getMembers()) {
                totalExp += ((Enemy) enemy).getExpReward();
                totalCoins += ((Enemy) enemy).getCoinDrop();
            }

            log("Tim mendapatkan total " + totalExp + " EXP!");
            log("Tim mendapatkan total " + totalCoins + " Coin!");
            playerParty.addMoney(totalCoins);

            for (Character member : playerParty.getMembers()) {
                if (member instanceof Player && !member.isDead()) {
                    log(((Player) member).gainExperience(totalExp));
                }
            }
            this.isBattleOngoing = false;
            return false;
        }
        return false;
    }
}
