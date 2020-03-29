//package com.runicrealms.plugin.command;
//
//import com.runicrealms.plugin.RunicBank;
//import com.runicrealms.plugin.util.FileUtil;
//import org.bukkit.ChatColor;
//import org.bukkit.command.Command;
//import org.bukkit.command.CommandExecutor;
//import org.bukkit.command.CommandSender;
//import org.bukkit.configuration.file.FileConfiguration;
//import org.bukkit.configuration.file.YamlConfiguration;
//import org.bukkit.entity.Player;
//
//import java.io.File;
//import java.io.IOException;
//
//public class BankCMD implements CommandExecutor {
//
//    public boolean onCommand(CommandSender sender, Command cmd, String lb, String[] args) {
//        if(sender instanceof Player) {
//            if (!sender.isOp()) return false;
//            if (!RunicBank.getBankManager().getStorages().containsKey(((Player) sender).getUniqueId())) {
//                retrieveDataFile(sender);
//            }
//            RunicBank.getBankManager().openBank(((Player) sender).getUniqueId());
//        } else {
//            sender.sendMessage(ChatColor.DARK_RED + ("You must be a player to use /bank!"));
//        }
//        return true;
//    }
//
//    private void retrieveDataFile(CommandSender sender) {
//        File playerFile = FileUtil.getPlayerFile(((Player) sender).getUniqueId());
//        FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(playerFile);
//        try {
//            fileConfig.save(playerFile);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }
//}
