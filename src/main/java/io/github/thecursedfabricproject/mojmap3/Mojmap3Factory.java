package io.github.thecursedfabricproject.mojmap3;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.cadixdev.bombe.type.FieldType;
import org.cadixdev.bombe.type.ObjectType;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.io.proguard.ProGuardReader;
import org.cadixdev.lorenz.model.MethodMapping;
import org.cadixdev.lorenz.model.MethodParameterMapping;
import org.cadixdev.lorenz.model.TopLevelClassMapping;

import net.fabricmc.lorenztiny.TinyMappingsReader;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;

public class Mojmap3Factory {
    // https://launchermeta.mojang.com/mc/game/version_manifest_v2.json
    private static URL ojmap;
    private static URL yarn;

    static {
        try {
            ojmap = new URL("https://launcher.mojang.com/v1/objects/be3ba1ca24543ecd73f240c36a3aa61916fa4d0c/client.txt");
            yarn = new URL("https://maven.fabricmc.net/net/fabricmc/yarn/21w13a%2Bbuild.9/yarn-21w13a%2Bbuild.9-mergedv2.jar");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } 
    }

    public static void main(String[] args) {
        MappingSet obf2ojmap = null;
        MappingSet obf2yarn = null;
        MappingSet obf2intermediary = null;
        try {
            InputStream ojmapStream = DownloadUtil.get(ojmap, "ojmap");
            try (ProGuardReader reader = new ProGuardReader(new InputStreamReader(ojmapStream))) {
                obf2ojmap = reader.read().reverse();
            }

            ZipInputStream yarnZipStream = new ZipInputStream(DownloadUtil.get(yarn, "yarn"));
            ZipEntry entry = null;
            while ((entry = yarnZipStream.getNextEntry()) != null) {
                if (entry.getName().endsWith(".tiny")) {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(yarnZipStream));
                    TinyTree tinyTree = TinyMappingFactory.load(bufferedReader);
                    try (TinyMappingsReader reader = new TinyMappingsReader(tinyTree, "official", "named")) {
                        obf2yarn = reader.read();
                    }
                    try (TinyMappingsReader reader = new TinyMappingsReader(tinyTree, "official", "intermediary")) {
                        obf2intermediary = reader.read();
                    }
                    break;
                }
            }
            yarnZipStream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Objects.requireNonNull(obf2ojmap);
        Objects.requireNonNull(obf2yarn);
        Objects.requireNonNull(obf2intermediary);
        MappingSet ojmapWithParam = obf2ojmap.copy();
        for (TopLevelClassMapping topLevelClassMapping : obf2yarn.getTopLevelClassMappings()) {
            for (MethodMapping methodMapping : topLevelClassMapping.getMethodMappings()) {
                Object[] parameterMappings = methodMapping.getParameterMappings().toArray();
                for (int i = 0; i < parameterMappings.length; i++) {
                    MethodParameterMapping methodParameterMapping = (MethodParameterMapping) parameterMappings[i];
                    ojmapWithParam.getTopLevelClassMapping(topLevelClassMapping.getFullObfuscatedName()).orElseThrow()
                        .getMethodMapping(methodMapping.getObfuscatedName(), methodMapping.getObfuscatedDescriptor()).orElseThrow(
                                () -> {
                                    System.out.println(methodMapping.getObfuscatedName());
                                    return new RuntimeException("missing method: " + methodMapping.getObfuscatedName());
                                }
                            )
                            .getOrCreateParameterMapping(i).setDeobfuscatedName(remapParam(methodParameterMapping, i, obf2ojmap, obf2yarn));
                }
            }
        }
        System.out.println(obf2ojmap);
        System.out.println(obf2yarn);
        System.out.println(obf2intermediary);
    }

    private static String className(ObjectType objectType) {
        String a = objectType.getClassName();
        return a.substring(Math.max(a.lastIndexOf('/') + 1, 0), a.length());
    }

    private static String copyCaps(String caps, String from, String to) {
        if (Character.isUpperCase(caps.charAt(0))) {
            return to;
        } else if (caps.equals(from.toLowerCase())) {
            return to.toLowerCase();
        } else if (to.toUpperCase().equals(to)) {
            return to.toUpperCase();
        } else {
            return to.substring(0, 1).toLowerCase() + to.substring(1);
        }
    }

    private static String remapParam(MethodParameterMapping parameterMappings, int theActualRealIndex, MappingSet to, MappingSet from) {
        FieldType paramReturnType = parameterMappings.getParent().getSignature().getDescriptor().getParamTypes().get(theActualRealIndex);
        if (!(paramReturnType instanceof ObjectType)) return parameterMappings.getDeobfuscatedName();
        ObjectType oReturn = (ObjectType)paramReturnType;
        String toClass = className((ObjectType)to.deobfuscate(oReturn));
        String fromClass = className((ObjectType)from.deobfuscate(oReturn));
        if (parameterMappings.getDeobfuscatedName().toLowerCase().contains(fromClass.toLowerCase())) {
            String result = Pattern.compile("(?i)" + fromClass).matcher(parameterMappings.getDeobfuscatedName()).replaceAll(r -> {
                String a = r.group();
                return copyCaps(a, fromClass, toClass);
            });
            System.out.println(parameterMappings.getDeobfuscatedName() + " " + result + " " + toClass + " " + fromClass);
            return result;
        }
        return parameterMappings.getDeobfuscatedName();
    }
}