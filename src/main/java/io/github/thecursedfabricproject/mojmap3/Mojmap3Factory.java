package io.github.thecursedfabricproject.mojmap3;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.io.proguard.ProGuardReader;

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
                obf2ojmap = reader.read();
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
        System.out.println(obf2ojmap);
        System.out.println(obf2yarn);
        System.out.println(obf2intermediary);
    }
}