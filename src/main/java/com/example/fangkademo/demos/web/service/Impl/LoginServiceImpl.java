package com.example.fangkademo.demos.web.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.util.RandomUtil;
import com.example.fangkademo.demos.web.domain.dto.UserDTO;
import com.example.fangkademo.demos.web.domain.entity.Result;
import com.example.fangkademo.demos.web.domain.entity.User;
import com.example.fangkademo.demos.web.domain.dto.LoginFormDTO;
import com.example.fangkademo.demos.web.mapper.UserMapper;
import com.example.fangkademo.demos.web.service.LoginService;
import com.example.fangkademo.demos.web.utils.RegexUtils;
import com.example.fangkademo.demos.web.utils.SystemConstants;
import com.sun.xml.messaging.saaj.packaging.mime.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static com.example.fangkademo.demos.web.utils.RedisConstants.*;

@Service
public class LoginServiceImpl extends ServiceImpl<UserMapper, User> implements LoginService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone) {
        //TODO 对手机号获取验证码进行限流
        //判断是不是二级限制
        if (stringRedisTemplate.opsForSet().isMember(TWO_LEVERLIMIT_KEY + phone, "1") != null) {
            return Result.fail("发送过于频繁，请半小时后稍后再试");
        }
        //判断是不是一级限制
        if (stringRedisTemplate.opsForSet().isMember(ONE_LEVERLIMIT_KEY + phone, "1") != null) {
            return Result.fail("发送过于频繁，请五分钟后稍后再试");
        }
        //检查过去1分钟内发送验证码的次数
        Long countOneMin = stringRedisTemplate.opsForZSet().count(SENDCODE_SENDTIME_KEY + phone, System.currentTimeMillis() - 60 * 1000, System.currentTimeMillis());
        if (countOneMin != null && countOneMin > 0) return Result.fail("发送过于频繁，请稍后再试");

        //获取半小时内发送验证码的次数
        Long countHalfHour = stringRedisTemplate.opsForZSet().count(SENDCODE_SENDTIME_KEY + phone, System.currentTimeMillis() - 30 * 60 * 1000, System.currentTimeMillis());
        if (countHalfHour != null && countHalfHour >= 8) {
            stringRedisTemplate.opsForSet().add(TWO_LEVERLIMIT_KEY + phone, "1");
            stringRedisTemplate.expire(ONE_LEVERLIMIT_KEY + phone, 30, TimeUnit.MINUTES);
            return Result.fail("半小时内发送次数过多，接下来如需再发送请等待半小时后重试");
        }

        //获取五分钟内的请求次数，3次以后进行进入以及限制，
        Long countFiveMin = stringRedisTemplate.opsForZSet().count(SENDCODE_SENDTIME_KEY + phone, System.currentTimeMillis() - 5 * 60 * 1000, System.currentTimeMillis());
        if (countFiveMin != null && countFiveMin >= 3) {
            //如果大于3次，则进行一级限制
            stringRedisTemplate.opsForSet().add(ONE_LEVERLIMIT_KEY + phone, "1");
            stringRedisTemplate.expire(ONE_LEVERLIMIT_KEY + phone, 5, TimeUnit.MINUTES);
            return Result.fail("5分钟内已经发送了3次，接下来如需再发送请等待5分钟后重试");
        }

        //1. 校验手机号
        if (RegexUtils.isEmailInvalid(phone)) {
            //如果无效，则直接返回
            return Result.fail("手机号无效");
        }
        //2. 生成验证码
        String code = RandomUtil.randomString(4);
        //3. 保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MILLISECONDS);
        //4. 发送验证码
        //因为发送验证码是需要花钱的，所以打印生成的验证码
        System.out.println("验证码为：" + code);
        // 更新发送时间和次数
        stringRedisTemplate.opsForZSet().add(SENDCODE_SENDTIME_KEY + phone, System.currentTimeMillis() + "", System.currentTimeMillis());

        return Result.ok("验证码发送成功");

    }

    //登录注册
    @Override
    public Result login(LoginFormDTO loginForm) {
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();
        //检验手机号是否正确，不同的请求就应该再次去进行确认
        if (RegexUtils.isEmailInvalid(phone)) {
            //如果无效，则直接返回
            return Result.fail("邮箱格式不正确！！");
        }
        //从redis中读取验证码，并进行校验
        String Cachecode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        //1. 校验邮箱
        if (RegexUtils.isEmailInvalid(phone)) {
            return Result.fail("邮箱格式不正确！！");
        }
        //2. 不符合格式则报错
        if (Cachecode == null || !code.equals(Cachecode)) {
            return Result.fail("无效的验证码");
        }
        //如果上述都没有问题的话，就从数据库中查询该用户的信息

        //select * from tb_user where phone = ?
        User user = query().eq("phone", phone).one();

        //判断用户是否存在
        if (user == null) {
            user = createuser(phone);
        }
        //保存用户信息到Redis中
        String token = UUID.randomUUID().toString();

        //7.2 将UserDto对象转为HashMap存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        HashMap<String, String> userMap = new HashMap<>();
        userMap.put("id", String.valueOf(userDTO.getId()));


        //7.3 存储
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);

        //7.4 设置token有效期为30分钟
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        //7.5 登陆成功则删除验证码信息
        stringRedisTemplate.delete(LOGIN_CODE_KEY + phone);

        //8. 返回token
        return Result.ok(token);
    }

    private User createuser(String phone) {
        //创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        //保存用户 insert into tb_user(phone,nick_name) values(?,?)
        save(user);
        return user;
    }

    @Override
    public Result logout() {
        return Result.ok();
    }
}
