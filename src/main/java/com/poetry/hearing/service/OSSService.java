package com.poetry.hearing.service;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.*;
import com.poetry.hearing.dao.UserMapper;
import com.poetry.hearing.domain.User;
import com.poetry.hearing.domain.UserExample;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.POIXMLTextExtractor;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Service
@CacheConfig(cacheNames = "oss")
public class OSSService {

    private static String endpoint = "http://oss-cn-shenzhen.aliyuncs.com";
    private static String accessKeyId = "LTAI9QE0MJC1iNvd";
    private static String accessKeySecret = "fF0YZSwxkECZzVpipyfDndOCJz8Ljz";
    private static String bucketName = "hearing-poetry";
    public static String localPath = "/home/hearing/webSources/";

    @Autowired
    private UserMapper userMapper;

    public String download(String key) throws Throwable {

        File dest = new File(localPath + key);
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }

        OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
        DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
        // Sets the local file to download to
        downloadFileRequest.setDownloadFile(localPath + key);
        // Sets the concurrent task thread count 5. By default it's 1.
        downloadFileRequest.setTaskNum(5);
        // Sets the part size, by default it's 100K.
        downloadFileRequest.setPartSize(1024 * 1024);
        // Enable checkpoint. By default it's false.
        downloadFileRequest.setEnableCheckpoint(true);

        ossClient.downloadFile(downloadFileRequest);
        String filePath = localPath + key;

        ossClient.shutdown();
        return filePath;
    }

    private void createBucket(OSSClient ossClient, String name){
        CreateBucketRequest createBucketRequest= new CreateBucketRequest(name);
        createBucketRequest.setCannedACL(CannedAccessControlList.PublicReadWrite);
        createBucketRequest.setStorageClass(StorageClass.IA);
        ossClient.createBucket(createBucketRequest);
    }

    private String[] readWord(String filePath){
        String text = "";
        File file = new File(filePath);
        //2003
        if(file.getName().endsWith(".doc")){
            try {
                FileInputStream stream = new FileInputStream(file);
                WordExtractor word = new WordExtractor(stream);
                text = word.getText();
                //去掉word文档中的多个换行
//                text = text.replaceAll("(\\r\\n){2,}", "\r\n");
//                text = text.replaceAll("(\\n){2,}", "\n");
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }else if(file.getName().endsWith(".docx")){       //2007
            try {
                OPCPackage oPCPackage = POIXMLDocument.openPackage(filePath);
                XWPFDocument xwpf = new XWPFDocument(oPCPackage);
                POIXMLTextExtractor ex = new XWPFWordExtractor(xwpf);
                text = ex.getText();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return text.split("\n");
    }

    public String copyFile(MultipartFile file, String filePath) throws IOException {
        if (file.isEmpty()){
            return "myself";
        }
        String originalFilename = file.getOriginalFilename();

        String fileName = UUID.randomUUID() + "-" + originalFilename;

        File dest = new File(filePath + fileName);

        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        file.transferTo(dest);
        return filePath + fileName;
    }

    @Cacheable(key = "#p0")
    public String[] readFromOSS(String key) throws IOException {
        OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
        // 下载object到文件
        File file = new File(localPath + key);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        ossClient.getObject(new GetObjectRequest(bucketName, key), new File(localPath + key));

        if (key.endsWith(".doc") || key.endsWith(".docx")) {
            return readWord(localPath + key);
        } else {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(new File(localPath + key)));
            int s;
            StringBuilder result = new StringBuilder();
            while ((s = reader.read()) != -1) {
                if (s == 10) {
                    result.append("\n");
                } else {
                    result.append((char) s);
                }
            }
            reader.close();
            return result.toString().split("\n");
        }
    }

    @CacheEvict(allEntries = true)
//    @CachePut(key = "#p1.substring(0, #p1.indexOf('/'))")
    public void upload(String localFile, String key, String email) throws Throwable {
        OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);

        if (!ossClient.doesBucketExist(bucketName)){
            //如果Bucket不存在则创建
            createBucket(ossClient, bucketName);
        }

        ObjectMetadata meta = new ObjectMetadata();
        meta.addUserMetadata("email", email);

        // 设置断点续传请求
        UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
        // 指定上传的本地文件
        uploadFileRequest.setUploadFile(localFile);
        // 指定上传并发线程数
        uploadFileRequest.setTaskNum(5);
        // 指定上传的分片大小
        uploadFileRequest.setPartSize(1024 * 1024);
        // 开启断点续传
        uploadFileRequest.setEnableCheckpoint(true);
        uploadFileRequest.setObjectMetadata(meta);
        // 断点续传上传
        ossClient.uploadFile(uploadFileRequest);
        // 关闭client
        ossClient.shutdown();
    }

    @Cacheable(key = "#p0+#p2")
    public List<Map<String, String>> getObjectsOrdered(String category, String userEmail, String flag) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(category+"/");
        if (!userEmail.isEmpty()) {
            stringBuilder.append(userEmail+"/");
        }
        if (!flag.isEmpty()) {
            stringBuilder.append(flag+"/");
        }
        String prefix = stringBuilder.toString();
        System.out.println(prefix);
        OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
        final int maxKeys = 500;
        ObjectListing objectListing = ossClient.listObjects(new ListObjectsRequest(bucketName).withPrefix(prefix).withMaxKeys(maxKeys));
        List<OSSObjectSummary> sums = objectListing.getObjectSummaries();

        sums.sort(new Comparator<OSSObjectSummary>() {
            @Override
            public int compare(OSSObjectSummary o1, OSSObjectSummary o2) {
                return o1.getLastModified().compareTo(o2.getLastModified());
            }
        });

        List<Map<String, String>> objectInfoMaps = new ArrayList<>();
        for (OSSObjectSummary s : sums) {
            if (s.getKey().endsWith("/")) {
                continue;
            }
            ObjectMetadata metadata = ossClient.getObjectMetadata(bucketName, s.getKey());
            Map<String, String> map = new HashMap<>();
            map.put("key", s.getKey());

            ObjectListing articleBg = ossClient.listObjects(new ListObjectsRequest(bucketName).withPrefix("articleBg/Bg" +
                    s.getKey().substring(s.getKey().lastIndexOf("/")+1, s.getKey().lastIndexOf("."))));
            List<OSSObjectSummary> articleBgSum = articleBg.getObjectSummaries();
            if (articleBgSum.size() == 1) {
                map.put("bgKey", getUrlFromKey(articleBgSum.get(0).getKey(), new Date(new Date().getTime() + 3600 * 1000)));
            }
            if (prefix.startsWith("picture")) {
                map.put("bgKey",  getUrlFromKey(s.getKey(), new Date(new Date().getTime() + 3600 * 1000)));
            }
            UserExample userExample = new UserExample();
            UserExample.Criteria criteria = userExample.createCriteria();
            String email = metadata.getUserMetadata().get("email");
            if (email == null) {
                map.put("author", "hearing");
                map.put("autograph", "命运的败笔追随岁月老去，深邃的世界埋葬着痛苦，昼夜交替之间，他的生死循环。");
            } else {
                criteria.andEmailEqualTo(email);
                List<User> users = userMapper.selectByExample(userExample);
                if (users.size() >= 1) {
                    User user = users.get(0);
                    map.put("author", user.getName());
                    map.put("autograph", user.getAutograph());
                }
            }
            objectInfoMaps.add(map);
        }

        ossClient.shutdown();
        return objectInfoMaps;
    }

    private String getUrlFromKey(String key, Date date) {
        OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
        return String.valueOf(ossClient.generatePresignedUrl(bucketName, key, date));
    }

    public void delObjByKey(String key) {
        OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
        ossClient.deleteObject(bucketName, key);
        ossClient.shutdown();
    }

    @CacheEvict(allEntries = true)
//    @CachePut(key = "#p0.substring(0,#p0.indexOf('/'))+'collect'")
    public void copyObj(String src, String dest) {
        OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
        ossClient.copyObject(bucketName, src, bucketName, dest);
        ossClient.shutdown();
    }
}
