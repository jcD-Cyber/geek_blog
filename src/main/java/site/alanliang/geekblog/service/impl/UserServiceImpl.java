package site.alanliang.geekblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import site.alanliang.geekblog.common.TableConstant;
import site.alanliang.geekblog.dao.RoleUserMapper;
import site.alanliang.geekblog.dao.UserMapper;
import site.alanliang.geekblog.exception.BadRequestException;
import site.alanliang.geekblog.exception.EntityExistException;
import site.alanliang.geekblog.model.RoleUser;
import site.alanliang.geekblog.model.User;
import site.alanliang.geekblog.query.UserQuery;
import site.alanliang.geekblog.service.UserService;
import site.alanliang.geekblog.utils.StringUtils;
import site.alanliang.geekblog.vo.UserInfoVO;
import site.alanliang.geekblog.vo.UserLoginVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Descriptin TODO
 * @Author AlanLiang
 * Date 2020/4/6 20:52
 * Version 1.0
 **/
@Service
@CacheConfig(cacheNames = "user")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoleUserMapper roleUserMapper;

    @Override
    @Cacheable
    public User getById(Long id) {
        QueryWrapper<User> userWrapper = new QueryWrapper<>();
        userWrapper.select(User.Table.ID, User.Table.USERNAME, User.Table.NICKNAME, User.Table.SEX, User.Table.PHONE, User.Table.EMAIL, User.Table.STATUS)
                .eq(User.Table.ID, id);
        User user = userMapper.selectOne(userWrapper);
        QueryWrapper<RoleUser> roleUserWrapper = new QueryWrapper<>();
        roleUserWrapper.select(RoleUser.Table.ROLE_ID)
                .eq(RoleUser.Table.USER_ID, id);
        List<RoleUser> roleUsers = roleUserMapper.selectList(roleUserWrapper);
        if (!CollectionUtils.isEmpty(roleUsers)) {
            user.setRoleId(roleUsers.get(0).getRoleId());
        }
        return user;
    }

    @Override
    @CacheEvict(allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Long id) {
        userMapper.deleteById(id);
    }

    @Override
    @CacheEvict(allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void removeByIdList(List<Long> idList) {
        userMapper.deleteBatchIds(idList);
    }

    @Override
    @CacheEvict(allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(User user) {
        userMapper.updateById(user);
    }

    @Override
    @CacheEvict(allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void saveOfUpdate(User user) {
        if (user.getId() == null) {
            //保存
            //验证用户名是否唯一
            QueryWrapper<User> wrapper = new QueryWrapper<>();
            wrapper.eq(User.Table.USERNAME, user.getUsername());
            if (null != userMapper.selectOne(wrapper)) {
                throw new EntityExistException("用户", "用户名", user.getUsername());
            }
            //验证邮箱是否唯一
            wrapper.clear();
            wrapper.eq(User.Table.EMAIL, user.getEmail());
            if (null != userMapper.selectOne(wrapper)) {
                throw new EntityExistException("用户", "邮箱", user.getEmail());
            }
            //验证手机号码是否唯一
            wrapper.clear();
            wrapper.eq(User.Table.PHONE, user.getPhone());
            if (null != userMapper.selectOne(wrapper)) {
                throw new EntityExistException("用户", "手机号码", user.getPhone());
            }
            userMapper.insert(user);
            RoleUser roleUser = new RoleUser();
            roleUser.setRoleId(user.getRoleId());
            roleUser.setUserId(user.getId());
            roleUserMapper.insert(roleUser);
        } else {
            //更新
            //验证手机号码是否唯一
            QueryWrapper<User> userWrapper = new QueryWrapper<>();
            userWrapper.eq(User.Table.PHONE, user.getPhone());
            List<User> users = userMapper.selectList(userWrapper);
            users = users.stream().filter(u -> !u.getId().equals(user.getId())).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(users)) {
                throw new EntityExistException("用户", "手机号码", user.getPhone());
            }
            //验证邮箱是否唯一
            userWrapper.clear();
            userWrapper.eq(User.Table.EMAIL, user.getPhone());
            users = userMapper.selectList(userWrapper);
            users = users.stream().filter(u -> !u.getId().equals(user.getId())).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(users)) {
                throw new EntityExistException("用户", "邮箱", user.getEmail());
            }
            //首先更新用户
            userMapper.updateById(user);
            //再添加用户角色关联
            RoleUser roleUser = new RoleUser();
            roleUser.setUserId(user.getId());
            roleUser.setRoleId(user.getRoleId());
            QueryWrapper<RoleUser> roleUserWrapper = new QueryWrapper<>();
            roleUserWrapper.eq(RoleUser.Table.USER_ID, user.getId());
            if (!CollectionUtils.isEmpty(roleUserMapper.selectList(roleUserWrapper))) {
                //有记录则先删除
                roleUserMapper.delete(roleUserWrapper);
            }
            //最后添加
            roleUserMapper.insert(roleUser);
        }
    }

    @Override
    @CacheEvict(allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void updateInfo(UserInfoVO userInfoVO) {
        //验证手机号码是否唯一
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq(User.Table.PHONE, userInfoVO.getPhone());
        List<User> users = userMapper.selectList(wrapper);
        users = users.stream().filter(u -> !u.getId().equals(userInfoVO.getId())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(users)) {
            throw new EntityExistException("用户", "手机号码", userInfoVO.getPhone());
        }
        //验证邮箱是否唯一
        wrapper.clear();
        wrapper.eq(User.Table.EMAIL, userInfoVO.getPhone());
        users = userMapper.selectList(wrapper);
        users = users.stream().filter(u -> !u.getId().equals(userInfoVO.getId())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(users)) {
            throw new EntityExistException("用户", "邮箱", userInfoVO.getEmail());
        }
        User user = new User();
        BeanUtils.copyProperties(userInfoVO, user);
        userMapper.updateById(user);
    }

    @Override
    @Cacheable
    public Integer countAll() {
        return userMapper.selectCount(null);
    }

    @Override
    @CacheEvict(allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(UserLoginVO passwordVO) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.select(User.Table.PASSWORD).eq(User.Table.ID, passwordVO.getUserId());
        User user = userMapper.selectOne(wrapper);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(passwordVO.getOldPassword(), user.getPassword())) {
            throw new BadRequestException(HttpStatus.BAD_REQUEST, "旧密码输入不正确");
        }
        user.setId(passwordVO.getUserId());
        user.setPassword(encoder.encode(passwordVO.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    @Cacheable
    public Page<User> listTableByPage(int current, int size, UserQuery userQuery) {
        Page<User> page = new Page<>(current, size);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(userQuery.getUsername())) {
            wrapper.like(User.Table.USERNAME, userQuery.getUsername());
        }
        if (!StringUtils.isEmpty(userQuery.getEmail())) {
            wrapper.like(User.Table.EMAIL, userQuery.getEmail());
        }
        if (userQuery.getStartDate() != null && userQuery.getEndDate() != null) {
            wrapper.between(TableConstant.USER_ALIAS + User.Table.CREATE_TIME, userQuery.getStartDate(), userQuery.getEndDate());
        }
        return userMapper.listTableByPage(page, wrapper);
    }

    @Override
    @Cacheable
    public User checkUser(String username, String password) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.select(User.Table.ID, User.Table.USERNAME)
                .eq(User.Table.USERNAME, username)
                .eq(User.Table.PASSWORD, password);
        return userMapper.selectOne(wrapper);
    }
}
