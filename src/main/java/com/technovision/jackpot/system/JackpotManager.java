package com.technovision.jackpot.system;

import com.technovision.jackpot.gui.ConfirmGUI;
import com.technovision.jackpot.messages.MessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

import static com.technovision.jackpot.Jackpot.*;

public class JackpotManager implements CommandExecutor {

    public static LotteryBag<UUID> JACKPOT;
    public static long MONEY;
    public static long TOTAL_TICKETS;
    public static HashMap<String, Long> TICKETS;

    public JackpotManager() {
        MONEY = 0;
        TOTAL_TICKETS = 0;
        JACKPOT = new LotteryBag<UUID>();
        TICKETS = new HashMap<String, Long>();
        PLUGIN.getCommand("jackpot").setExecutor(this);
    }

    public void awardWinner() {
        if (!JACKPOT.isEmpty()) {
            long prize = (long) (MONEY - (MONEY * MessageHandler.getJackpotDouble("tax-percent")));
            String prizeString = FORMATTER.format(prize);
            OfflinePlayer player = Bukkit.getOfflinePlayer(JACKPOT.getRandom());
            List<String> msg = MessageHandler.parseResults(prizeString, player.getName());
            for (String line : msg) {
                Bukkit.broadcastMessage(line);
            }
            ECON.depositPlayer(player, prize);
        }
        MONEY = 0;
        TOTAL_TICKETS = 0;
        TICKETS.clear();
        JACKPOT = new LotteryBag<UUID>();
    }

    public static void enterJackpot(Player player, long amt, long total) {
        ECON.withdrawPlayer(player, total);
        JACKPOT.addEntry(player.getUniqueId(), amt);
        MONEY += total;
        TOTAL_TICKETS += amt;
        if (TICKETS.containsKey(player.getUniqueId().toString())) {
            long oldAmt = TICKETS.get(player.getUniqueId().toString());
            TICKETS.put(player.getUniqueId().toString(), oldAmt + amt);
        } else {
            TICKETS.put(player.getUniqueId().toString(), amt);
        }
        player.sendMessage(MessageHandler.parseBuyMessage("buy-ticket", amt));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            if (cmd.getName().equalsIgnoreCase("jackpot")) {
                Player player = (Player) sender;
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("bet") || args[0].equalsIgnoreCase("place")) {
                        long amt = 1;
                        long oldAmt = TICKETS.getOrDefault(player.getUniqueId().toString(), 0L);
                        long max = PLUGIN.getConfig().getLong("max");
                        if (args.length >= 2) {
                            try {
                                amt = Long.parseLong(args[1]);
                                if (amt < 1) { throw new NumberFormatException(); }
                            } catch (NumberFormatException e) {
                                player.sendMessage("�c�l(!) �c/jackpot buy <amount>");
                                return true;
                            }
                        }
                        if (max < oldAmt + amt) {
                            player.sendMessage("�c�l(!) �cYou can only buy " + max + " tickets.");
                            return true;
                        }
                        long total = MessageHandler.getJackpotValue("ticket-price") * amt;
                        if (ECON.getBalance(player) >= total) {
                            if (MessageHandler.getJackpotBoolean("confirm-gui")) {
                                player.openInventory(new ConfirmGUI(amt, total).getInventory());
                            } else {
                                enterJackpot(player, amt, total);
                            }
                        } else {
                            player.sendMessage(MessageHandler.parseBuyMessage("cannot-afford", amt));
                        }
                    } else if (args[0].equalsIgnoreCase("reload")) {
                        if (player.hasPermission("jackpot.reload") || player.isOp()) {
                            PLUGIN.reloadConfig();
                            PLUGIN.saveConfig();
                            Bukkit.getServer().getPluginManager().disablePlugin(PLUGIN);
                            Bukkit.getServer().getPluginManager().enablePlugin(PLUGIN);
                            player.sendMessage("�a�l(!) �aJackpot successfully reloaded the config.");
                        } else {
                            player.sendMessage("�c�l(!) �cYou do not have permission to use that command!");
                        }
                    } else {
                        player.sendMessage("�c�l(!) �c/jackpot buy <amount>");
                    }
                } else {
                    long amount = 0;
                    double percent = 0;
                    if (TICKETS.containsKey(player.getUniqueId().toString())) {
                        amount = TICKETS.get(player.getUniqueId().toString());
                        percent = ((double) amount / TOTAL_TICKETS) * 100;
                    }
                    String tax = String.valueOf((int) (MessageHandler.getJackpotDouble("tax-percent") * 100));
                    List<String> msg = MessageHandler.parseInfo(
                            FORMATTER.format(MONEY), tax, TOTAL_TICKETS, amount, (int) percent);
                    for (String line : msg) {
                        player.sendMessage(line);
                    }
                }
            }
        }
        return true;
    }
}
