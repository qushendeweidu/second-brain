package com.laodeng.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laodeng.backend.domain.dto.UserSocialInsertDTO;
import com.laodeng.backend.domain.po.UserSocial;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/10/30 22:42
 * @description 用户绑定第三方相关的信息mapper层
 */
public interface UserSocialMapper extends BaseMapper<UserSocial> {
    @Select("SELECT * FROM user_social WHERE provider = #{provider} AND provider_uid = #{uid}")
    UserSocial selectByProviderUid(@Param("provider") String provider, @Param("uid") String uid);

    @Select("SELECT * FROM user_social WHERE user_id = #{userId} AND provider = #{provider}")
    UserSocial selectByUserAndProvider(@Param("userId") Long userId, @Param("provider") String provider);

    @Insert("INSERT INTO user_social (user_id, provider, provider_uid, provider_login) " +
            "VALUES (#{userId}, #{provider}, #{providerUid}, #{providerLogin})")
    int insert(UserSocialInsertDTO userSocialInsertDTO);

    @Delete("DELETE FROM user_social WHERE user_id = #{userId} AND provider = #{provider}")
    int deleteByUserAndProvider(@Param("userId") Long userId, @Param("provider") String provider);
}

