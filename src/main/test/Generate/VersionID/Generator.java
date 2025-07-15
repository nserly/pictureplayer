package Generate.VersionID;

import top.nserly.PicturePlayer.Version.VersionID;

import java.io.File;
import java.util.HashMap;
import java.util.TreeMap;

public class Generator {
    public static void main(String[] args) {
        VersionID versionID = new VersionID();
        versionID.setNormalVersion("{version}");
        versionID.setNormalVersionID("{versionID}");
        versionID.setNormalVersionDescribe("{MDWebsite}Describe/{versionID}.txt");
        versionID.setNormalVersionMainFile("{MDWebsite}/{version}.jar");

//        versionID.setTestVersion("{version}");
//        versionID.setTestVersionID("{versionID}");
//        versionID.setTestVersionDescribe("{MDWebsite}Describe/{versionID}.txt");
//        versionID.setTestVersionMainFile("{MDWebsite}/{version}.jar");


        HashMap<String, String> SpecialFields = new HashMap<>();
        SpecialFields.put("version", "V1.0.0beta15");
        SpecialFields.put("versionID", "1315");
        SpecialFields.put("MDWebsite", "https://gitee.com/nserly-huaer/ImagePlayer/raw/master/artifacts/PicturePlayer_jar/");
        SpecialFields.put("LibWebsite", "https://gitee.com/nserly-huaer/ImagePlayer/raw/master/artifacts/PicturePlayer_jar/lib/");


        TreeMap<String, String> dependencies = getTreeMap();


        versionID.setNormalDependencies(dependencies);
//        versionID.setTestDependencies(dependencies);
        versionID.setSpecialFields(SpecialFields);
        System.out.println(VersionID.gson.toJson(versionID));
    }

    private static TreeMap<String, String> getTreeMap() {
        TreeMap<String, String> dependencies = new TreeMap<>();
        File[] files = new File("artifacts/PicturePlayer_jar/lib/").listFiles();
        for (File file : files) {
            if (file.getName().endsWith(".jar") && file.isFile()) {
                String dependencyName = file.getName();
                dependencies.put(dependencyName.substring(0, dependencyName.lastIndexOf("-")), "{LibWebsite}" + dependencyName);
            }
        }

        return dependencies;
    }
}
