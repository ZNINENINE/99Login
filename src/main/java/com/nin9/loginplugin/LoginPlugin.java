// Arquivos do plugin: coloque cada arquivo no caminho indicado em um projeto Maven/Gradle.

// ===== File: src/main/java/com/nin9/loginplugin/LoginPlugin.java =====
package com.nin9.loginplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class LoginPlugin extends JavaPlugin {
    private static LoginPlugin instance;
    private AuthManager authManager;
    private LoginListener loginListener;
    private Commands commands;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        authManager = new AuthManager(this);
        commands = new Commands(this, authManager);
        loginListener = new LoginListener(this, authManager);

        // Registrar listeners
        getServer().getPluginManager().registerEvents(loginListener, this);

        // Registrar comandos
        getCommand("registrar").setExecutor(commands);
        getCommand("login").setExecutor(commands);
        getCommand("alterarsenha").setExecutor(commands);
        getCommand("deletarsenha").setExecutor(commands);

        getLogger().info("99Login habilitado.");
    }

    @Override
    public void onDisable() {
        getLogger().info("99Login desabilitado.");
    }

    public static LoginPlugin getInstance() {
        return instance;
    }
}


// ===== File: src/main/java/com/nin9/loginplugin/AuthManager.java =====
package com.nin9.loginplugin;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;
import java.util.Properties;

public class AuthManager {
    private final JavaPlugin plugin;
    private final File playersDir;
    private final SecureRandom random = new SecureRandom();

    public AuthManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playersDir = new File(plugin.getDataFolder(), "players");
        if (!playersDir.exists()) playersDir.mkdirs();
    }

    private File getPlayerFile(UUID uuid) {
        return new File(playersDir, uuid.toString() + ".properties");
    }

    public boolean isRegistered(UUID uuid) {
        return getPlayerFile(uuid).exists();
    }

    public synchronized void register(Player player, String senha) throws Exception {
        UUID uuid = player.getUniqueId();
        File f = getPlayerFile(uuid);
        if (f.exists()) throw new IllegalStateException("Já registrado");

        byte[] salt = new byte[16];
        random.nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hash = hashPassword(salt, senha);

        Properties props = new Properties();
        props.setProperty("name", player.getName());
        props.setProperty("salt", saltB64);
        props.setProperty("hash", hash);

        try (FileOutputStream out = new FileOutputStream(f)) {
            props.store(out, "99Login player data");
        }
    }

    public synchronized boolean verify(UUID uuid, String senha) {
        try {
            File f = getPlayerFile(uuid);
            if (!f.exists()) return false;
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(f)) { props.load(in); }
            String saltB64 = props.getProperty("salt");
            String storedHash = props.getProperty("hash");
            byte[] salt = Base64.getDecoder().decode(saltB64);
            String hash = hashPassword(salt, senha);
            return storedHash != null && storedHash.equals(hash);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro verificando senha", e);
            return false;
        }
    }

    public synchronized void changePassword(UUID uuid, String novaSenha) throws Exception {
        File f = getPlayerFile(uuid);
        if (!f.exists()) throw new IllegalStateException("Não registrado");
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(f)) { props.load(in); }

        byte[] salt = new byte[16];
        random.nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hash = hashPassword(salt, novaSenha);

        props.setProperty("salt", saltB64);
        props.setProperty("hash", hash);
        try (FileOutputStream out = new FileOutputStream(f)) { props.store(out, "99Login player data"); }
    }

    public synchronized void deleteAccount(UUID uuid) throws Exception {
        File f = getPlayerFile(uuid);
        if (f.exists() && !f.delete()) {
            throw new IllegalStateException("Não foi possível deletar o arquivo do jogador");
        }
    }

    private String hashPassword(byte[] salt, String senha) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        md.update(senha.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();
        return Base64.getEncoder().encodeToString(digest);
    }
}


// ===== File: src/main/resources/plugin.yml =====
name: 99Login
main: com.nin9.loginplugin.LoginPlugin
version: 1.0
api-version: 1.21
commands:
  registrar:
    description: Registra uma senha para sua conta
    usage: /registrar <senha>
  login:
    description: Faz login com sua senha
    usage: /login <senha>
  alterarsenha:
    description: Altera sua senha
    usage: /alterarsenha <senha_atual> <senha_nova>
  deletarsenha:
    description: Deleta sua conta do sistema
    usage: /deletarsenha <senha>
