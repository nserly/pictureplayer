package top.nserly.PicturePlayer.Version;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;
import top.nserly.SoftwareCollections_API.String.StringFormation;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Getter
@Setter
public class VersionID {
    @Expose(serialize = false, deserialize = false) // 既不能序列化，也不能反序列化
    public static final Gson gson = new GsonBuilder()
            .serializeNulls()
            .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY) // 字段名直接匹配 JSON key
            .enableComplexMapKeySerialization()
            .create();

    //用于启动文件
    private String StartMainFile;

    //最新外部版本（稳定）
    private String NormalVersion;

    //最新内部版本（稳定）
    private String NormalVersionID;

    //最新稳定版本描述文件网址
    private String NormalVersionDescribe;

    //最新稳定版本主文件网址（xxx.jar）
    private String NormalVersionMainFile;

    //最新稳定版依赖（key:依赖名 ; value:依赖下载地址）
    private TreeMap<String, String> NormalDependencies;


    //最新外部版本（测试）
    private String TestVersion;

    //最新内部版本（测试）
    private String TestVersionID;

    //最新测试版本描述文件网址
    private String TestVersionDescribe;

    //最新稳定版本主文件网址（xxx.jar）
    private String TestVersionMainFile;

    //最新测试版依赖（key:依赖名 ; value:依赖下载地址）
    private TreeMap<String, String> TestDependencies;

    //特殊字段（使用{xxx}标注的）（key:特殊字段（不包括"{}"的，如特殊字段{demo}，key一定要时demo ; value:对应值（里面不能含有特殊字段）））
    private HashMap<String, String> SpecialFields;

    //还原含有特殊字段的字符串原本的字符串
    public static String getString(String str, Map<String, String> SpecialFields) {
        StringFormation formation = new StringFormation(str);
        formation.removeAndAdd(SpecialFields);
        return formation.getProcessingString();
    }
}
