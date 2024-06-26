package com.ghostchu.peerbanhelper.config;

import com.ghostchu.peerbanhelper.Main;
import com.ghostchu.peerbanhelper.text.Lang;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.bspfsystems.yamlconfiguration.configuration.InvalidConfigurationException;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Slf4j
public class ProfileUpdateScript {
    private final YamlConfiguration conf;

    public ProfileUpdateScript(YamlConfiguration conf) {
        this.conf = conf;
    }

    @UpdateScript(version = 8)
    public void bigUpdate() {
        conf.set("ignore-peers-from-addresses", List.of(
                "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "fc00::/7",
                "fd00::/8", "100.64.0.0/10", "169.254.0.0/16", "127.0.0.0/8", "fe80::/10"));
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.getOptions().setParseComments(true);
        try (var in = Main.class.getResourceAsStream("/profile.yml")) {
            if (in == null) {
                log.error("Failed to upgrade configuration, no resources");
                System.exit(1);
                return;
            }
            String str = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            configuration.loadFromString(str);
            conf.set("module.expression-engine", configuration.get("module.expression-engine"));
        } catch (IOException | InvalidConfigurationException e) {
            log.error("Failed to upgrade configuration", e);
            System.exit(1);
        }

    }

    @UpdateScript(version = 7)
    public void progressCheckerIPPrefixLength() {
        conf.set("module.progress-cheat-blocker.ipv4-prefix-length", 32);
        conf.set("module.progress-cheat-blocker.ipv6-prefix-length", 64);
    }

    @UpdateScript(version = 5)
    public void subModule() {
        conf.set("module.ip-address-blocker-rules.enabled", false);
        conf.set("module.ip-address-blocker-rules.check-interval", 86400000);
        conf.set("module.ip-address-blocker-rules.rules.example-rule.enabled", false);
        conf.set("module.ip-address-blocker-rules.rules.example-rule.name", "Example");
        conf.set("module.ip-address-blocker-rules.rules.example-rule.url", "https://example.com/example.txt");
    }


    @UpdateScript(version = 4)
    public void ipDatabase() {
        conf.set("module.ip-address-blocker.asns", new ArrayList<>());
        conf.set("module.ip-address-blocker.regions", new ArrayList<>());
    }

    @UpdateScript(version = 3)
    public void multiDialingBlocker() {
        conf.set("module.multi-dialing-blocker.enabled", false);
        conf.set("module.multi-dialing-blocker.subnet-mask-length", 24);
        conf.set("module.multi-dialing-blocker.subnet-mask-v6-length", 64);
        conf.set("module.multi-dialing-blocker.tolerate-num", 3);
        conf.set("module.multi-dialing-blocker.cache-lifespan", 86400);
        conf.set("module.multi-dialing-blocker.keep-hunting", true);
        conf.set("module.multi-dialing-blocker.keep-hunting-time", 2592000);
    }

    @UpdateScript(version = 2)
    public void newRuleSyntax() {
        List<String> peerId = conf.getStringList("module.peer-id-blacklist.exclude-peer-id");
        List<String> clientName = conf.getStringList("module.client-name-blacklist.exclude-client-name");
        peerId = convertRuleStringExclude(peerId);
        clientName = convertRuleStringExclude(clientName);
        peerId.addAll(convertRuleString(conf.getStringList("module.peer-id-blacklist.banned-peer-id")));
        clientName.addAll(convertRuleString(conf.getStringList("module.client-name-blacklist.banned-client-name")));

        conf.set("module.peer-id-blacklist.banned-peer-id", peerId);
        conf.set("module.client-name-blacklist.banned-client-name", clientName);
        conf.set("module.peer-id-blacklist.exclude-peer-id", null);
        conf.set("module.client-name-blacklist.exclude-client-name", null);
        conf.set("module.active-probing-removed", conf.get("module.active-probing"));
        conf.set("module.active-probing", null);
    }

    @UpdateScript(version = 1)
    public void addExcludeLists() {
        conf.set("module.peer-id-blacklist.exclude-peer-id", Collections.emptyList());
        conf.set("module.client-name-blacklist.exclude-client-name", Collections.emptyList());
    }

    private List<String> convertRuleStringExclude(List<String> oldRules) {
        List<String> newRules = new ArrayList<>();
        for (String oldRule : oldRules) {
            oldRule = oldRule.toLowerCase(Locale.ROOT);
            String[] ruleExploded = oldRule.split("@", 2);
            if (ruleExploded.length != 2) {
                log.warn(Lang.ERR_INVALID_RULE_SYNTAX, oldRule);
                continue;
            }
            String matchMethod = ruleExploded[0];
            String ruleBody = ruleExploded[1];
            JsonObject newRuleObj = switch (matchMethod) {
                case "contains" -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("method", "CONTAINS");
                    object.addProperty("content", ruleBody);
                    object.addProperty("hit", "FALSE");
                    yield object;
                }
                case "startswith" -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("method", "STARTS_WITH");
                    object.addProperty("content", ruleBody);
                    object.addProperty("hit", "FALSE");
                    yield object;
                }
                case "endswith" -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("method", "ENDS_WITH");
                    object.addProperty("content", ruleBody);
                    object.addProperty("hit", "FALSE");
                    yield object;
                }
                case "length" -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("method", "LENGTH");
                    object.addProperty("min", Integer.parseInt(ruleBody));
                    object.addProperty("max", Integer.parseInt(ruleBody));
                    object.addProperty("hit", "FALSE");
                    yield object;
                }
                case "equals" -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("method", "EQUALS");
                    object.addProperty("content", ruleBody);
                    object.addProperty("success", "FALSE");
                    yield object;
                }
                case "regex" -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("method", "REGEX");
                    object.addProperty("content", ruleBody);
                    object.addProperty("success", "NEGATIVE");
                    yield object;
                }
                default -> null;
            };
            if (newRuleObj != null) {
                newRules.add(newRuleObj.toString());
            }
        }
        return newRules;
    }

    private List<String> convertRuleString(List<String> oldRules) {
        List<String> newRules = new ArrayList<>();
        for (String oldRule : oldRules) {
            oldRule = oldRule.toLowerCase(Locale.ROOT);
            String[] ruleExploded = oldRule.split("@", 2);
            if (ruleExploded.length != 2) {
                log.warn(Lang.ERR_INVALID_RULE_SYNTAX, oldRule);
                continue;
            }
            String matchMethod = ruleExploded[0];
            String ruleBody = ruleExploded[1];
            JsonObject newRuleObj = switch (matchMethod) {
                case "contains" -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("method", "CONTAINS");
                    object.addProperty("content", ruleBody);
                    yield object;
                }
                case "startswith" -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("method", "STARTS_WITH");
                    object.addProperty("content", ruleBody);
                    yield object;
                }
                case "endswith" -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("method", "ENDS_WITH");
                    object.addProperty("content", ruleBody);
                    yield object;
                }
                case "length" -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("method", "LENGTH");
                    object.addProperty("min", Integer.parseInt(ruleBody));
                    object.addProperty("max", Integer.parseInt(ruleBody));
                    yield object;
                }
                case "equals" -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("method", "EQUALS");
                    object.addProperty("content", ruleBody);
                    yield object;
                }
                case "regex" -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("method", "REGEX");
                    object.addProperty("content", ruleBody);
                    yield object;
                }
                default -> null;
            };
            if (newRuleObj != null) {
                newRules.add(newRuleObj.toString());
            }
        }
        return newRules;
    }
}
