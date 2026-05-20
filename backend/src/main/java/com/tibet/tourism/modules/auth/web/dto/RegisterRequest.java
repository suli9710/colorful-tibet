package com.tibet.tourism.modules.auth.web.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度需为3到32个字符")
    @Pattern(regexp = "^[\\p{L}\\p{N}_-]+$", message = "用户名只能包含文字、数字、下划线或短横线")
    private String username;

    @Size(max = 32, message = "昵称长度不能超过32个字符")
    private String nickname;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 72, message = "密码长度需为8到72个字符")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
            message = "密码需包含大小写字母、数字和特殊字符")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
