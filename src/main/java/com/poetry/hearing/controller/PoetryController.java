package com.poetry.hearing.controller;

import com.poetry.hearing.domain.User;
import com.poetry.hearing.security.MyAuthenticationProvider;
import com.poetry.hearing.security.MyUserDetails;
import com.poetry.hearing.service.OSSService;
import com.poetry.hearing.service.RedisService;
import com.poetry.hearing.service.UserService;
import com.poetry.hearing.util.Constant;
import com.poetry.hearing.util.Msg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.*;
import java.util.*;

@Controller
public class PoetryController {

    @Autowired
    private UserService userService;

    @Autowired
    private OSSService ossService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private MyAuthenticationProvider myAuthenticationProvider;

    @GetMapping("/hearing")
    public String hello(){
        return "index";
    }

    @GetMapping("/error")
    public String error(){
        return "error";
    }

    private HttpSession getSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            session = request.getSession(true);
        }
        return session;
    }

    @GetMapping("/articleSum")
    public String articleSum(Map<String, Object> map, @RequestParam("category") String category,
                             @RequestParam("page") Integer page) {
        Object o = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        MyUserDetails user;
        if (o.getClass().getName().contains("String")) {
            user = null;
        } else {
            user = (MyUserDetails) o;
        }

        List<Map<String, String>> objectInfoMaps;
        switch (category) {
            case Constant.MINE_ARTICLE:
                if (user != null) {
                    objectInfoMaps = ossService.getObjectsOrdered("ancientPoetry",  user.getEmail(), "upload");
                    objectInfoMaps.addAll(ossService.getObjectsOrdered("modernPoetry", user.getEmail(),"upload"));
                    objectInfoMaps.addAll(ossService.getObjectsOrdered("prose", user.getEmail(),"upload"));
                    objectInfoMaps.addAll(ossService.getObjectsOrdered("novel", user.getEmail(),  "upload"));
                } else {
                    return "registerOrLogin";
                }
                break;
            case Constant.COLLECT_ARTICLE:
                if (user != null) {
                    objectInfoMaps = ossService.getObjectsOrdered("ancientPoetry", user.getEmail(), "collect");
                    objectInfoMaps.addAll(ossService.getObjectsOrdered("modernPoetry" ,user.getEmail(),"collect"));
                    objectInfoMaps.addAll(ossService.getObjectsOrdered("prose",user.getEmail(), "collect"));
                    objectInfoMaps.addAll(ossService.getObjectsOrdered("novel", user.getEmail(), "collect"));
                } else {
                    return "registerOrLogin";
                }
                break;
            case Constant.MINE_PICTURE:
                if (user != null) {
                    objectInfoMaps = ossService.getObjectsOrdered("picture",user.getEmail(),"upload");
                } else {
                    return "registerOrLogin";
                }
                break;
            case Constant.COLLECT_PICTURE:
                if (user != null) {
                    objectInfoMaps = ossService.getObjectsOrdered("picture",user.getEmail(), "collect");
                } else {
                    return "registerOrLogin";
                }
                break;
            default:
                objectInfoMaps = ossService.getObjectsOrdered(category, "", "");
        }
        map.put("pageNow", page);
        int pageNum = objectInfoMaps.size()%Constant.numPerPage==0?
                objectInfoMaps.size()/Constant.numPerPage:objectInfoMaps.size()/Constant.numPerPage+1;
        map.put("pageNum", pageNum=pageNum==0?1:pageNum);
        map.put("recordNum", objectInfoMaps.size());
        List<Map<String, String>> showInfoMaps;
        if (page >= pageNum) {
            showInfoMaps=objectInfoMaps.subList((pageNum-1)*Constant.numPerPage, objectInfoMaps.size());
        }else {
            showInfoMaps=objectInfoMaps.subList((page-1)*Constant.numPerPage, page*Constant.numPerPage);
        }

        if (user != null) {
            if (category.equals("picture") || category.equals(Constant.MINE_PICTURE) || category.equals(Constant.COLLECT_PICTURE)) {
                List<Map<String, String>> pictureInfoMaps = ossService.getObjectsOrdered("picture",user.getEmail(),"collect");
                for (Map<String, String> showInfoMap : showInfoMaps) {
                    for (Map<String, String> pictureInfoMap : pictureInfoMaps) {
                        if (pictureInfoMap.get("key").endsWith(showInfoMap.get("key").substring(showInfoMap.get("key").lastIndexOf("/")+1))) {
                            showInfoMap.put("hasCollect", "true");
                            break;
                        }
                    }
                }
            }
        }
        map.put("contentInfo", showInfoMaps);
        map.put("category", category);
        return "articleSum";
    }

    @GetMapping("/myself")
    public String myself(Map<String, Object> map){
        MyUserDetails user = ((MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        if (user == null) {
            return "registerOrLogin";
        }
        List<Map<String, String>> head = ossService.getObjectsOrdered("head" ,user.getEmail(), "");

        map.put("name", user.getUsername());
        map.put("autograph", user.getAutograph());
        return "myself";
    }

    @GetMapping("/connect")
    public String connect(){
        return "connect";
    }

    @PostMapping("/article")
    public String article(Map<String, Object> map, @RequestParam("key") String key,
                          @RequestParam(value = "bgKey", required = false) String bgKey) throws IOException {
        map.put("hasCollect", false);
        Object o = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        MyUserDetails user;
        if (o.getClass().getName().contains("String")) {
            user = null;
        } else {
            user = (MyUserDetails) o;
        }
        if (user != null) {
            List<Map<String, String>> objectInfoMaps = ossService.getObjectsOrdered("ancientPoetry",user.getEmail(),"collect");
            objectInfoMaps.addAll(ossService.getObjectsOrdered("modernPoetry",  user.getEmail(),  "collect"));
            objectInfoMaps.addAll(ossService.getObjectsOrdered("prose", user.getEmail(),  "collect"));
            objectInfoMaps.addAll(ossService.getObjectsOrdered("novel",  user.getEmail(),  "collect"));
            for (Map<String, String> objectInfoMap:objectInfoMaps) {
                if (objectInfoMap.get("key").endsWith(key.substring(key.lastIndexOf("/")+1))) {
                    map.put("hasCollect", true);
                    break;
                }
            }
        }
        map.put("article", ossService.readFromOSS(key));
        map.put("key", key);
        map.put("bgKey", bgKey);
        return "article";
    }

    @GetMapping("/registerOrLogin")
    public String registerOrLogin() {
        return "registerOrLogin";
    }

    @PostMapping("/register")
    public String register(@Valid User user, BindingResult bindingResult){
        if (bindingResult.hasErrors()) {
            return "redirect:registerOrLogin";
        }
        Msg msg = userService.register(user);
        if ((boolean)msg.getExtend().get(Constant.LOGIN_REGISTER_STATUS)){
            return "registerOrLogin";
        } else {
            return "registerOrLogin";
        }
    }

    @PostMapping("/upload")
    public String uploadArticle(@RequestParam("file")MultipartFile file,
                                @RequestParam(value = "fileBg", required = false)MultipartFile fileBg,
                                @RequestParam("category") String category) throws Throwable {
        String source = ossService.copyFile(file, OSSService.localPath);
        String sourceBg = null;
        if (fileBg != null) {
            sourceBg = ossService.copyFile(fileBg, OSSService.localPath);
        }
        MyUserDetails user = ((MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        if (user == null) {
            return "registerOrLogin";
        }
        ossService.upload(source, category + "/" + user.getEmail() + "/upload/" +
                source.substring(source.lastIndexOf("/") + 1), user.getEmail());
        if (fileBg != null) {
            ossService.upload(sourceBg, "articleBg/Bg" + source.substring(source.lastIndexOf("/") + 1,
                    source.lastIndexOf(".")) + sourceBg.substring(sourceBg.lastIndexOf(".")), user.getEmail());
        }
        return "myself";
    }

    @PostMapping("/uploadHead")
    public String uploadHead(@RequestParam("file")MultipartFile file) throws Throwable {
        String source = ossService.copyFile(file, OSSService.localPath);
        MyUserDetails user = ((MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        if (user == null) {
            return "registerOrLogin";
        }
        List<Map<String, String>> head = ossService.getObjectsOrdered("head", user.getEmail(), "");
        if (head.size() > 0) {
            ossService.delObjByKey(head.get(0).get("key"));
        }
        ossService.upload(source, "head/" + user.getEmail() + "/" + source.substring(source.lastIndexOf("/") + 1), user.getEmail());
        return "redirect:myself";
    }

    @PostMapping("/updateUserInfo")
    @ResponseBody
    public String updateUserInfo(@RequestParam("name") String name, @RequestParam("autograph") String autograph) {
        MyUserDetails user = ((MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        if (user == null) {
            return "registerOrLogin";
        }
        Msg msg = userService.updateUserInfo(user.getEmail(), name, autograph);
        user = new MyUserDetails((User)msg.getExtend().get(Constant.MSG_LOGINING_USER));
        if (redisService.getCacheByKey(Constant.KEY_HAS_USER_CACHE) != null) {
            redisService.setUserCache(user);
        }
        SecurityContextHolder.getContext().setAuthentication(myAuthenticationProvider.
                authenticate(new UsernamePasswordAuthenticationToken(user, null)));
        return msg.getMsg();
    }

    @PostMapping("/collect")
    @ResponseBody
    public Msg collect(@RequestParam("category") String category, @RequestParam("key") String key) {
        Object o = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        MyUserDetails user;
        if (o.getClass().getName().contains("String")) {
            user = null;
        } else {
            user = (MyUserDetails) o;
        }
        if (user == null) {
            return Msg.fail().add("msg", "请在登录后再进行操作!").add("collected", false);
        }
        String dest = category + "/" + user.getEmail() + "/collect/" + key.substring(key.lastIndexOf("/") + 1);
        ossService.copyObj(key, dest);
        return Msg.success().add("msg", "收藏成功!").add("collected", true);
    }
}
