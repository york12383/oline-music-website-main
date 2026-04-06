package com.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.music.common.R;
import com.example.music.mapper.ConsumerMapper;
import com.example.music.mapper.FeedbackMapper;
import com.example.music.model.domain.Consumer;
import com.example.music.model.domain.Feedback;
import com.example.music.model.request.FeedbackRequest;
import com.example.music.service.FeedbackService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Collections;

@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Autowired
    private ConsumerMapper consumerMapper;

    private boolean isAdminLoggedIn(HttpSession session) {
        return session.getAttribute("adminId") != null || session.getAttribute("adminName") != null || session.getAttribute("name") != null;
    }

    @Override
    public R addFeedback(FeedbackRequest feedbackRequest, HttpSession session) {
        if (feedbackRequest == null) {
            return R.error("反馈内容不能为空");
        }
        if (StringUtils.isBlank(feedbackRequest.getTitle())) {
            return R.error("反馈标题不能为空");
        }
        if (StringUtils.isBlank(feedbackRequest.getContent())) {
            return R.error("反馈内容不能为空");
        }

        Feedback feedback = new Feedback();
        BeanUtils.copyProperties(feedbackRequest, feedback);
        feedback.setFeedbackType(StringUtils.defaultIfBlank(feedbackRequest.getFeedbackType(), "other"));
        feedback.setTitle(StringUtils.trim(feedbackRequest.getTitle()));
        feedback.setContent(StringUtils.trim(feedbackRequest.getContent()));
        feedback.setContact(StringUtils.trimToNull(feedbackRequest.getContact()));
        feedback.setPagePath(StringUtils.trimToNull(feedbackRequest.getPagePath()));
        feedback.setStatus(0);

        Integer userId = null;
        Object sessionUserId = session.getAttribute("userId");
        if (sessionUserId instanceof Integer) {
            userId = (Integer) sessionUserId;
        } else if (sessionUserId != null) {
            try {
                userId = Integer.parseInt(String.valueOf(sessionUserId));
            } catch (NumberFormatException ignored) {
                userId = null;
            }
        }

        if (userId != null) {
            Consumer consumer = consumerMapper.selectById(userId);
            if (consumer != null) {
                feedback.setUserId(consumer.getId());
                feedback.setUsername(consumer.getUsername());
            }
        }

        if (feedbackMapper.insert(feedback) > 0) {
            return R.success("反馈提交成功，我们会尽快查看");
        }
        return R.error("反馈提交失败");
    }

    @Override
    public R myFeedback(HttpSession session) {
        Object sessionUserId = session.getAttribute("userId");
        Integer userId = null;
        if (sessionUserId instanceof Integer) {
            userId = (Integer) sessionUserId;
        } else if (sessionUserId != null) {
            try {
                userId = Integer.parseInt(String.valueOf(sessionUserId));
            } catch (NumberFormatException ignored) {
                userId = null;
            }
        }

        if (userId == null) {
            return R.success("未登录，暂无反馈记录", Collections.emptyList());
        }

        QueryWrapper<Feedback> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByDesc("create_time");
        queryWrapper.last("limit 10");
        return R.success("查询成功", feedbackMapper.selectList(queryWrapper));
    }

    @Override
    public R allFeedback(HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return R.error("请先登录管理员账户");
        }

        QueryWrapper<Feedback> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("status").orderByDesc("create_time");
        return R.success("查询成功", feedbackMapper.selectList(queryWrapper));
    }

    @Override
    public R updateFeedbackStatus(Integer id, Integer status, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return R.error("请先登录管理员账户");
        }
        if (id == null) {
            return R.error("反馈记录不存在");
        }
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            return R.error("反馈记录不存在");
        }
        feedback.setStatus(status != null && status == 1 ? 1 : 0);
        if (feedbackMapper.updateById(feedback) > 0) {
            return R.success("反馈状态更新成功");
        }
        return R.error("反馈状态更新失败");
    }

    @Override
    public R deleteFeedback(Integer id, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return R.error("请先登录管理员账户");
        }
        if (id == null) {
            return R.error("反馈记录不存在");
        }
        if (feedbackMapper.deleteById(id) > 0) {
            return R.success("反馈删除成功");
        }
        return R.error("反馈删除失败");
    }
}
