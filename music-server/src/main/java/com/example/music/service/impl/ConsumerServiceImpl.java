package com.example.music.service.impl;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.music.common.R;
import com.example.music.controller.FileUploadController;
import com.example.music.controller.UserSingerController;
import com.example.music.mapper.*;
import com.example.music.model.domain.*;
import com.example.music.model.request.ConsumerRequest;
import com.example.music.model.request.SingerRequest;
import com.example.music.service.ConsumerService;
import com.example.music.utils.DefaultAvatarUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.music.constant.Constants.SALT;

@Service
public class ConsumerServiceImpl extends ServiceImpl<ConsumerMapper, Consumer>
        implements ConsumerService {
    private static final int ACTIVE_USER = 0;
    private static final int DELETED_USER = 1;
    private static final String DELETED_USER_DISPLAY_NAME = "已注销用户";
    private static final String DEFAULT_DELETED_AVATAR = DefaultAvatarUtils.buildDefaultAvatarPath(1);

    @Autowired
    private ConsumerMapper consumerMapper;

    @Autowired
    private SingerMapper singerMapper;

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private CollectMapper collectMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Autowired
    private RankListMapper rankListMapper;

    @Autowired
    private RankSongMapper rankSongMapper;

    @Autowired
    private ConsumerRecommendSeedMapper consumerRecommendSeedMapper;

    @Autowired
    private UserSupportMapper userSupportMapper;

    private QueryWrapper<Consumer> buildActiveUserQuery() {
        return new QueryWrapper<Consumer>().eq("deleted", ACTIVE_USER);
    }

    private Consumer findActiveUserByUsername(String username) {
        if (StringUtils.isBlank(username)) {
            return null;
        }
        QueryWrapper<Consumer> queryWrapper = buildActiveUserQuery();
        queryWrapper.eq("username", username);
        return consumerMapper.selectOne(queryWrapper);
    }

    private boolean isDeletedUser(Consumer consumer) {
        return consumer != null && Objects.equals(consumer.getDeleted(), DELETED_USER);
    }

    private Consumer sanitizeDeletedUser(Consumer consumer) {
        Consumer sanitizedConsumer = new Consumer();
        if (consumer != null) {
            BeanUtils.copyProperties(consumer, sanitizedConsumer);
        }
        sanitizedConsumer.setUsername(DELETED_USER_DISPLAY_NAME);
        sanitizedConsumer.setAvator(DEFAULT_DELETED_AVATAR);
        sanitizedConsumer.setPhoneNum(null);
        sanitizedConsumer.setEmail(null);
        sanitizedConsumer.setBirth(null);
        sanitizedConsumer.setSex(null);
        sanitizedConsumer.setLocation(null);
        sanitizedConsumer.setIntroduction("该账号已注销");
        return sanitizedConsumer;
    }

    private Consumer normalizeConsumerAvatar(Consumer consumer) {
        if (consumer == null || isDeletedUser(consumer)) {
            return consumer;
        }
        consumer.setAvator(DefaultAvatarUtils.normalizeAvatar(consumer.getAvator(), consumer.getId(), consumer.getUsername()));
        return consumer;
    }

    private List<Consumer> normalizeConsumerAvatar(List<Consumer> consumers) {
        if (consumers == null || consumers.isEmpty()) {
            return consumers;
        }
        return consumers.stream().map(this::normalizeConsumerAvatar).collect(Collectors.toList());
    }

    private Integer resolveSessionUserId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object sessionUserId = session.getAttribute("userId");
        if (sessionUserId instanceof Integer) {
            return (Integer) sessionUserId;
        }
        if (sessionUserId != null) {
            try {
                return Integer.parseInt(String.valueOf(sessionUserId));
            } catch (NumberFormatException ignored) {
                session.removeAttribute("userId");
            }
        }
        return null;
    }

    private boolean hasAdminPermission(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object adminNameAttr = session.getAttribute("adminName");
        String username = adminNameAttr instanceof String ? (String) adminNameAttr : (String) session.getAttribute("name");
        if (StringUtils.isBlank(username)) {
            return false;
        }
        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", username);
        return adminMapper.selectCount(queryWrapper) > 0;
    }

    private void clearUserSession(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute("username");
        session.removeAttribute("userId");
    }

    private String buildDeletedUsername(Integer id) {
        return "deleted_user_" + id + "_" + System.currentTimeMillis();
    }

    @Override
    public R logout(HttpSession session){
        session.removeAttribute("username");
        session.removeAttribute("userId");
        return R.success("注销成功");
    }

    /**
     * 新增用户
     */
    //实现 addUser 方法，调用 ConsumerMapper 进行数据库操作
    @Override
    public R addUser(ConsumerRequest registryRequest) {
        if (registryRequest == null) {
            return R.warning("注册信息不能为空");
        }
        String username = StringUtils.trimToEmpty(registryRequest.getUsername());
        String password = StringUtils.trimToEmpty(registryRequest.getPassword());
        String email = StringUtils.trimToNull(registryRequest.getEmail());
        String phoneNum = StringUtils.trimToNull(registryRequest.getPhoneNum());

        if (StringUtils.isBlank(username)) {
            return R.warning("用户名不能为空");
        }
        if (StringUtils.isBlank(password)) {
            return R.warning("密码不能为空");
        }
        if (email != null && email.length() > 30) {
            return R.warning("邮箱长度不能超过30个字符");
        }
        if (phoneNum != null && phoneNum.length() > 15) {
            return R.warning("手机号长度不能超过15个字符");
        }

        registryRequest.setUsername(username);
        registryRequest.setPassword(password);
        registryRequest.setEmail(email);
        registryRequest.setPhoneNum(phoneNum);

        if (this.existUser(registryRequest.getUsername())) {
            return R.warning("用户名已注册");
        }
        Consumer consumer = new Consumer();
        BeanUtils.copyProperties(registryRequest, consumer);
        //MD5加密
        String secretPassword = DigestUtils.md5DigestAsHex((SALT + registryRequest.getPassword()).getBytes(StandardCharsets.UTF_8));
        consumer.setPassword(secretPassword);
        //都用用
        if (StringUtils.isBlank(consumer.getPhoneNum())) {
            consumer.setPhoneNum(null);
        }
        if (StringUtils.isBlank(consumer.getEmail())) {
            consumer.setEmail(null);
        }
        consumer.setAvator(DefaultAvatarUtils.resolvePreparedDefaultAvatar(null, consumer.getUsername()));
        consumer.setDeleted(ACTIVE_USER);
        try {
            if (StringUtils.isNotBlank(consumer.getEmail()) && findByEmail(consumer.getEmail()) != null) {
                return R.fatal("邮箱不允许重复");
            }
            if (consumerMapper.insert(consumer) > 0) {
                return R.success("注册成功");
            } else {
                return R.error("注册失败");
            }
        } catch (DuplicateKeyException e) {
            return R.fatal(e.getMessage());
        }
    }

    //修改用户信息
    @Override
    public R updateUserMsg(ConsumerRequest updateRequest) {
        Consumer consumer = new Consumer();
        BeanUtils.copyProperties(updateRequest, consumer);
        Consumer existingUser = consumerMapper.selectById(consumer.getId());
        if (existingUser == null || isDeletedUser(existingUser)) {
            return R.error("用户不存在或已注销");
        }
            // 开始事务处理
            try {
                // 更新用户头像
                if (consumerMapper.updateById(consumer) > 0) {
                    // 查询该用户关联的歌手ID
                    Consumer user = consumerMapper.selectById(consumer.getId());
                    // 如果用户是歌手，同时更新歌手的图片
                    if (user != null && user.getSingerId() != null) {
                        String normalizedAvatar = DefaultAvatarUtils.normalizeAvatar(user.getAvator(), user.getId(), user.getUsername());

                        Singer singer = new Singer();
                        singer.setId(user.getSingerId());
                        singer.setName(user.getUsername());
                        singer.setBirth(user.getBirth());
                        singer.setSex(user.getSex());
                        singer.setLocation(user.getLocation());
                        singer.setIntroduction(user.getIntroduction());
                        singer.setPic(normalizedAvatar);
                        singerMapper.updateById(singer);
                    }
                    return R.success("修改成功");
                } else {
                    return R.error("修改失败");
                }
            } catch (Exception e) {
                return R.error("更新失败: " + e.getMessage());
            }
    }

    //修改用户密码
    @Override
    public R updatePassword(ConsumerRequest updatePasswordRequest) {

       if (!this.verityPasswd(updatePasswordRequest.getUsername(),updatePasswordRequest.getOldPassword())) {
            return R.error("密码输入错误");
        }

        Consumer consumer = new Consumer();
        consumer.setId(updatePasswordRequest.getId());
        String secretPassword = DigestUtils.md5DigestAsHex((SALT + updatePasswordRequest.getPassword()).getBytes(StandardCharsets.UTF_8));
        consumer.setPassword(secretPassword);

        if (consumerMapper.updateById(consumer) > 0) {
            return R.success("密码修改成功");
        } else {
            return R.error("密码修改失败");
        }
    }

    /**
     * 缩减验证
     * @param updatePasswordRequest
     * @return
     */
    @Override
    public R updatePassword01(ConsumerRequest updatePasswordRequest) {
        Consumer consumer = new Consumer();
        consumer.setId(updatePasswordRequest.getId());
        String secretPassword = DigestUtils.md5DigestAsHex((SALT + updatePasswordRequest.getPassword()).getBytes(StandardCharsets.UTF_8));
        consumer.setPassword(secretPassword);

        if (consumerMapper.updateById(consumer) > 0) {
            return R.success("密码修改成功");
        } else {
            return R.error("密码修改失败");
        }
    }

    @Autowired  // 注入 FileUploadController
    private FileUploadController fileUploadController;

    //更新用户头像
    @Override
    public R updateUserAvator(MultipartFile avatorFile,int id,HttpSession session) {
        //文件上传当使用 el-upload 组件上传文件时，默认情况下不会自动携带 cookie 或 token 等认证信息。
        Consumer existingUser = consumerMapper.selectById(id);
        if (existingUser == null || isDeletedUser(existingUser)) {
            return R.error("用户不存在或已注销");
        }

        String s = fileUploadController.uploadAtorImgFile(avatorFile);
        String imgPath = "/img/avatorImages/" + s;
        Consumer consumer = new Consumer();
        consumer.setId(id);
        consumer.setAvator(imgPath);
        // 开始事务处理
        try {
            // 更新用户头像
            if (consumerMapper.updateById(consumer) > 0) {
                // 查询该用户关联的歌手ID
                Consumer user = consumerMapper.selectById(id);
                if (user != null && user.getSingerId() != null) {
                    // 如果用户是歌手，同时更新歌手的图片
                    Singer singer = new Singer();
                    singer.setId(user.getSingerId());
                    singer.setPic(imgPath);
                    singerMapper.updateById(singer);
                }
                return R.success("上传成功", imgPath);
            } else {
                return R.error("上传失败");
            }
        } catch (Exception e) {
            return R.error("更新失败: " + e.getMessage());
        }
    }

    @Override
    public boolean existUser(String username) {
        return findActiveUserByUsername(username) != null;
    }

    @Override
    public boolean verityPasswd(String username, String password) {
        Consumer consumer = findActiveUserByUsername(username);
        if (consumer == null) {
            return false;
        }
        String secretPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes(StandardCharsets.UTF_8));
        return Objects.equals(consumer.getPassword(), secretPassword);
    }

    // 注销用户，保留公开内容
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R deleteUser(Integer id, HttpSession session) {
        if (id == null || id <= 0) {
            return R.error("用户ID无效");
        }

        Consumer consumer = consumerMapper.selectById(id);
        if (consumer == null) {
            return R.error("用户不存在");
        }
        if (isDeletedUser(consumer)) {
            if (Objects.equals(resolveSessionUserId(session), id)) {
                clearUserSession(session);
            }
            return R.warning("账号已注销");
        }

        boolean isAdmin = hasAdminPermission(session);
        Integer currentUserId = resolveSessionUserId(session);
        if (!isAdmin && !Objects.equals(currentUserId, id)) {
            return R.error("权限不足");
        }

        try {
            userSupportMapper.delete(new QueryWrapper<UserSupport>().eq("user_id", id));
            collectMapper.delete(new QueryWrapper<Collect>().eq("user_id", id));
            feedbackMapper.delete(new QueryWrapper<Feedback>().eq("user_id", id));
            playHistoryMapper.delete(new QueryWrapper<PlayHistory>().eq("user_id", id));
            rankListMapper.delete(new QueryWrapper<RankList>().eq("consumer_id", id));
            rankSongMapper.delete(new QueryWrapper<RankSong>().eq("consumer_id", id));
            consumerRecommendSeedMapper.delete(new QueryWrapper<ConsumerRecommendSeed>().eq("user_id", id));

            UpdateWrapper<Consumer> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", id)
                    .set("username", buildDeletedUsername(id))
                    .set("password", DigestUtils.md5DigestAsHex((SALT + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8)))
                    .set("sex", null)
                    .set("phone_num", null)
                    .set("email", null)
                    .set("birth", null)
                    .set("introduction", "该账号已注销")
                    .set("location", null)
                    .set("avator", DEFAULT_DELETED_AVATAR)
                    .set("deleted", DELETED_USER)
                    .set("update_time", new Date());

            if (consumerMapper.update(null, updateWrapper) > 0) {
                if (Objects.equals(currentUserId, id)) {
                    clearUserSession(session);
                }
                return R.success("账号已注销，公开评论等内容已保留");
            }
            return R.error("注销失败");
        } catch (Exception e) {
            return R.error("注销失败: " + e.getMessage());
        }
    }


    //管理员的session正常使用
    @Override
    public R allUser(HttpSession session) {
        Object adminNameAttr = session.getAttribute("adminName");
        String username = adminNameAttr instanceof String ? (String) adminNameAttr : (String) session.getAttribute("name");
        if (StringUtils.isBlank(username)) {
            return R.error("请先登录管理员账户");
        }

        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", username);
        if (adminMapper.selectCount(queryWrapper) > 0) {
            return R.success(null, normalizeConsumerAvatar(consumerMapper.selectList(buildActiveUserQuery())));
        }

        return R.error("权限不足");
    }

    @Override
    public R userOfId(Integer id) {
        Consumer consumer = consumerMapper.selectById(id);
        if (consumer == null) {
            return R.success(null, Collections.emptyList());
        }
        if (isDeletedUser(consumer)) {
            return R.success(null, Collections.singletonList(sanitizeDeletedUser(consumer)));
        }
        return R.success(null, Collections.singletonList(normalizeConsumerAvatar(consumer)));
    }


    //用户登录
    @Override
    public R loginStatus(ConsumerRequest loginRequest, HttpSession session) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        Consumer consumer = findActiveUserByUsername(username);
        if (consumer != null && this.verityPasswd(username, password)) {
            session.setAttribute("username", username);
            session.setAttribute("userId", consumer.getId());
            return R.success("登录成功", Collections.singletonList(normalizeConsumerAvatar(consumer)));
        }
        return R.error("用户名或密码错误");
    }

    @Override
    public R loginEmailStatus(ConsumerRequest loginRequest, HttpSession session) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();
        Consumer consumer1 = findByEmail(email);
        if (consumer1 == null) {
            return R.error("邮箱不存在");
        }
        if (this.verityPasswd(consumer1.getUsername(), password)) {
            session.setAttribute("username", consumer1.getUsername());
            session.setAttribute("userId", consumer1.getId());
            return R.success("登录成功", Collections.singletonList(normalizeConsumerAvatar(consumer1)));
        } else {
            return R.error("用户名或密码错误");
        }
    }

    @Override
    public R getSessionUser(HttpSession session) {
        Integer userId = null;
        Object sessionUserId = session.getAttribute("userId");
        if (sessionUserId instanceof Integer) {
            userId = (Integer) sessionUserId;
        } else if (sessionUserId != null) {
            try {
                userId = Integer.parseInt(String.valueOf(sessionUserId));
            } catch (NumberFormatException ignored) {
                session.removeAttribute("userId");
            }
        }

        if (userId != null) {
            Consumer consumer = consumerMapper.selectById(userId);
            if (consumer != null && !isDeletedUser(consumer)) {
                session.setAttribute("username", consumer.getUsername());
                session.setAttribute("userId", consumer.getId());
                return R.success("登录状态有效", normalizeConsumerAvatar(consumer));
            }
            clearUserSession(session);
        }

        String username = (String) session.getAttribute("username");
        if (StringUtils.isNotBlank(username)) {
            Consumer consumer = findActiveUserByUsername(username);
            if (consumer != null) {
                session.setAttribute("userId", consumer.getId());
                return R.success("登录状态有效", normalizeConsumerAvatar(consumer));
            }
            session.removeAttribute("username");
        }

        return R.error("未登录");
    }

    @Override
    public Consumer findByEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return null;
        }
        QueryWrapper<Consumer> queryWrapper = buildActiveUserQuery();
        queryWrapper.eq("email", email);
        return consumerMapper.selectOne(queryWrapper);
    }

    // 获取用户创建的歌手的id
    @Override
    public R getUserSinger(Integer id) {
        // 首先从consumer表中获取用户的singer_id
        Consumer consumer = consumerMapper.selectById(id);
        //System.out.println("获取用户创建的歌手的id:  "+consumer.getSingerId());
        if (consumer == null || isDeletedUser(consumer) || consumer.getSingerId() == null) {
            return R.error("用户不存在");
        }
        // 返回歌手信息
        // 直接返回 singerId（如果没有，返回 null）
        Integer singerId = consumer.getSingerId();
        return R.success("查询成功", consumer.getSingerId());
    }

    //用户成为歌手，用户注册为歌手，用户歌手
    @Override
    public R becomeSinger(UserSingerController.SingerRequests singerRequests, HttpSession session) {
        Singer singer = new Singer();
        BeanUtils.copyProperties(singerRequests, singer);
        Consumer currentConsumer = consumerMapper.selectById(singerRequests.getUserId());
        singer.setPic(DefaultAvatarUtils.normalizeAvatar(
                currentConsumer == null ? null : currentConsumer.getAvator(),
                singerRequests.getUserId(),
                singerRequests.getName()
        ));
        if (singerMapper.insert(singer) > 0) {
            //在数据库创建歌手后，获取歌手的id，并更新用户的singer_id字段
            String username = (String) session.getAttribute("username");
            System.out.println("用户名: " + username);

            Consumer consumer = new Consumer();
            consumer.setId(singerRequests.getUserId());  // 设置用户ID
            consumer.setSingerId(singer.getId());         // 设置歌手ID
            //System.out.println("成为歌手更新数据库:  "+consumer);
            // 正确调用updateById方法
            if (consumerMapper.updateById(consumer) > 0) {
                //System.out.println(singerRequests.getUserId());
                return R.success("添加成功");
            } else {
                return R.error("用户信息更新失败");
            }
        } else {
            return R.error("添加失败");
        }
    }

    @Override
    public R becomeSinger1(SingerRequest singerRequests, HttpSession session){
        return null;
    }

    @Autowired
    private PlayHistoryMapper playHistoryMapper;
    // ConsumerServiceImpl.java


    // 添加歌曲播放记录
    @Override
    public R addPlayHistory(int songId, int userId, HttpSession session) {
        // 验证用户身份：检查session中的用户名是否对应传入的userId
        String sessionUsername = (String) session.getAttribute("username");
        if (sessionUsername == null) {
            return R.error("用户未登录");
        }
        // 根据用户名查找用户信息
        Consumer sessionConsumer = findActiveUserByUsername(sessionUsername);

        if (sessionConsumer == null) {
            return R.error("用户不存在");
        }
        // 验证传入的userId是否与session中的用户ID一致
        if (sessionConsumer.getId() != userId) {
            return R.error("权限不足，无法为其他用户添加播放记录");
        }
        try {
            PlayHistory playHistory = new PlayHistory();
            playHistory.setUserId(userId);
            playHistory.setSongId(songId);
            playHistory.setPlayTime(new Date());
            // 你可以根据需要设置 duration，默认为0
            playHistory.setDuration(30);

            // 插入播放记录到数据库
            playHistoryMapper.insert(playHistory);

            return R.success("播放记录添加成功");
        } catch (Exception e) {
            return R.error("播放记录添加失败: " + e.getMessage());
        }
    }

    @Autowired
    private SongMapper songMapper;

    // 获取最近20个播放记录
    @Override
    public R getRecentPlayHistory(int userId) {
        if (userId <= 0) {
            return R.error("用户ID无效");
        }

        try {
            // --- 1. 获取原始播放记录（查询足够多的记录进行去重）---
            // 为了确保能找到 20 个不重复的歌曲ID，我们先查询最近的 100 条记录
            QueryWrapper<PlayHistory> historyQueryWrapper = new QueryWrapper<>();
            historyQueryWrapper.eq("user_id", userId);
            historyQueryWrapper.orderByDesc("play_time");
            historyQueryWrapper.last("LIMIT 50");

            List<PlayHistory> rawHistoryList = playHistoryMapper.selectList(historyQueryWrapper);

            // --- 2. 内存中进行去重和排序，获取最近20个不重复的歌曲ID ---
            // 使用 LinkedHashMap：
            // 1. Map 的 Key 自动去重 (songId)
            // 2. LinkedHashMap 保持插入顺序
            // 由于 rawHistoryList 是按时间倒序的，先遇到的记录就是该 songId 的最新播放记录
            Map<Integer, PlayHistory> uniqueRecentPlays = new LinkedHashMap<>();

            for (PlayHistory history : rawHistoryList) {
                // putIfAbsent 保证只会存储该 songId 第一次出现（即最晚）的记录
                uniqueRecentPlays.putIfAbsent(history.getSongId(), history);
            }

            // 提取最终的歌曲ID列表（最多20个），顺序已经是按最近播放时间倒序
            List<Integer> finalSongIds = uniqueRecentPlays.keySet().stream()
                    .limit(20)
                    .collect(Collectors.toList());
            if (finalSongIds.isEmpty()) {
                return R.success("查询成功", new ArrayList<>());
            }
            // --- 3. 批量查询歌曲详细信息 ---
            QueryWrapper<Song> songQueryWrapper = new QueryWrapper<>();
            songQueryWrapper.in("id", finalSongIds);

            // 批量查询出来的歌曲，顺序是乱的
            List<Song> songs = songMapper.selectList(songQueryWrapper);


            return R.success("查询成功", songs);
        } catch (Exception e) {
            return R.error("查询失败: 系统错误");
        }
    }
}




