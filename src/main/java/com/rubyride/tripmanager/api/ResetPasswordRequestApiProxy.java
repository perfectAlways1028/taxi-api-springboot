package com.rubyride.tripmanager.api;

import com.rubyride.api.ResetPasswordRequestApi;
import com.rubyride.model.UserPassword;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResetPasswordRequestApiProxy implements ResetPasswordRequestApi {
  private final ResetPasswordRequestApiImpl resetPasswordRequestApi;

  public ResetPasswordRequestApiProxy(final ResetPasswordRequestApiImpl resetPasswordRequestApi) {
    this.resetPasswordRequestApi = resetPasswordRequestApi;
  }

  @Override
  public ResponseEntity<Void> resetPasswordRequest(final String username) {
    return resetPasswordRequestApi.requestPasswordReset(username);
  }

  @Override
  public ResponseEntity<Void> changePasswordRequest(final String username, final String resetid, final UserPassword userPassword) {
    return resetPasswordRequestApi.changePassword(username, resetid, userPassword);
  }
}
