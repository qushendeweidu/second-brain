package com.laodeng.backend.controller;

import com.laodeng.backend.common.ErrorCode;
import com.laodeng.backend.common.R;
import com.laodeng.backend.domain.dto.LoginDTO;
import com.laodeng.backend.domain.dto.UserDTO;
import com.laodeng.backend.domain.vo.UserVO;
import com.laodeng.backend.exception.ThrowUtils;
import com.laodeng.backend.service.UserService;
import com.laodeng.backend.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/7/26 19:06
 * @description user 相关的接口
 */

@Log4j2
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/login")
    public R<String> login(@RequestBody @Validated LoginDTO loginDTO) {
        String isLogin = this.userService.login(loginDTO);
        ThrowUtils.throwIf(isLogin==null, ErrorCode.OPERATION_ERROR);
        return R.success(isLogin);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/list")
    public R<List<UserVO>> getUserVOByUserDTO(@RequestBody UserDTO userDTO) {
        List<UserVO> userVOByUserDTO = this.userService.getUserVOByUserDTO(userDTO);
        return R.success(userVOByUserDTO);
    }

}
