package Generate.VersionID;

import top.nserly.PicturePlayer.Version.PicturePlayerVersion;
import top.nserly.PicturePlayer.Version.VersionID;
import top.nserly.SoftwareCollections_API.FileHandle.FileContents;
import top.nserly.SoftwareCollections_API.FileHandle.JarFileRenamer;
import top.nserly.SoftwareCollections_API.FileHandle.JarVersionCleaner;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

public class Generator {
    public static void main(String[] args) throws IOException {
        VersionID versionID = new VersionID();
        versionID.setStartMainFile("{MDWebsite}PicturePlayerRunner.exe");

        versionID.setNormalVersion("{version}");
        versionID.setNormalVersionID("{versionID}");
        versionID.setNormalVersionDescribe("{MDWebsite}Describe/{versionID}.txt");
        versionID.setNormalVersionMainFile("{MDWebsite}/{version}.jar");

//        versionID.setTestVersion("{version}");
//        versionID.setTestVersionID("{versionID}");
//        versionID.setTestVersionDescribe("{MDWebsite}Describe/{versionID}.txt");
//        versionID.setTestVersionMainFile("{MDWebsite}/{version}.jar");


        HashMap<String, String> SpecialFields = new HashMap<>();
        SpecialFields.put("version", PicturePlayerVersion.getShorterVersion());
        SpecialFields.put("versionID", PicturePlayerVersion.getVersionID());
        SpecialFields.put("MDWebsite", "https://gitee.com/nserly-huaer/ImagePlayer/raw/master/artifacts/PicturePlayer_jar/");
        SpecialFields.put("LibWebsite", "https://gitee.com/nserly-huaer/ImagePlayer/raw/master/artifacts/PicturePlayer_jar/lib/");


        TreeMap<String, String> dependencies = getTreeMap();


        versionID.setNormalDependencies(dependencies);
//        versionID.setTestDependencies(dependencies);
        versionID.setSpecialFields(SpecialFields);
        String versionIDJson = VersionID.gson.toJson(versionID);
        System.out.println(versionIDJson);
        FileContents.write("artifacts/PicturePlayer_jar/VersionID.sum", versionIDJson);

        File file = new File("artifacts/PicturePlayer_jar/PicturePlayer.jar");
        if (file.exists()) {
            File renameToFile = new File("artifacts/PicturePlayer_jar/" + SpecialFields.get("version") + ".jar");
            if (renameToFile.exists()) {
                if (!renameToFile.delete()) {
                    throw new RuntimeException("Delete old File Error");
                }
            }
            if (!file.renameTo(renameToFile)) {
                throw new RuntimeException("Rename Main File Error");
            }
        }
    }

    private static TreeMap<String, String> getTreeMap() throws IOException {
        TreeMap<String, String> dependencies = new TreeMap<>();
        final File libFile = new File("artifacts/PicturePlayer_jar/lib/");
        JarVersionCleaner.cleanOldVersions(libFile.getPath(), false);

        HashSet<File> hashSet = JarFileRenamer.renameJarFile(libFile.getPath());
        for (File file : hashSet) {
            if (file.getName().endsWith(".jar") && file.isFile()) {
                String dependencyName = file.getName();
                if (dependencies.containsKey(dependencyName.substring(0, dependencyName.lastIndexOf("-")))) {
                    throw new RuntimeException("Dependency Name Conflict: " + dependencyName);
                }
                dependencies.put(dependencyName.substring(0, dependencyName.lastIndexOf("-")), "{LibWebsite}" + dependencyName);
            }
        }

        System.out.println("Dependency counts:" + dependencies.size());
        if (hashSet.size() != dependencies.size()) {
            throw new RuntimeException("Dependencies are incomplete!");
        }
        return dependencies;
    }
}
